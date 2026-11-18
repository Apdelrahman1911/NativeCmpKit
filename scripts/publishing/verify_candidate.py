#!/usr/bin/env python3
"""Verify Maven coordinates, published metadata and artifacts, and prepare an independent consumer."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
from xml.etree import ElementTree
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[2]
MANIFEST = json.loads((ROOT / "docs/publishing/coordinates.json").read_text())
GROUP = MANIFEST["group"]
VERSION = MANIFEST["candidateVersion"]
MODULES = {module["artifact"]: module for module in MANIFEST["modules"]}
TARGETS = tuple(MANIFEST["platformArtifacts"])
POM_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
PRIMARY = {"desktop": "jar", "android": "aar", "js": "klib", "wasm-js": "klib",
           "iosarm64": "klib", "iossimulatorarm64": "klib"}


def components(module):
    return {module, *(f"{module}-{target}" for target in TARGETS)}


def closure(module):
    result = {module}
    for dependency in MODULES[module]["dependencies"]:
        result.update(closure(dependency))
    return result


def check_pom(path, module, coordinate):
    def field(parent, tag):
        return parent.findtext("/".join(f"m:{part}" for part in tag.split("/")), namespaces=POM_NAMESPACE)

    pom = ElementTree.parse(path).getroot()
    repository = MANIFEST["repository"]
    repository_url = f"https://github.com/{repository}"
    for key, expected in (
        ("groupId", GROUP), ("artifactId", coordinate), ("version", VERSION),
        ("developers/developer/name", MANIFEST["maintainer"]), ("licenses/license/name", "Apache-2.0"),
        ("name", f"{MANIFEST['name']} {module}"), ("url", repository_url),
        ("issueManagement/url", f"{repository_url}/issues"),
        ("scm/url", repository_url), ("scm/connection", f"scm:git:{repository_url}.git"),
        ("scm/developerConnection", f"scm:git:ssh://git@github.com/{repository}.git"),
    ):
        if field(pom, key) != expected:
            raise ValueError(f"{path.name}: unexpected {key} for {coordinate}")
    allowed = set().union(*(components(name) for name in closure(module)))
    for dependency in pom.findall("m:dependencies/m:dependency", POM_NAMESPACE):
        group = field(dependency, "groupId")
        name = field(dependency, "artifactId")
        if group == GROUP and name not in allowed:
            raise ValueError(f"{coordinate} pulls in unrelated Kira module {name}")
        if group == "org.openjfx":
            raise ValueError(f"{coordinate} captured a producer JavaFX dependency")


def check_module(path, group_dir, module, coordinate):
    data = json.loads(path.read_text())
    component = data["component"]
    expected_component = module if "url" in component else coordinate
    if (component["group"], component["module"], component["version"]) != (GROUP, expected_component, VERSION):
        raise ValueError(f"{path.name} references the wrong publication: {component}")
    allowed = set().union(*(components(name) for name in closure(module)))
    redirects = set()
    for variant in data["variants"]:
        if redirect := variant.get("available-at"):
            name = redirect["module"]
            expected = group_dir / name / VERSION / f"{name}-{VERSION}.module"
            actual = (path.parent / redirect["url"]).resolve(strict=True)
            if actual != expected or name not in components(module) or redirect["group"] != GROUP or redirect["version"] != VERSION:
                raise ValueError(f"{path.name} has an invalid target redirect: {redirect}")
            redirects.add(name)
        for dependency in variant.get("dependencies", []):
            if dependency["group"] == GROUP and dependency["module"] not in allowed:
                raise ValueError(f"{path.name} has an unrelated module dependency: {dependency}")
            if dependency["group"] == "org.openjfx":
                raise ValueError(f"{path.name} captured a producer JavaFX dependency")
        for file in variant.get("files", []):
            artifact = (path.parent / file["url"]).resolve(strict=True)
            if artifact.parent != path.parent or artifact.stat().st_size != file["size"]:
                raise ValueError(f"{path.name} references the wrong artifact: {artifact}")
            if hashlib.sha256(artifact.read_bytes()).hexdigest() != file["sha256"]:
                raise ValueError(f"{path.name} has a stale artifact hash: {artifact.name}")
    return redirects


def verify_repository(repository):
    group_dir = repository.joinpath(*GROUP.split("."))
    expected = set().union(*(components(module) for module in MODULES))
    actual = {path.name for path in group_dir.iterdir() if (path / VERSION).is_dir()}
    if actual != expected:
        raise ValueError(f"Missing coordinates: {sorted(expected - actual)}; unexpected: {sorted(actual - expected)}")
    for module in MODULES:
        for target in ("", *TARGETS):
            coordinate = f"{module}-{target}" if target else module
            directory = group_dir / coordinate / VERSION
            base = directory / f"{coordinate}-{VERSION}"
            check_pom(Path(f"{base}.pom"), module, coordinate)
            redirects = check_module(Path(f"{base}.module"), group_dir, module, coordinate)
            if not target and redirects != components(module) - {module}:
                raise ValueError(f"{coordinate} is missing Kotlin target redirects: {redirects}")
            for suffix in (
                "." + PRIMARY.get(target, "jar"), "-sources.jar", "-javadoc.jar", "-notices.zip",
            ):
                with ZipFile(Path(f"{base}{suffix}")) as archive:
                    if archive.testzip() is not None:
                        raise ValueError(f"Corrupt candidate archive {base}{suffix}")
                    if suffix == "-notices.zip" and not {"LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md"} <= set(archive.namelist()):
                        raise ValueError(f"{base} lacks the required accompanying notices")
    print(f"Verified {len(MODULES)} modules and {len(expected)} Maven coordinates ({GROUP}, {VERSION}).")


def verify_native_notices(repository):
    group_dir = repository.joinpath(*GROUP.split("."))
    for module in ("kira-file-picker", "kira-share"):
        archive = group_dir / module / VERSION / f"{module}-{VERSION}-notices.zip"
        with ZipFile(archive) as notices:
            records = json.loads(notices.read("native-dependencies/notice-collection.json"))
        if records["module"] != module or not records["packagesWithNotices"] or records["missingNotices"]:
            raise ValueError(f"{module} native notices still need review: {records}")
    print("Both packaged native modules contain notices collected from their locked crate archives.")


def prepare_consumers(destination):
    shutil.copytree(ROOT / "tests/publishing/consumer", destination)
    (destination / "coordinates.json").write_text(json.dumps({**MANIFEST, "candidateVersion": VERSION}, indent=2) + "\n")
    for name, module in MODULES.items():
        symbol = module["consumerSymbol"]
        package, member = symbol.rsplit(".", 1)
        source = destination / name / "src/commonMain/kotlin/Consumer.kt"
        source.parent.mkdir(parents=True)
        if module.get("consumerPermissionExtension"):
            code = f"""@file:OptIn({package}.ExperimentalPermissionsApi::class)

