var SPREADSHEET_ID = "1a7r3rXY7dPyUjKCdvNopK2Y9ufKYhda0o6DCYBukv2o";
var PRICE_PER_TON_HALAFAYA = 41800;
var PRICE_PER_TON_FACTORY = 10000;
var PRICE_PER_LITER = 430;
var DEDUCTION_RATE = 0.18;
var SECURITY_SCOPE_STRICT_BLOCK_UNSCOPED = true;
var IMAGE_STORAGE_ENABLED = false;

function doGet(e) {
  try {
    var page = e && e.parameter ? String(e.parameter.page || "").trim().toLowerCase() : "";
    if (page === "admin") return renderAdminPage_();
  } catch (err) {}
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function renderAdminPage_() {
  return HtmlService.createHtmlOutput("<html dir='rtl'><body style='font-family:Tajawal,sans-serif;padding:20px'><h2>نظام Core الجديد</h2><p>تم فصل Dashboard التتبع عن هذا النظام. هذه الصفحة خاصة بالإدارة التشغيلية.</p></body></html>")
    .setTitle("Core Admin")
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

function handleRequest(e) {
  var data = readRequestData_(e);
  var action = String(data.action || "").trim();

  try {
    if (!action) return json({ success: false, message: "No action" });

    if (action === "systemHealthCheck") return json(systemHealthCheck(data));
    if (action === "createSystemBackup") return json(createSystemBackup(data));
    if (action === "resetAllDataWithArchive") return json(resetAllDataWithArchive(data));

    if (action === "companyActivationVerify") return json(verifyCompanyActivation(data));
    if (action === "companyActivationList") return json(listCompanyActivationCodes(data));
    if (action === "companyActivationCreate") return json(createCompanyActivationCode(data));
    if (action === "companyActivationSetEnabled") return json(setCompanyActivationEnabled(data));
    if (action === "companyActivationUnbind") return json(unbindCompanyActivationCode(data));
    if (action === "companyActivationUpdateScope") return json(updateCompanyActivationScope(data));
    if (action === "companyActivationAuditList") return json(listCompanyActivationAudit(data));

    if (action === "getAvailableMonths") return json({ success: true, data: getAvailableMonths() });
    if (action === "trip") return json(saveTripMain_(data));
    if (action === "factory") return json(saveFactoryMain_(data));
    if (action === "login") return json(callExisting_("loginDriver", [data]));

    if (action === "gps" || action === "drivers" || action === "route" || action === "alerts" || action === "autoTrips") {
      return json({
        success: false,
        message: "Tracking API is separated from Core. Use Tracking deployment.",
        separated: true
      });
    }

    return json({ success: false, message: "Unknown action: " + action });
  } catch (err) {
    return json({ success: false, message: String(err), action: action });
  }
}

function readRequestData_(e) {
  var data = {};
  if (e && e.parameter) data = e.parameter;
  if (e && e.postData && e.postData.contents) {
    var raw = String(e.postData.contents || "").trim();
    if (raw && raw.charAt(0) === "{") {
      try { data = JSON.parse(raw); } catch (err) {}
    }
  }
  return data || {};
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

function saveTripMain_(data) {
  var scopeValidation = validateScopeForProtectedAction_("trip", data || {});
  if (!scopeValidation.success) return scopeValidation;

  var docNumber = String(data.docNumber || "").trim();
  if (!docNumber) return { success: false, message: "رقم الوصل مطلوب" };

  var monthKey = resolveMonthFromTrip_(data);
  if (!monthKey.success) return monthKey;

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ensureTripsSheet_(ss, monthKey.monthKey);

  var quantity = toNumber_(data.quantity);
  var liters = toNumber_(data.liters);
  var netQty = round3_(quantity - (quantity * DEDUCTION_RATE));
  var price = round0_(netQty * PRICE_PER_TON_HALAFAYA);
  var imageUrl = resolveReceiptImageUrl_(data);

  sheet.appendRow([
    docNumber,
    String(data.driverName || ""),
    String(data.carNumber || ""),
    String(data.loadDate || ""),
    String(data.unloadDate || ""),
    quantity,
    String(data.ownerType || data.owner || ""),
    String(data.destination || data.station || ""),
    imageUrl,
    nowBaghdad_(),
    liters,
    String(data.bogerNumber || ""),
    toNumber_(data.distance),
    price,
    String(data.notes || "")
  ]);

  return { success: true, message: "تم حفظ النقلة", month: monthKey.monthKey };
}

function saveFactoryMain_(data) {
  var scopeValidation = validateScopeForProtectedAction_("factory", data || {});
  if (!scopeValidation.success) return scopeValidation;

  var docNumber = String(data.docNumber || "").trim();
  if (!docNumber) return { success: false, message: "رقم الوصل مطلوب" };

  var monthKey = resolveMonthFromTrip_(data);
  if (!monthKey.success) return monthKey;

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ensureFactorySheet_(ss, "F_" + monthKey.monthKey);

  var quantity = toNumber_(data.quantity);
  var netQty = round3_(quantity - (quantity * DEDUCTION_RATE));
  var price = round0_(netQty * PRICE_PER_TON_FACTORY);
  var imageUrl = resolveReceiptImageUrl_(data);

  sheet.appendRow([
    docNumber,
    String(data.driverName || ""),
    String(data.carNumber || ""),
    String(data.loadDate || data.unloadDate || ""),
    String(data.unloadDate || data.loadDate || ""),
    quantity,
    String(data.ownerType || data.owner || ""),
    String(data.factory || data.destination || ""),
    imageUrl,
    nowBaghdad_(),
    String(data.vehicleOwner || ""),
    String(data.notes || ""),
    "factory",
    price
  ]);

  return { success: true, message: "تم حفظ وصلة المعمل", month: monthKey.monthKey };
}

function ensureTripsSheet_(ss, name) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.appendRow(["رقم الوصل", "اسم السائق", "رقم السيارة", "تاريخ التحميل", "تاريخ التفريغ", "الكمية", "المالك", "المحطة", "رابط الصورة", "وقت الإرسال", "لترات الكاز", "رقم البوجر", "المسافة", "سعر النقل", "ملاحظات"]);
  }
  return sheet;
}

function ensureFactorySheet_(ss, name) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.appendRow(["رقم الوصل", "اسم السائق", "رقم السيارة", "تاريخ التحميل", "تاريخ التفريغ", "الكمية", "المالك", "المحطة/المعمل", "رابط الصورة", "وقت الإرسال", "مالك السيارة أو المالك", "ملاحظات", "source", "سعر النقل"]);
  }
  return sheet;
}

