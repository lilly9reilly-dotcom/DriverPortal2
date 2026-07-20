function loginDriver(data) {
  data = data || {};

  var requestedName = String(data.name || data.driverName || "").trim();
  var requestedPhone = normalizeLoginPhone_(data.phone || "");
  var requestedCar = normalizeLoginVehicle_(data.carNumber || data.car || "");

  if (!requestedCar) {
    return {
      success: false,
      message: "يجب إدخال رقم السيارة"
    };
  }

  var authorized = findAuthorizedDriverForLogin_(requestedName, requestedPhone, requestedCar);
  if (!authorized.allowed) {
    return {
      success: false,
      message: authorized.message || "هذا المستخدم غير مصرح له بالدخول. يرجى التواصل مع الإدارة."
    };
  }

  var loginMessage = authorized.autoCreated
    ? "تم تسجيل الدخول وإنشاء تصريح السيارة تلقائيًا."
    : "تم تسجيل الدخول";

  return {
    success: true,
    message: loginMessage,
    driver: authorized.driverName || requestedName || ("سائق المركبة " + requestedCar),
    carNumber: authorized.carNumber || String(data.carNumber || data.car || "").trim(),
    newDriver: false,
    access: "authorized"
  };
}

function findAuthorizedDriverForLogin_(driverName, phone, carNumber) {
  var ensured = ensureAuthorizedDriversLoginSheet_();
  var sheet = ensured.sheet;

  if (ensured.created) {
    return createBootstrapAuthorizationForLogin_(sheet, driverName, phone, carNumber);
  }

  var values = sheet.getDataRange().getValues();
  if (!values || values.length < 2) {
    return createBootstrapAuthorizationForLogin_(sheet, driverName, phone, carNumber);
  }

  var colMap = buildColumnMap_(values[0]);
  var normalizedCar = normalizeLoginVehicle_(carNumber);

  for (var i = 1; i < values.length; i++) {
    var row = values[i];
    if (!row || row.join("").trim() === "") continue;

    var rowNameRaw = getCellByAliases_(row, colMap, ["driverName", "driver", "name", "اسم السائق", "السائق"], 0);
    var rowPhoneRaw = getCellByAliases_(row, colMap, ["phone", "mobile", "driverPhone", "رقم الهاتف", "الهاتف"], 1);
    var rowCarRaw = getCellByAliases_(row, colMap, ["carNumber", "car", "vehicle", "plate", "رقم السيارة", "السيارة"], 2);
    var rowActiveRaw = getCellByAliases_(row, colMap, ["active", "enabled", "allowed", "status", "الحالة", "مفعل"], 3);

    var rowName = normalizeText_(rowNameRaw);
    var rowCar = normalizeLoginVehicle_(rowCarRaw);

    if (rowCar === normalizedCar) {
      if (!isLoginAccessEnabled_(rowActiveRaw)) {
        return {
          allowed: false,
          message: "هذا الحساب غير مفعل من الإدارة."
        };
      }

      return {
        allowed: true,
        driverName: String(rowNameRaw || driverName || ("سائق المركبة " + carNumber)).trim(),
        carNumber: String(rowCarRaw || carNumber).trim()
      };
    }
  }

  // The driver app authenticates by vehicle number.  A vehicle that has not
  // been recorded yet is registered as active on its first successful login.
  return createBootstrapAuthorizationForLogin_(sheet, driverName, phone, carNumber);
}

function createBootstrapAuthorizationForLogin_(sheet, driverName, phone, carNumber) {
  var resolvedCar = String(carNumber || "").trim();
  if (!resolvedCar) {
    return {
      allowed: false,
      message: "يجب إدخال رقم السيارة"
    };
  }

  var resolvedName = String(driverName || "").trim();
  if (!resolvedName) {
    resolvedName = "سائق المركبة " + resolvedCar;
  }

  sheet.appendRow([
    resolvedName,
    String(phone || "").trim(),
    resolvedCar,
    1,
    "auto bootstrap from login",
    new Date()
  ]);

  return {
    allowed: true,
    driverName: resolvedName,
    carNumber: resolvedCar,
    autoCreated: true
  };
}

function ensureAuthorizedDriversLoginSheet_() {
  var ss = openSpreadsheetWithRetry_();
  var sheet = getFirstExistingLoginSheet_(ss, [
    "AuthorizedDrivers",
    "Authorized Drivers",
    "DriversAccess",
    "DriverAccess",
    "السائقين المصرح لهم",
    "المصرح لهم",
    "تصاريح السائقين"
  ]);

  var created = false;
  if (!sheet) {
    sheet = ss.insertSheet("AuthorizedDrivers");
    created = true;
  }

  var header = ["driverName", "phone", "carNumber", "active", "notes", "createdAt"];
  var lastRow = Math.max(sheet.getLastRow(), 0);
  var lastCol = Math.max(sheet.getLastColumn(), 0);

  if (lastRow === 0) {
    sheet.getRange(1, 1, 1, header.length).setValues([header]);
    sheet.setFrozenRows(1);
    return { sheet: sheet, created: created };
  }

  var firstRow = sheet.getRange(1, 1, 1, Math.max(lastCol, header.length)).getValues()[0] || [];
  var map = buildColumnMap_(firstRow);
  var hasName = map[normalizeText_("driverName")] !== undefined || map[normalizeText_("اسم السائق")] !== undefined;
  var hasPhone = map[normalizeText_("phone")] !== undefined || map[normalizeText_("رقم الهاتف")] !== undefined;
  var hasCar = map[normalizeText_("carNumber")] !== undefined || map[normalizeText_("رقم السيارة")] !== undefined;

  if (!(hasName && hasPhone && hasCar)) {
    sheet.insertRowBefore(1);
    sheet.getRange(1, 1, 1, header.length).setValues([header]);
    sheet.setFrozenRows(1);
  }

  return { sheet: sheet, created: created };
}

function getFirstExistingLoginSheet_(ss, names) {
  for (var i = 0; i < names.length; i++) {
    var sheet = ss.getSheetByName(names[i]);
    if (sheet) return sheet;
  }
  return null;
}

function normalizeLoginPhone_(value) {
  var digits = String(value || "")
    .replace(/[٠-٩]/g, function(d) { return "٠١٢٣٤٥٦٧٨٩".indexOf(d); })
    .replace(/\.0$/, "")
    .replace(/\D/g, "");

  if (digits.indexOf("00964") === 0) digits = digits.substring(5);
  if (digits.indexOf("964") === 0) digits = digits.substring(3);
  if (digits.indexOf("0") === 0) digits = digits.substring(1);
  return digits;
}

function normalizeLoginVehicle_(value) {
  return String(value || "")
    .replace(/[٠-٩]/g, function(d) { return "٠١٢٣٤٥٦٧٨٩".indexOf(d); })
    .replace(/[\u200E\u200F]/g, "")
    .replace(/[\s_\-\/\\.]+/g, "")
    .replace(/[^\u0600-\u06FFa-zA-Z0-9]/g, "")
    .trim()
    .toLowerCase();
}

function isLoginAccessEnabled_(value) {
  var text = normalizeText_(value).replace(/\s+/g, "");
  if (!text) return true;
  var disabled = {
    "0": true,
    "false": true,
    "no": true,
    "inactive": true,
    "disabled": true,
    "blocked": true,
    "موقوف": true,
    "معطل": true,
    "محظور": true,
    "غيرمفعل": true
  };
  return !disabled[text];
}
