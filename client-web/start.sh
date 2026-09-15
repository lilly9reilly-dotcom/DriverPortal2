#!/usr/bin/env bash
# تشغيل بوابة العميل محليًا في المتصفح
set -euo pipefail
cd "$(dirname "$0")"
PORT="${1:-8765}"
echo "بوابة العميل جاهزة على: http://127.0.0.1:${PORT}/"
echo "تجربة يونيغاز — رمز الدخول: C2221"
exec python3 -m http.server "$PORT" --bind 127.0.0.1