function getAvailableMonths() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var months = {};
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    var n = String(sheets[i].getName() || "").trim();
    if (/^(F_)?\d{4}_\d{2}$/i.test(n)) {
      months[n.replace(/^F_/, "")] = true;
    }
  }
  return Object.keys(months).sort().reverse();
}

function resolveMonthFromTrip_(data) {
  var d = String((data && (data.unloadDate || data.loadDate)) || "").trim();
  if (!d) return { success: true, monthKey: Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy_MM") };
  var p = parseDateParts_(d);
  if (!p) return { success: false, message: "تاريخ النقلة غير صالح" };
  return { success: true, monthKey: p.year + "_" + ("0" + p.month).slice(-2) };
}

function parseDateParts_(raw) {
  var t = String(raw || "").trim().replace(/-/g, "/");
  var m = t.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})$/);
  if (m) return { year: Number(m[1]), month: Number(m[2]), day: Number(m[3]) };
  m = t.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (m) return { year: Number(m[3]), month: Number(m[2]), day: Number(m[1]) };
  return null;
}

function validateScopeForProtectedAction_(action, data) {
  var companyId = String(data.companyId || "").trim();
  var activationCode = String(data.activationCode || "").trim();
  if (SECURITY_SCOPE_STRICT_BLOCK_UNSCOPED && (!companyId || !activationCode)) {
    return {
      success: false,
      message: "companyId و activationCode مطلوبان لهذا الإجراء",
      action: action,
      scope_required: true
    };
  }
  return { success: true };
}

