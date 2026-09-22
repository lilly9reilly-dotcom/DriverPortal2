# -*- coding: utf-8 -*-
from pathlib import Path

main = Path(r"F:\DriverPortal2\temp-prod-live-pull\Main.js")
c = main.read_text(encoding="utf-8")

if "function resetAccountingFreshStart" not in c:
    marker = '    if (action === "createSystemBackup") {\n      return json(createSystemBackup(data));\n    }'
    if marker not in c:
        # try CRLF
        marker = marker.replace("\n", "\r\n")
    if marker not in c:
        raise SystemExit("action marker not found")
    replacement = marker + (
        "\n\n    if (action === \"resetAccountingFreshStart\" || action === \"resetAllDataWithArchive\") {\n"
        "      return json(resetAccountingFreshStart(data));\n"
        "    }"
        if "\r\n" not in marker
        else (
            "\r\n\r\n    if (action === \"resetAccountingFreshStart\" || action === \"resetAllDataWithArchive\") {\r\n"
            "      return json(resetAccountingFreshStart(data));\r\n"
            "    }"
        )
    )
    # simpler approach
    idx = c.find('if (action === "createSystemBackup")')
    if idx < 0:
        raise SystemExit("createSystemBackup action not found")
    # find end of that if block
    end = c.find("\n", c.find("}", idx))
    # insert after the closing brace line
    brace = c.find("}", idx)
    line_end = c.find("\n", brace)
    inject = (
        '\n\n    if (action === "resetAccountingFreshStart" || action === "resetAllDataWithArchive") {\n'
        "      return json(resetAccountingFreshStart(data));\n"
        "    }"
    )
    c = c[:line_end] + inject + c[line_end:]
    print("wired action at", idx)

fn = r'''

/**
 * تصفير محاسبي: أرشفة كل الأشهر، تفريغ قواعد المعتمدين (DB_* و *_DB)،
 * حذف أوراق مولَّدة، إنشاء الشهر الحالي فارغاً. لا يمس المعتمدين/السيارات/GPS.
 */
function resetAccountingFreshStart(data) {
  data = data || {};
  var dryRun = String(data.dryRun || "").toLowerCase() === "true";
  var confirmToken = String(data.confirmToken || "").trim();
  if (!dryRun && confirmToken !== "RESET_ALL_CONFIRMED") {
    return { success: false, message: "confirmToken غير صحيح", requiredToken: "RESET_ALL_CONFIRMED" };
  }

  var backup = { success: true, skipped: true };
  if (!dryRun) {
    backup = createSystemBackup({ label: "accounting_fresh_start" });
    if (!backup.success) return { success: false, message: backup.message || "فشل النسخة الاحتياطية" };
  }

  var ss = openSpreadsheetWithRetry_();
  var sheets = ss.getSheets();
  var stamp = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd_HHmmss");
  var archived = [];
  var deleted = [];
  var clearedDb = [];

  for (var i = sheets.length - 1; i >= 0; i--) {
    var sh = sheets[i];
    var n = String(sh.getName() || "").trim();

    if (
      n === "CompanyActivationCodes" ||
      n === "CompanyActivationAudit" ||
      n === "AuthorizedDrivers" ||
      n === "Agents" ||
      n === "Fleet" ||
      n === "العملاء" ||
      n === "السيارات" ||
      n === "معتمدون" ||
      n === "60" ||
      n === "تنظيم_المعتمدين" ||
      /^gps_/i.test(n)
    ) continue;

    if (/^DB_/.test(n) || /_DB$/i.test(n)) {
      if (!dryRun) clearOwnerOrAgentDbSheet_(sh);
      clearedDb.push(n);
      continue;
    }

    if (/^(F_)?\d{4}_\d{2}$/i.test(n)) {
      if (!dryRun) sh.setName("ARCH_" + n + "_" + stamp);
      archived.push(n);
      continue;
    }

    if (/^(TPL_|STMT_|AGT10_|CO10_|MINP_|مح_|مع_|Routed_)/.test(n)) {
      if (!dryRun) ss.deleteSheet(sh);
      deleted.push(n);
    }
  }

  var currentMonth = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy_MM");
  if (!dryRun) {
    ensureTripsSheet_(ss, currentMonth);
    ensureFactorySheet_(ss, "F_" + currentMonth);
  }

  return {
    success: true,
    dryRun: dryRun,
    backup: backup,
    archivedMonthSheets: archived,
    deletedSupportSheets: deleted,
    clearedAgentDatabases: clearedDb,
    recreated: [currentMonth, "F_" + currentMonth],
    message: dryRun
      ? ("معاينة: أرشفة " + archived.length + " شهر · تفريغ " + clearedDb.length + " قاعدة · حذف " + deleted.length + " مولَّدة")
      : ("تم التصفير: أرشفة " + archived.length + " · تفريغ " + clearedDb.length + " · شهر فارغ " + currentMonth)
  };
}

function clearOwnerOrAgentDbSheet_(sheet) {
  if (!sheet) return;
  var last = sheet.getLastRow();
  if (last > 1) sheet.deleteRows(2, last - 1);
  if (sheet.getLastRow() === 0) {
    var headers = typeof ownerDbHeaders_ === "function" ? ownerDbHeaders_() : ["رقم الوصل", "السائق", "رقم السيارة"];
    sheet.appendRow(headers);
    sheet.setFrozenRows(1);
  }
}
'''

if "function resetAccountingFreshStart" not in c:
    c = c + fn
    print("appended function")
else:
    print("function already present")

main.write_text(c, encoding="utf-8")
print("done", c.count("resetAccountingFreshStart"))
