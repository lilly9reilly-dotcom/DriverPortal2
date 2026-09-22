# -*- coding: utf-8 -*-
from pathlib import Path
import re

p = Path(r"F:\DriverPortal2\temp-prod-live-pull\Main.js")
c = p.read_text(encoding="utf-8")

# Add skipBackup support
c2, n = re.subn(
    r'(function resetAccountingFreshStart\(data\) \{[\s\S]*?var backup = \{ success: true, skipped: true \};\s*)if \(!dryRun\) \{\s*backup = createSystemBackup\(\{ label: "accounting_fresh_start" \}\);',
    r'\1var skipBackup = String(data.skipBackup || "").toLowerCase() === "true";\n  if (!dryRun && !skipBackup) {\n    backup = createSystemBackup({ label: "accounting_fresh_start" });',
    c,
    count=1,
)
print("skipBackup patches", n)
if n == 0:
    # fallback simple replace
    old = '  if (!dryRun) {\n    backup = createSystemBackup({ label: "accounting_fresh_start" });'
    new = '  var skipBackup = String(data.skipBackup || "").toLowerCase() === "true";\n  if (!dryRun && !skipBackup) {\n    backup = createSystemBackup({ label: "accounting_fresh_start" });'
    if old in c:
        c2 = c.replace(old, new, 1)
        print("simple replace ok")
    else:
        old = old.replace("\n", "\r\n")
        new = new.replace("\n", "\r\n")
        if old in c:
            c2 = c.replace(old, new, 1)
            print("crlf replace ok")
        else:
            raise SystemExit("could not patch skipBackup")

# Wrap sheet ops in try/catch inside the for-loop clear/archive - optional
# Add try around clearOwnerOrAgentDbSheet_ call
c2 = c2.replace(
    "if (!dryRun) clearOwnerOrAgentDbSheet_(sh);",
    "if (!dryRun) { try { clearOwnerOrAgentDbSheet_(sh); } catch (clearErr) { clearedDb.push(n + ':ERR'); continue; } }",
)

p.write_text(c2, encoding="utf-8")
print("written", "skipBackup" in c2)
