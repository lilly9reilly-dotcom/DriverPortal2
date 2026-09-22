# -*- coding: utf-8 -*-
from pathlib import Path
import re

p = Path(r"F:\DriverPortal2\temp-prod-live-pull\Main.js")
c = p.read_text(encoding="utf-8")

# Replace the whole resetAccountingFreshStart function with a safer in-place clearer
start = c.find("function resetAccountingFreshStart(data)")
if start < 0:
    raise SystemExit("fn not found")
# find next function after clearOwnerOrAgentDbSheet_
end_marker = "function clearOwnerOrAgentDbSheet_"
end = c.find(end_marker, start)
# include clearOwner function too - find following function
end2 = c.find("\nfunction ", end + 10)
if end2 < 0:
    end2 = len(c)

new_block = r'''function resetAccountingFreshStart(data) {
  data = data || {};
  var dryRun = String(data.dryRun || "").toLowerCase() === "true";
  var confirmToken = String(data.confirmToken || "").trim();
  if (!dryRun && confirmToken !== "RESET_ALL_CONFIRMED") {
    return { success: false, message: "confirmToken غير صحيح", requiredToken: "RESET_ALL_CONFIRMED" };
  }

  try {
    var backup = { success: true, skipped: true };
    var skipBackup = String(data.skipBackup || "").toLowerCase() === "true";
    if (!dryRun && !skipBackup) {
      backup = createSystemBackup({ label: "accounting_fresh_start" });
      if (!backup.success) return { success: false, message: backup.message || "فشل النسخة الاحتياطية" };
    }

    var ss = openSpreadsheetWithRetry_();
    var sheets = ss.getSheets();
    var stamp = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd_HHmmss");
    var archived = [];
    var deleted = [];
    var clearedDb = [];
    var clearedMonths = [];
    var errors = [];

    for (var i = 0; i < sheets.length; i++) {
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
        /^gps_/i.test(n) ||
        /^ARCH_/i.test(n)
      ) continue;

      try {
        if (/^DB_/.test(n) || /_DB$/i.test(n)) {
          if (!dryRun) clearOwnerOrAgentDbSheet_(sh);
          clearedDb.push(n);
          continue;
        }

        if (/^(F_)?\d{4}_\d{2}$/i.test(n)) {
          // تفريغ مكانياً أسرع وأكثر أماناً من إعادة التسمية على الشيت الكبير
          if (!dryRun) clearMonthSheetKeepHeader_(sh, n);
          clearedMonths.push(n);
          archived.push(n);
          continue;
        }

        if (/^(TPL_|STMT_|AGT10_|CO10_|MINP_|مح_|مع_|Routed_)/.test(n)) {
          if (!dryRun) ss.deleteSheet(sh);
          deleted.push(n);
        }
      } catch (sheetErr) {
        errors.push(n + ": " + String(sheetErr));
      }
    }

    var currentMonth = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy_MM");
    if (!dryRun) {
      try { ensureTripsSheet_(ss, currentMonth); } catch (e1) { errors.push("ensureTrips:" + String(e1)); }
      try { ensureFactorySheet_(ss, "F_" + currentMonth); } catch (e2) { errors.push("ensureFactory:" + String(e2)); }
    }

    return {
      success: errors.length === 0,
      dryRun: dryRun,
      backup: backup,
      archivedMonthSheets: archived,
      clearedMonthSheets: clearedMonths,
      deletedSupportSheets: deleted,
      clearedAgentDatabases: clearedDb,
      recreated: [currentMonth, "F_" + currentMonth],
      errors: errors,
      message: dryRun
        ? ("معاينة: تفريغ " + archived.length + " شهر · " + clearedDb.length + " قاعدة · حذف " + deleted.length + " مولَّدة")
        : ("تم التصفير: أشهر " + clearedMonths.length + " · DB " + clearedDb.length + " · أخطاء " + errors.length)
    };
  } catch (err) {
    return { success: false, message: "فشل التصفير: " + String(err), error: String(err) };
  }
}

function clearMonthSheetKeepHeader_(sheet, name) {
  if (!sheet) return;
  var last = sheet.getLastRow();
  var lastCol = Math.max(sheet.getLastColumn(), 1);
  if (last > 1) {
    sheet.getRange(2, 1, last, lastCol).clearContent();
  }
}

function clearOwnerOrAgentDbSheet_(sheet) {
  if (!sheet) return;
  var last = sheet.getLastRow();
  var lastCol = Math.max(sheet.getLastColumn(), 1);
  if (last > 1) {
    sheet.getRange(2, 1, last, lastCol).clearContent();
  }
  if (sheet.getLastRow() === 0) {
    var headers = typeof ownerDbHeaders_ === "function" ? ownerDbHeaders_() : ["رقم الوصل", "السائق", "رقم السيارة"];
    sheet.appendRow(headers);
    sheet.setFrozenRows(1);
  }
}

'''

c = c[:start] + new_block + c[end2:]
p.write_text(c, encoding="utf-8")
print("replaced reset fn, length", len(new_block))
