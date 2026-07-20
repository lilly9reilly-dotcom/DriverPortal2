// ========================================
// Wallet Engine - نسخة نهائية
// ملف: Wallet.gs
// الربح = الإجمالي مباشرة (index 14)
// أو الكمية × سعر النقل (index 13) كـ fallback
// شيت 2026_04 أعمدته:
// 0=رقم الوصل, 1=اسم السائق, 2=رقم السيارة
// 3=تاريخ التحميل, 4=تاريخ التفريغ, 5=الكمية
// 6=المالك, 7=المحطة, 8=رابط الصورة
// 9=وقت الإرسال, 10=لترات الكاز, 11=رقم البوجر
// 12=المسافة, 13=سعر النقل, 14=الإجمالي, 15=ملاحظات
// ========================================

var DRIVER_COL   = 1;
var QTY_COL      = 5;
var LITERS_COL   = 10;
var PRICE_COL    = 13;
var TOTAL_COL    = 14;

// ========================================
// getDriverWallet - المحفظة الكاملة
// ========================================
function getDriverWallet(data) {
  var rawDriverName = data.driverName || data.name || data.driver || "";
  var driverName = normalizeText_(rawDriverName);

  if (!driverName) {
    return { success: false, message: "driverName is required" };
  }

  var ss     = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();

  var trips           = 0;
  var quantity        = 0;
  var liters          = 0;
  var profit          = 0;
  var maintenanceCost = 0;

  for (var s = 0; s < sheets.length; s++) {
    var sheet     = sheets[s];
    var sheetName = sheet.getName();

    var isFactorySheet = /^F_\d{4}_\d{2}$/i.test(sheetName);
    if (!/^\d{4}_\d{2}$/.test(sheetName) && !isFactorySheet) continue;

    var values = sheet.getDataRange().getValues();
    if (values.length < 2) continue;

    for (var i = 1; i < values.length; i++) {
      var row       = values[i];
      var rowDriver = normalizeText_(row[DRIVER_COL]);

      if (!rowDriver || rowDriver !== driverName) continue;

      var rowQuantity = normalizeQuantityForMoney_(toNumber_(row[isFactorySheet ? 3 : QTY_COL]));
      var rowLiters   = isFactorySheet ? 0 : toNumber_(row[LITERS_COL]);
      var rowPrice    = isFactorySheet ? extractDriverFareFromNotes_(row[7]) : toNumber_(row[PRICE_COL]);

      var rowProfit = rowPrice > 0 ? rowPrice : 0;

      trips++;
      quantity += rowQuantity;
      liters   += rowLiters;
      profit   += rowProfit;
    }
  }

  // تكاليف الصيانة
  var maintenanceSheet = ss.getSheetByName("سجل الصيانة");
  if (maintenanceSheet) {
    var mValues = maintenanceSheet.getDataRange().getValues();
    for (var j = 1; j < mValues.length; j++) {
      var mDriver = normalizeText_(mValues[j][1]);
      var cost    = toNumber_(mValues[j][8]);
      if (mDriver === driverName) {
        maintenanceCost += cost;
      }
    }
  }

  return {
    success:     true,
    trips:       trips,
    quantity:    quantity,
    liters:      liters,
    profit:      profit,
    maintenance: maintenanceCost,
    netProfit:   profit - maintenanceCost
  };
}

// ========================================
// getDriverStats - للـ Dashboard
// ========================================
function getDriverStats(driverName) {
  return getDriverWallet({ driverName: driverName });
}

// ========================================
// getDriverStatsToday - إحصائيات اليوم
// ========================================
function getDriverStatsToday(driverName) {
  var normalizedDriver = normalizeText_(driverName || "");

  if (!normalizedDriver) {
    return { success: false, message: "driverName is required" };
  }

  var ss     = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();
  var today  = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd");

  var trips    = 0;
  var quantity = 0;
  var liters   = 0;
  var profit   = 0;

  for (var s = 0; s < sheets.length; s++) {
    var sheet     = sheets[s];
    var sheetName = sheet.getName();

    if (!/^\d{4}_\d{2}$/.test(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (values.length < 2) continue;

    for (var i = 1; i < values.length; i++) {
      var row       = values[i];
      var rowDriver = normalizeText_(row[DRIVER_COL]);

      if (!rowDriver || rowDriver !== normalizedDriver) continue;

      // تاريخ التحميل = index 3
      var rowDate = "";
      try {
        var dateVal = row[3];
        if (dateVal) {
          rowDate = Utilities.formatDate(new Date(dateVal), "Asia/Baghdad", "yyyy-MM-dd");
        }
      } catch (e) {
        rowDate = "";
      }

      if (rowDate !== today) continue;

      var rowQuantity = toNumber_(row[QTY_COL]);
      var rowLiters   = toNumber_(row[LITERS_COL]);
      var rowPrice    = toNumber_(row[PRICE_COL]);
      var rowProfit   = rowPrice > 0 ? rowPrice : 0;

      trips++;
      quantity += rowQuantity;
      liters   += rowLiters;
      profit   += rowProfit;
    }
  }

  return {
    success:  true,
    trips:    trips,
    quantity: quantity,
    liters:   liters,
    profit:   profit
  };
}

// ========================================
// دوال مساعدة
// ========================================

function normalizeText_(value) {
  return String(value || "")
    .replace(/\u00A0/g, " ")
    .replace(/\s+/g, " ")
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .trim()
    .toLowerCase();
}

function normalizeHeader_(value) {
  return normalizeText_(value)
    .replace(/[:\-_]/g, "")
    .replace(/\s+/g, "");
}

function findHeaderIndex_(headers, possibleNames) {
  var normalizedTargets = possibleNames.map(function(name) {
    return normalizeHeader_(name);
  });
  for (var i = 0; i < headers.length; i++) {
    if (normalizedTargets.indexOf(headers[i]) > -1) {
      return i;
    }
  }
  return -1;
}

function toNumber_(value) {
  if (value === null || value === "" || typeof value === "undefined") return 0;
  if (typeof value === "number") return value;
  var s = String(value).replace(/[^\d.\-]/g, "").trim();
  var n = parseFloat(s);
  return isNaN(n) ? 0 : n;
}