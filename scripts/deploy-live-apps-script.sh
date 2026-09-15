#!/usr/bin/env bash
# Deploy ministry + GPS Apps Script to the LIVE spreadsheet project.
# Prerequisite: Google login via clasp (one-time).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASP_BIN="${CLASP_BIN:-$ROOT/.npm-global/node_modules/.bin/clasp}"
LIVE_SHEET_ID="1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM"
LIVE_SMOKE_URL="https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec"
cd "$ROOT/apps-script"

if [[ ! -x "$CLASP_BIN" ]]; then
  echo "Installing clasp locally..."
  npm install --prefix "$ROOT/.npm-global" @google/clasp@2.4.2
  CLASP_BIN="$ROOT/.npm-global/node_modules/.bin/clasp"
fi

if [[ ! -f "$HOME/.clasprc.json" ]]; then
  echo "=============================================="
  echo "مطلوب تسجيل دخول Google مرة واحدة:"
  echo "  $CLASP_BIN login"
  echo "ثم أعد تشغيل هذا السكربت."
  echo "=============================================="
  exit 2
fi

# Ensure parentId points at live sheet
python3 - <<PY
import json
from pathlib import Path
p = Path('.clasp.json')
cfg = json.loads(p.read_text())
cfg['parentId'] = '$LIVE_SHEET_ID'
p.write_text(json.dumps(cfg, indent=2) + '\n')
print('parentId =', cfg.get('parentId'))
print('scriptId =', cfg.get('scriptId'))
PY

echo "==> Pushing Apps Script files..."
"$CLASP_BIN" push --force

echo "==> Creating/updating web app deployment (if permitted)..."
# Prefer creating a new versioned deployment; fall back to push-only.
if "$CLASP_BIN" deploy -d "ministry-gps-$(date +%Y%m%d-%H%M)" 2>/tmp/clasp_deploy_err; then
  echo "Deploy OK"
else
  echo "Deploy command note:"
  cat /tmp/clasp_deploy_err || true
  echo "Files were pushed. If URL must stay the same, open Deploy → Manage deployments"
  echo "and publish a New version on the EXISTING web app deployment."
fi

echo "==> Smoke checks against current live URL (may still be old until version published)..."
python3 - <<'PY'
import json, urllib.parse, urllib.request
URL = "https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec"

def call(action, **extra):
    q = urllib.parse.urlencode({"action": action, **extra})
    with urllib.request.urlopen(URL + "?" + q, timeout=45) as r:
        return json.loads(r.read().decode())

for action in ["login", "drivers", "listAgents", "systemHealthCheck", "createClientDatabasesNow"]:
    try:
        if action == "login":
            d = call(action, carNumber="22076", password="22076")
        else:
            d = call(action)
        print(action, "=>", d.get("success"), str(d.get("message", ""))[:120])
    except Exception as e:
        print(action, "ERR", e)
PY

echo "Done."
