"""Assemble only the manifest-verified native payloads of the current revision."""
from pathlib import Path
import shutil
import subprocess
import sys

from scripts.native.artifacts import (
    MODULE_STEMS, SUPPORTED_PLATFORMS, binary_relative_path, manifest_relative_path, validate_bundle,
)


def main():
    incoming, output = (Path(arg).resolve(strict=False) for arg in sys.argv[1:3])
    version = sys.argv[3]
    root = Path.cwd().resolve()
    if not output.is_relative_to(root / "build") or output.exists():
        raise ValueError("Use an empty staging path below the producer build directory")
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    options = {"require_clean": True, "expected_revision": revision, "expected_version": version}
    for module in MODULE_STEMS:
        for platform in SUPPORTED_PLATFORMS:
            archive = incoming / f"native-payload-{platform}" / module
            validate_bundle(root, module, archive, [platform], **options)
            for relative in (binary_relative_path(module, platform), manifest_relative_path(module, platform)):
                destination = output / module / relative
                destination.parent.mkdir(parents=True, exist_ok=True)
                with (archive / relative).open("rb") as source, destination.open("xb") as target:
                    shutil.copyfileobj(source, target)
        validate_bundle(root, module, output / module, list(SUPPORTED_PLATFORMS), **options)
    print(f"Verified both native modules for all {len(SUPPORTED_PLATFORMS)} desktop targets at {revision}.")


if __name__ == "__main__":
    main()
