#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
PORT="${1:-8765}"
echo "تطبيق المعتمد: http://127.0.0.1:${PORT}/"
echo "تجربة معتمد: سجاد C1587 | ابراهيم C3137"
exec python3 -m http.server "$PORT" --bind 127.0.0.1
