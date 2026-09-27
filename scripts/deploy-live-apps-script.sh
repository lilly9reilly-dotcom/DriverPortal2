#!/usr/bin/env bash
# Hardened live deploy: never wipe sheets; require preflight; smoke GPS after push.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLASP_BIN="${CLASP_BIN:-$ROOT/.npm-global/node_modules/.bin/clasp}"
LIVE_SHEET_ID="1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM"
LIVE_URL="https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec"

cd "$ROOT"

echo "==> 0) Preflight (offline + live read-only)"
bash "$ROOT/scripts/safe-predeploy-verify.sh"

if [[ ! -f "$HOME/.clasprc.json" ]]; then
  echo "STOP: clasp login required. Live system left untouched."
  echo "  $CLASP_BIN login"
  exit 2
fi

echo "==> 1) Baseline smoke BEFORE push (must keep working after)"
bash "$ROOT/scripts/safe-postdeploy-smoke.sh" "$LIVE_URL" | tee /tmp/smoke-before.log

cd "$ROOT/apps-script"
python3 - <<PY
import json
from pathlib import Path
p=Path('.clasp.json')
cfg=json.loads(p.read_text())
cfg['parentId']='$LIVE_SHEET_ID'
p.write_text(json.dumps(cfg, indent=2)+'\n')
print(cfg)
PY

echo "==> 2) Push Apps Script (no sheet wipe)"
"$CLASP_BIN" push --force

echo "==> 3) Prefer NEW VERSION on EXISTING web app deployment (keeps same URL)"
echo "If clasp deploy creates a NEW URL, keep using the old URL until you publish a version"
echo "on the existing deployment AKfycbwQsUx8… from the Apps Script UI."
"$CLASP_BIN" deploy -d "safe-ministry-gps-$(date +%Y%m%d-%H%M)" || true

echo "==> 4) Smoke AFTER push on the SAME live URL"
bash "$ROOT/scripts/safe-postdeploy-smoke.sh" "$LIVE_URL" | tee /tmp/smoke-after.log

python3 - <<'PY'
from pathlib import Path
before=Path('/tmp/smoke-before.log').read_text()
after=Path('/tmp/smoke-after.log').read_text()
# GPS/login must still PASS
for key in ['login','drivers/GPS']:
    if f'FAIL {key}' in after or f'FAIL  {key}' in after:
        raise SystemExit(f'ROLLBACK NEEDED: {key} broken after deploy')
print('PASS  login+GPS still healthy after deploy attempt')
if 'PASS  ministry listAgents live' in after:
    print('PASS  ministry endpoints are live')
else:
    print('WARN  ministry not yet on this URL — publish New version on EXISTING deployment in Apps Script UI')
PY

echo "Done. Next safe Admin actions (manual):"
echo "  1) createClientDatabasesNow"
echo "  2) organizeHistoricalAgentLedgers / ترحيل من يونيو"
echo "  3) applyAlyamamaBrandFont"
echo "Do NOT run reset/wipe/cleanupApply without backup."