function verifyCompanyActivation(data) {
  var code = String((data && (data.activationCode || data.code)) || "").trim().toUpperCase();
  var deviceId = String((data && data.deviceId) || "").trim();
  if (!code) return { success: false, allowed: false, message: "يرجى إدخال كود التفعيل" };
  if (!deviceId) return { success: false, allowed: false, message: "معرف الجهاز غير متوفر" };

  var sheet = ensureCompanyActivationSheet_();
  var vals = sheet.getDataRange().getValues();
  for (var i = 1; i < vals.length; i++) {
    var rowCode = String(vals[i][0] || "").trim().toUpperCase();
    if (rowCode !== code) continue;

    var enabled = String(vals[i][1] || "1").trim() !== "0";
    if (!enabled) return { success: false, allowed: false, message: "الكود موقوف" };

    var boundDevice = String(vals[i][2] || "").trim();
    if (!boundDevice) {
      sheet.getRange(i + 1, 3).setValue(deviceId);
      sheet.getRange(i + 1, 4).setValue(new Date());
      sheet.getRange(i + 1, 7).setValue(new Date());
      appendCompanyActivationAudit_({ action: "bind", code: code, actor: "system", details: "first bind" });
      return { success: true, allowed: true, message: "تم التفعيل" };
    }

    if (boundDevice !== deviceId) {
      return { success: false, allowed: false, message: "الكود مرتبط بجهاز آخر" };
    }

    sheet.getRange(i + 1, 7).setValue(new Date());
    return { success: true, allowed: true, message: "مفعل مسبقًا" };
  }

  return { success: false, allowed: false, message: "الكود غير موجود" };
}

function ensureCompanyActivationSheet_() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("CompanyActivationCodes");
  if (!sheet) sheet = ss.insertSheet("CompanyActivationCodes");
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["code", "enabled", "device_id", "activated_at", "package_name", "version_name", "last_seen_at", "notes", "app_key", "allowed_package", "company_id", "max_devices"]);
  }
  return sheet;
}

function ensureCompanyActivationAuditSheet_() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("CompanyActivationAudit");
  if (!sheet) sheet = ss.insertSheet("CompanyActivationAudit");
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(["timestamp", "action", "code", "actor", "app_key", "company_id", "details"]);
  }
  return sheet;
}

function appendCompanyActivationAudit_(entry) {
  var s = ensureCompanyActivationAuditSheet_();
  s.appendRow([new Date(), String(entry.action || ""), String(entry.code || ""), String(entry.actor || "admin"), String(entry.appKey || "company"), String(entry.companyId || ""), String(entry.details || "")]);
}

function listCompanyActivationCodes(data) {
  var s = ensureCompanyActivationSheet_();
  var vals = s.getDataRange().getValues();
  var out = [];
  for (var i = 1; i < vals.length; i++) {
    if (!String(vals[i][0] || "").trim()) continue;
    out.push({ code: vals[i][0], enabled: String(vals[i][1] || "1") !== "0", deviceId: vals[i][2] || "", notes: vals[i][7] || "" });
  }
  return { success: true, data: out };
}

function createCompanyActivationCode(data) {
  var s = ensureCompanyActivationSheet_();
  var code = String((data && data.code) || ("CMP-" + Utilities.formatDate(new Date(), "Asia/Baghdad", "yyMMddHHmmss") + "-" + Math.floor(Math.random() * 9000 + 1000))).trim().toUpperCase();
  s.appendRow([code, "1", "", "", "", "", "", String((data && data.notes) || ""), "company", "", "", 1]);
  appendCompanyActivationAudit_({ action: "create_code", code: code, actor: String((data && data.actor) || "admin") });
  return { success: true, code: code };
}

