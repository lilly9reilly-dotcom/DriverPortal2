function loginDriver(data) {
  data = data || {};

  var requestedName = String(data.name || data.driverName || "").trim();
  var requestedPhone = normalizeLoginPhone_(data.phone || "");
  var requestedCar = normalizeLoginVehicle_(data.carNumber || data.car || "");

  if (!requestedName || !requestedPhone || !requestedCar) {
    return {
      success: false,
      message: "يجب إدخال اسم السائق ورقم الهاتف ورقم السيارة"
    };
  }

  var authorized = findAuthorizedDriverForLogin_(requestedName, requestedPhone, requestedCar);
  if (!authorized.allowed) {
    return {
      success: false,
      message: authorized.message || "هذا المستخدم غير مصرح له بالدخول. يرجى التواصل مع الإدارة."
    };
  }

  return {
    success: true,
    message: "تم تسجيل الدخول",
    driver: authorized.driverName || requestedName,
    carNumber: authorized.carNumber || String(data.carNumber || data.car || "").trim(),
    newDriver: false,
    access: "authorized"
  };
}

function findAuthorizedDriverForLogin_(driverName, phone, carNumber) {
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

  if (!sheet) {
    return {
      allowed: false,
      message: "قائمة تصاريح السائقين غير موجودة. أنشئ شيت AuthorizedDrivers وأضف السائقين المصرح لهم."
    };
  }

  var values = sheet.getDataRange().getValues();
  if (!values || values.length < 2) {
    return {
      allowed: false,
      message: "قائمة التصاريح فارغة. أضف السائق قبل السماح بالدخول."
    };
  }

  var colMap = buildColumnMap_(values[0]);
  var normalizedName = normalizeText_(driverName);
  var normalizedPhone = normalizeLoginPhone_(phone);
  var normalizedCar = normalizeLoginVehicle_(carNumber);

  for (var i = 1; i < values.length; i++) {
    var row = values[i];
    if (!row || row.join("").trim() === "") continue;

    var rowNameRaw = getCellByAliases_(row, colMap, ["driverName", "driver", "name", "اسم السائق", "السائق"], 0);
    var rowPhoneRaw = getCellByAliases_(row, colMap, ["phone", "mobile", "driverPhone", "رقم الهاتف", "الهاتف"], 1);
    var rowCarRaw = getCellByAliases_(row, colMap, ["carNumber", "car", "vehicle", "plate", "رقم السيارة", "السيارة"], 2);
    var rowActiveRaw = getCellByAliases_(row, colMap, ["active", "enabled", "allowed", "status", "الحالة", "مفعل"], 3);

    var rowName = normalizeText_(rowNameRaw);
    var rowPhone = normalizeLoginPhone_(rowPhoneRaw);
    var rowCar = normalizeLoginVehicle_(rowCarRaw);

    if (rowName === normalizedName && rowPhone === normalizedPhone && rowCar === normalizedCar) {
      if (!isLoginAccessEnabled_(rowActiveRaw)) {
        return {
          allowed: false,
          message: "هذا الحساب غير مفعل من الإدارة."
        };
      }

      return {
        allowed: true,
        driverName: String(rowNameRaw || driverName).trim(),
        carNumber: String(rowCarRaw || carNumber).trim()
      };
    }
  }

  return {
    allowed: false,
    message: "بيانات الدخول غير مصرح بها. يرجى مراجعة الإدارة."
  };
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
    .replace(/\s+/g, "")
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
