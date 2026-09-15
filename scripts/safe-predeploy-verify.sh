#!/usr/bin/env bash
# Safe pre-deploy gate: never writes to live Google Sheet / never pushes code.
# Exit 0 only when offline safety checks pass.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
FAIL=0

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; FAIL=1; }
info() { echo "INFO  $1"; }

echo "========================================"
echo " بوابة التحقق الآمن قبل النشر"
echo "========================================"

# 1) Critical files present
for f in apps-script/Main.gs apps-script/Tracking.gs apps-script/MinistryCore.gs apps-script/MinistryAccounting.gs apps-script/Admin.html \
         app/src/main/java/com/driver/portal/network/GoogleSheetConfig.kt \
         app/src/main/java/com/driver/portal/network/DriverScopeConfig.kt; do
  if [[ -f "$f" ]]; then pass "exists $f"; else fail "missing $f"; fi
done

# 2) GPS must NOT be separated (would break driver app)
if rg -n "Tracking API is separated|separated: true" apps-script/Main.gs >/dev/null; then
  fail "Main.gs still separates GPS — would break drivers"
else
  pass "GPS/tracking kept in Main.gs"
fi
if rg -n "function handleGPS|function getDriversLive" apps-script/Tracking.gs >/dev/null; then
  pass "Tracking.gs has GPS handlers"
else
  fail "Tracking.gs missing GPS handlers"
fi

# 3) Station vs factory remain separate writers
if rg -n "ensureTripsSheet_|ensureFactorySheet_|F_\" \+|\"F_\" \+" apps-script/Main.gs >/dev/null; then
  pass "trip/factory sheet writers present"
else
  fail "trip/factory sheet separation missing"
fi
# factory must write F_ prefix
if rg -n 'ensureFactorySheet_\(ss, "F_"' apps-script/Main.gs >/dev/null; then
  pass "factory sheet uses F_ prefix"
else
  fail "factory sheet F_ prefix missing"
fi

# 4) Legacy compat for old APK
if rg -n "LEGACY_COMPAT_FILL_MISSING_SCOPE" apps-script/Main.gs >/dev/null; then
  pass "legacy scope fill enabled for old APKs"
else
  fail "legacy scope fill missing"
fi

# 5) Ministry offline tests
if node tests/run-ministry-tests.js >/tmp/ministry-tests.out 2>&1; then
  pass "ministry unit tests ($(tail -1 /tmp/ministry-tests.out))"
else
  fail "ministry unit tests failed"
  cat /tmp/ministry-tests.out | tail -20
fi

# 6) Official clients seed includes UniGas
if node tests/test-test.js >/tmp/clients-seed.out 2>&1; then
  if rg -n "يونيغاز|22219" /tmp/clients-seed.out >/dev/null; then
    pass "official client seed includes UniGas"
  else
    fail "UniGas missing from client seed output"
  fi
else
  fail "client seed script failed"
fi

# 7) Android source points to live URL
LIVE_ID='AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A'
if rg -n "$LIVE_ID" app/src/main/java/com/driver/portal/network/GoogleSheetConfig.kt >/dev/null; then
  pass "Android GoogleSheetConfig uses live script URL"
else
  fail "Android URL not on live deployment"
fi

# 8) Destructive wipe guards — ensure no onOpen wipe
if rg -n "deleteSheet|resetAllDataWithArchive\(|clear\(\)" apps-script/Main.gs | rg -v "function |cleanupSafety|replaceOrCreateSheet_|sheet\.clear\(\)" >/dev/null; then
  info "Main.gs contains some clear/delete helpers (expected for admin tools) — do not run wipe menus"
else
  pass "no unexpected wipe patterns flagged"
fi

# 9) Live read-only smoke (no trip write)
python3 - <<'PY' || FAIL=1
import json, urllib.parse, urllib.request, sys
URL='https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec'

def call(**kw):
    with urllib.request.urlopen(URL+'?'+urllib.parse.urlencode(kw), timeout=40) as r:
        return json.loads(r.read().decode())

login=call(action='login', carNumber='22076', password='22076')
drivers=call(action='drivers')
months=call(action='getAvailableMonths')
ok = login.get('success') and drivers.get('success') and months.get('success')
print(('PASS' if ok else 'FAIL') + '  live read-only login/drivers/months')
print('INFO  live ministry published?', call(action='listAgents').get('success') is True)
sys.exit(0 if ok else 1)
PY

echo "========================================"
if [[ $FAIL -eq 0 ]]; then
  echo " النتيجة: جاهز للنشر الآمن (بعد clasp login)"
  exit 0
else
  echo " النتيجة: هناك فشل — لا تنشر"
  exit 1
fi