function setCompanyActivationEnabled(data) {
  var code = String((data && data.code) || "").trim().toUpperCase();
  var enabled = String((data && data.enabled) || "").toLowerCase() === "true" || String((data && data.enabled) || "") === "1";
  var s = ensureCompanyActivationSheet_();
  var vals = s.getDataRange().getValues();
  for (var i = 1; i < vals.length; i++) {
    if (String(vals[i][0] || "").trim().toUpperCase() === code) {
      s.getRange(i + 1, 2).setValue(enabled ? "1" : "0");
      appendCompanyActivationAudit_({ action: enabled ? "enable_code" : "disable_code", code: code, actor: String((data && data.actor) || "admin") });
      return { success: true };
    }
  }
  return { success: false, message: "الكود غير موجود" };
}

function unbindCompanyActivationCode(data) {
  var code = String((data && data.code) || "").trim().toUpperCase();
  var s = ensureCompanyActivationSheet_();
  var vals = s.getDataRange().getValues();
  for (var i = 1; i < vals.length; i++) {
    if (String(vals[i][0] || "").trim().toUpperCase() === code) {
      s.getRange(i + 1, 3, 1, 5).clearContent();
      appendCompanyActivationAudit_({ action: "unbind_device", code: code, actor: String((data && data.actor) || "admin") });
      return { success: true };
    }
  }
  return { success: false, message: "الكود غير موجود" };
}

function updateCompanyActivationScope(data) {
  var code = String((data && data.code) || "").trim().toUpperCase();
  var s = ensureCompanyActivationSheet_();
  var vals = s.getDataRange().getValues();
  for (var i = 1; i < vals.length; i++) {
    if (String(vals[i][0] || "").trim().toUpperCase() === code) {
      s.getRange(i + 1, 9).setValue(String((data && data.appKey) || "company"));
      s.getRange(i + 1, 10).setValue(String((data && data.allowedPackage) || ""));
      s.getRange(i + 1, 11).setValue(String((data && data.companyId) || ""));
      s.getRange(i + 1, 12).setValue(Number((data && data.maxDevices) || 1));
      appendCompanyActivationAudit_({ action: "update_scope", code: code, actor: String((data && data.actor) || "admin") });
      return { success: true };
    }
  }
  return { success: false, message: "الكود غير موجود" };
}

function listCompanyActivationAudit(data) {
  var s = ensureCompanyActivationAuditSheet_();
  var vals = s.getDataRange().getValues();
  var out = [];
  for (var i = vals.length - 1; i >= 1; i--) {
    out.push({ timestamp: vals[i][0], action: vals[i][1], code: vals[i][2], actor: vals[i][3], details: vals[i][6] });
    if (out.length >= 200) break;
  }
  return { success: true, data: out };
}

