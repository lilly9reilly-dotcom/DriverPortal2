#!/usr/bin/env bash
# Post-deploy smoke: READ-MOSTLY. Never deletes sheets. Never sends a real trip receipt.
# Fails if GPS/login break after deploy.
set -euo pipefail
URL="${1:-https://script.google.com/macros/s/AKfycbwQsUx8PVIIPufmI8Ev0tTy6qEBtcNn7LXldhmCnuPwpq0VfZUjAx8pl13jSWxywvRM9A/exec}"
python3 - <<PY
import json, urllib.parse, urllib.request, sys
URL = "$URL"

def call(**kw):
    with urllib.request.urlopen(URL + "?" + urllib.parse.urlencode(kw), timeout=45) as r:
        return json.loads(r.read().decode())

fail = 0
def check(name, cond, detail=""):
    global fail
    print(("PASS" if cond else "FAIL"), name, detail)
    if not cond: fail = 1

login = call(action="login", carNumber="22076", password="22076")
check("login", login.get("success") is True, login.get("message",""))

drivers = call(action="drivers")
check("drivers/GPS", drivers.get("success") is True and isinstance(drivers.get("drivers"), list),
      f"count={len(drivers.get('drivers') or [])}")

months = call(action="getAvailableMonths")
check("months", months.get("success") is True, str(months.get("data")))

# Scope validation only — must NOT write
nodoc = call(action="trip", carNumber="22076", companyId="COMP-001", activationCode="CMP-260704184724-6391")
check("trip validation (no write)", nodoc.get("message") == "رقم الوصل مطلوب", nodoc.get("message",""))

factory_nodoc = call(action="factory", carNumber="22076", companyId="COMP-001", activationCode="CMP-260704184724-6391")
check("factory validation (no write)", "رقم الوصل" in str(factory_nodoc.get("message","")), factory_nodoc.get("message",""))

# Ministry endpoints (expected only AFTER deploy)
agents = call(action="listAgents")
health = call(action="systemHealthCheck")
print("INFO ministry listAgents:", agents.get("success"), agents.get("message"))
print("INFO ministry health:", health.get("success"), health.get("message"))
if agents.get("success") is True:
    check("ministry listAgents live", True)
else:
    print("WARN ministry not published yet on this URL (expected before deploy)")

sys.exit(fail)
PY
