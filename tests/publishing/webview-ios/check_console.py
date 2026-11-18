import subprocess
import sys
import time
from pathlib import Path

APP = "consumer.kira.webview"
PASS = "KIRA_WEBVIEW_PASS"
FAIL = "KIRA_WEBVIEW_FAIL"


def verify(simulator, log):
    command = ["xcrun", "simctl", "launch", "--console", simulator, APP]
    with log.open("wb") as output, subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT) as app:
        try:
            start = time.monotonic()
            while time.monotonic() - start < 85:
                console = log.read_text(errors="replace")
                if FAIL in console or app.poll() is not None:
                    break
                if PASS in console.splitlines():
                    print("Verified one passed external WKWebView simulator method.")
                    return
                time.sleep(0.5)
            raise SystemExit(log.read_text(errors="replace"))
        finally:
            subprocess.run(["xcrun", "simctl", "terminate", simulator, APP], check=False, timeout=15)
            if app.poll() is None:
                app.kill()


if __name__ == "__main__":
    verify(sys.argv[1], Path(sys.argv[2]))