function resetAllDataWithArchive(data) {
  data = data || {};
  var dryRun = String(data.dryRun || "").toLowerCase() === "true";
  var confirmToken = String(data.confirmToken || "").trim();
  if (!dryRun && confirmToken !== "RESET_ALL_CONFIRMED") {
    return { success: false, message: "confirmToken غير صحيح", requiredToken: "RESET_ALL_CONFIRMED" };
  }

  var backup = createSystemBackup({ label: "clean_rebuild" });
  if (!backup.success) return { success: false, message: "فشل النسخة الاحتياطية" };

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();
  var stamp = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd_HHmmss");
  var archived = [];
  var deleted = [];

  for (var i = sheets.length - 1; i >= 0; i--) {
    var sh = sheets[i];
    var n = String(sh.getName() || "").trim();

    if (n === "CompanyActivationCodes" || n === "CompanyActivationAudit" || n === "AuthorizedDrivers") continue;

    if (/^(F_)?\d{4}_\d{2}$/i.test(n)) {
      if (!dryRun) sh.setName("ARCH_" + n + "_" + stamp);
      archived.push(n);
      continue;
    }

    if (/^(TPL_|STMT_|مح_|مع_)/.test(n)) {
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
    recreated: [currentMonth, "F_" + currentMonth]
  };
}

function createSystemBackup(data) {
  try {
    var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    var label = String((data && data.label) || "manual").replace(/\s+/g, "_");
    var stamp = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd_HHmmss");
    var name = "BACKUP_" + ss.getName() + "_" + stamp + "_" + label;
    var copy = ss.copy(name);
    return {
      success: true,
      backupName: name,
      backupFileId: copy.getId(),
      backupUrl: "https://docs.google.com/spreadsheets/d/" + copy.getId() + "/edit"
    };
  } catch (err) {
    return { success: false, message: String(err) };
  }
}

function systemHealthCheck(data) {
  try {
    var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    return {
      success: true,
      spreadsheetId: ss.getId(),
      spreadsheetName: ss.getName(),
      strictScopeEnabled: !!SECURITY_SCOPE_STRICT_BLOCK_UNSCOPED,
      imageStorageEnabled: !!IMAGE_STORAGE_ENABLED,
      hasDoGet: typeof doGet === "function",
      hasDoPost: typeof doPost === "function"
    };
  } catch (err) {
    return { success: false, message: String(err) };
  }
}

function resolveReceiptImageUrl_(data) {
  // Keep disabled by default to avoid storing image URLs in monthly sheets.
  if (!IMAGE_STORAGE_ENABLED) return "";
  return String((data && (data.fileUrl || data.imageUrl || data.photoUrl)) || "").trim();
}

function toNumber_(v) {
  var n = Number(v);
  return isNaN(n) ? 0 : n;
}

function round3_(v) {
  return Math.round((Number(v || 0) + Number.EPSILON) * 1000) / 1000;
}

function round0_(v) {
  return Math.round(Number(v || 0));
}

function nowBaghdad_() {
  return Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");
}

// ==================== Login Function ====================

function loginDriver(data) {
  if (!data) {
    return { success: false, message: "لا توجد بيانات" };
  }

  var carNumber = String(data.carNumber || "").trim();
  if (!carNumber) {
    return { success: false, message: "رقم السيارة مطلوب" };
  }

  var driverName = String(data.name || "").trim();
  var phoneNumber = String(data.phone || "").trim();

  // للتطوير: قبول أي رقم سيارة صحيح
  // في الإنتاج: يجب التحقق من قاعدة بيانات السيارات المصرح بها

  try {
    // حاول العثور على السيارة في جدول السيارات المصرح بها
    var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    var sheet = ss.getSheetByName("AuthorizedCars") || ss.getSheetByName("Cars") || ss.getSheetByName("Vehicles");
    
    var found = false;
    if (sheet) {
      var values = sheet.getDataRange().getValues();
      for (var i = 1; i < values.length; i++) {
        var sheetCarNumber = String(values[i][0] || "").trim();
        if (sheetCarNumber.toLowerCase() === carNumber.toLowerCase()) {
          found = true;
          break;
        }
      }
    }

    // إذا كانت السيارة موجودة أو لا توجد قائمة معتمدة، اسمح بالدخول
    return {
      success: true,
      message: "تم تسجيل الدخول بنجاح",
      driver: driverName || ("سائق_" + carNumber),
      carNumber: carNumber,
      phone: phoneNumber,
      newDriver: !found,
      loginTime: nowBaghdad_()
    };

  } catch (err) {
    // في حالة الخطأ، اسمح بالدخول (للتطوير)
    return {
      success: true,
      message: "تم تسجيل الدخول",
      driver: driverName || ("سائق_" + carNumber),
      carNumber: carNumber,
      phone: phoneNumber,
      newDriver: true,
      loginTime: nowBaghdad_(),
      warning: "لم يتم التحقق من السيارة"
    };
  }
}

function callExisting_(name, args) {
  if (typeof this[name] === "function") {
    return this[name].apply(this, args || []);
  }
  return {
    success: false,
    message: name + " غير موجودة في المشروع"
  };
}