import {package}.Permission
import {symbol}

fun consumerPermission(): Permission = Permission.{member}
"""
        else:
            annotation = f"@file:OptIn({module['consumerOptIn']}::class)\n\n" if "consumerOptIn" in module else ""
            code = f"{annotation}typealias PublishedApi = {symbol}\n"
        source.write_text(code)


def main():
    global VERSION
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path)
    parser.add_argument("--require-native-notices", action="store_true")
    parser.add_argument("--producer-poms", action="store_true")
    parser.add_argument("--prepare-consumers", type=Path)
    parser.add_argument("--version", default=os.environ.get("RELEASE_VERSION") or VERSION)
    args = parser.parse_args()
    VERSION = args.version
    if args.producer_poms:
        for module, entry in MODULES.items():
            publications = ROOT / entry["directory"] / "build/publications"
            check_pom(publications / "kotlinMultiplatform/pom-default.xml", module, module)
            check_pom(publications / "desktop/pom-default.xml", module, f"{module}-desktop")
        print(f"Verified {2 * len(MODULES)} producer POMs for {len(MODULES)} independent modules.")
    if args.repository:
        repository = args.repository.resolve(strict=True)
        verify_repository(repository)
        if args.require_native_notices:
            verify_native_notices(repository)
    elif args.require_native_notices:
        parser.error("--require-native-notices needs --repository")
    if args.prepare_consumers:
        prepare_consumers(args.prepare_consumers)
    if not (args.producer_poms or args.repository or args.prepare_consumers):
        parser.error("select a verification or consumer-preparation operation")


if __name__ == "__main__":
    main()
