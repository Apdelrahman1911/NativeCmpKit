"""Collect notices from the checksum-verified, locked crates carried by native publications."""
import hashlib
import json
from pathlib import Path, PurePosixPath
import sys
import tarfile

from scripts.native.artifacts import MODULE_STEMS, metadata


def collect(root, module, cargo_home):
    output = root / "build/native-notices" / module
    report = metadata(root, module, output / "cargo-metadata.json")
    missing = []
    count = 0
    for package in report["packages"]:
        if package["source_kind"] != "crates.io":
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
                is_notice = len(path.parts) == 1 and path.name.upper().startswith(("LICENSE", "COPYING", "NOTICE"))
                if not (is_notice or path.as_posix() == package["license_file"]):
                    continue
                if not entry.isreg() or not 0 < entry.size <= 1024 * 1024:
                    raise ValueError(f"Invalid notice entry for {prefix}: {path}")
                with crate.extractfile(entry) as stream:
                    texts[path] = stream.read()
        if not texts:
            missing.append(package["id"])
            continue
        for relative, contents in texts.items():
            path = output / "crates" / prefix / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(contents)
        count += 1
    result = {"module": module, "packagesWithNotices": count, "missingNotices": missing}
    (output / "notice-collection.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


if __name__ == "__main__":
    cargo_home = Path(sys.argv[1]).resolve(strict=True)
    project = Path.cwd()
    print(json.dumps([collect(project, module, cargo_home) for module in MODULE_STEMS], indent=2))
