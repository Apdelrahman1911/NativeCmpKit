"""Collect notices from the checksum-verified, locked crates carried by native publications."""
import hashlib
import json
from pathlib import Path, PurePosixPath
import subprocess
import sys
import tarfile
from tempfile import TemporaryDirectory

from scripts.native.artifacts import MODULE_STEMS, SUPPORTED_PLATFORMS, _replace_directory, metadata, toolchain


def shipped_dependency_ids(root, module):
    selected = set()
    for configuration in SUPPORTED_PLATFORMS.values():
        raw = json.loads(subprocess.check_output(
            ["rustup", "run", toolchain(root), "cargo", "metadata", "--locked", "--offline",
             "--format-version", "1", "--filter-platform", configuration["rustTarget"]],
            cwd=root / module / "native", text=True,
        ))
        nodes = {node["id"]: node["deps"] for node in raw["resolve"]["nodes"]}
        pending = [raw["resolve"]["root"]]
        visited = set()
        while pending:
            identifier = pending.pop()
            if identifier in visited:
                continue
            visited.add(identifier)
            pending.extend(dep["pkg"] for dep in nodes[identifier]
                           if any(edge["kind"] != "dev" for edge in dep["dep_kinds"]))
        selected.update(f"{package['name']}@{package['version']}" for package in raw["packages"]
                        if package["id"] in visited and package["source"])
    return selected


def supplemental_notices(directory, package, catalog):
    supplement = catalog.get(package["id"])
    if not supplement:
        return {}
    if supplement["archiveSha256"] != package["checksum"] or supplement["license"] != package["license"]:
        raise ValueError(f"Supplement does not match the locked crate: {package['id']}")
    texts = {}
    for name, record in supplement["files"].items():
        path = (directory / record["source"]).resolve(strict=True)
        if not path.is_relative_to(directory) or len(PurePosixPath(name).parts) != 1:
            raise ValueError(f"Invalid supplemental notice path for {package['id']}")
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != record["sha256"]:
            raise ValueError(f"Modified supplemental notice for {package['id']}: {name}")
        texts[PurePosixPath(name)] = data
    return texts


def collect_to(root, module, cargo_home, output):
    report = metadata(root, module, output / "cargo-metadata.json")
    selected = shipped_dependency_ids(root, module)
    supplement_root = (root / "licenses/native-cargo").resolve()
    catalog = json.loads((supplement_root / "catalog.json").read_text())
    missing = []
    count = 0
    for package in report["packages"]:
        if package["id"] not in selected:
            continue
        name, version = package["name"], package["version"]
        prefix = f"{name}-{version}"
        candidates = sorted(cargo_home.glob(f"registry/cache/index.crates.io-*/{prefix}.crate"))
        archive = next(
            (path for path in candidates if path.is_file() and not path.is_symlink() and
             hashlib.sha256(path.read_bytes()).hexdigest() == package["checksum"]),
            None,
        )
        if archive is None:
            raise ValueError(f"No checksum-verified crates.io archive for {prefix}")
        texts = {}
        with tarfile.open(archive, "r:gz") as crate:
            for entry in crate:
                pieces = PurePosixPath(entry.name).parts
                if len(pieces) < 2 or pieces[0] != prefix or ".." in pieces:
                    continue
                path = PurePosixPath(*pieces[1:])
                is_notice = len(path.parts) == 1 and path.name.upper().startswith(("LICENSE", "COPYING", "COPYRIGHT", "NOTICE"))
                if not (is_notice or path.as_posix() == package["license_file"]):
                    continue
                if not entry.isreg() or not 0 < entry.size <= 1024 * 1024:
                    raise ValueError(f"Invalid notice entry for {prefix}: {path}")
                with crate.extractfile(entry) as stream:
                    texts[path] = stream.read()
        supplement = supplemental_notices(supplement_root, package, catalog)
        if texts.keys() & supplement.keys():
            raise ValueError(f"Duplicate native notice for {package['id']}")
        texts.update(supplement)
        if supplement:
            texts[PurePosixPath("supplement-provenance.json")] = (
                json.dumps(catalog[package["id"]], indent=2).encode() + b"\n"
            )
        if not texts:
            missing.append(package["id"])
            continue
        for relative, contents in texts.items():
            path = output / "crates" / prefix / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(contents)
        count += 1
    result = {
        "module": module, "platforms": list(SUPPORTED_PLATFORMS), "packagesWithNotices": count,
        "missingNotices": missing,
        "notInReleaseGraph": sorted(package["id"] for package in report["packages"]
                                    if package["source_kind"] == "crates.io" and package["id"] not in selected),
    }
    (output / "notice-collection.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def collect(root, module, cargo_home):
    destination = root / "build/native-notices" / module
    destination.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=destination.parent, prefix=".notices-") as temporary:
        output = Path(temporary) / module
        result = collect_to(root, module, cargo_home, output)
        _replace_directory(output, destination)
    return result


if __name__ == "__main__":
    cargo_home = Path(sys.argv[1]).resolve(strict=True)
    project = Path.cwd()
    print(json.dumps([collect(project, module, cargo_home) for module in MODULE_STEMS], indent=2))
