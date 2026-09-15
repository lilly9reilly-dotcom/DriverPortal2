#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
PORT="${1:-8765}"
echo "بوابة العميل: http://127.0.0.1:${PORT}/"
echo "تجربة: يونيغاز C2221 | شركة ارض الهادي C1220"
exec python3 -m http.server "$PORT" --bind 127.0.0.1
