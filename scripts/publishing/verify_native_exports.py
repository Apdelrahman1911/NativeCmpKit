"""Load the host payloads and verify that each Kotlin JNI declaration is exported."""
import ctypes
from pathlib import Path
import re
import sys

from scripts.native.artifacts import binary_relative_path, current_platform


if __name__ == "__main__":
    root = Path(sys.argv[1]).resolve(strict=True)
    platform = current_platform()
    for module, bridge in (
        ("kira-file-picker", "NativeFilePickerBridge"),
        ("kira-share", "NativeShareBridge"),
    ):
        payload = root / module / binary_relative_path(module, platform)
        package = "picker" if module == "kira-file-picker" else "share"
        source = next(Path(module).glob(f"src/desktopMain/kotlin/**/{bridge}.kt"))
        methods = re.findall(r"\bexternal\s+fun\s+([A-Za-z][A-Za-z0-9_]*)\s*\(", source.read_text())
        if not methods:
            raise ValueError(f"No JNI methods found in {source}")
        library = ctypes.CDLL(str(payload))
        for method in methods:
            getattr(library, f"Java_io_github_apdelrahman1911_kira_{package}_platform_{bridge}_{method}")
        print(f"{module} {platform}: loaded and found {len(methods)} JNI exports.")
