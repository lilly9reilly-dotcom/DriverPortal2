// ========================================
// Fleet Tracking - Final Unified Apps Script
// نسخة نهائية موحّدة للنشر: Android + Dashboard + GPS
// آمنة على بنية الشيت الحالية بدون تغييرات مكسرة
// ========================================

var SPREADSHEET_ID = "1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM";

function doGet(e) {
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function handleRequest(e) {
  var data = extractRequestData(e);
  var action = String(data.action || "").trim();

  if (!action) {
    action = hasGpsPayload_(data) ? "gps" : "health";
  }

  try {
    if (action === "health") {
      return json(getSystemHealth());
    }

    if (action === "gps") {
      return json(handleGPS(data));
    }

    if (action === "drivers" || action === "get_drivers") {
      return json(getDriversLive());
    }

    if (action === "route") {
      return json(
        getVehicleRoute({
          vehicle: data.vehicle || "",
          carNumber: data.carNumber || "",
          driverName: data.driverName || data.driver || "",
          id: data.id || "",
          limit: data.limit || 160,
        })
      );
    }

    if (action === "cleanup") {
      return json(cleanupDuplicates());
    }

    if (action === "alerts") {
      return json(getRecentAlerts());
    }

    if (action === "autoTrips") {
      return json(getAutoTrips());
    }

    if (action === "login") {
      return json(runIfExists_("loginDriver", [data], { success: false, message: "loginDriver غير منشور في هذه النسخة" }));
    }

    if (action === "trip") {
      return json(saveTrip(data));
    }

    if (action === "factory") {
      var factoryResult = runIfExists_("saveFactoryMain_", [data], null);
      if (factoryResult) {
        return json(factoryResult);
      }
      return json(runIfExists_("saveFactory", [data], { success: false, message: "saveFactory غير منشور في هذه النسخة" }));
    }

    if (action === "history") {
      return json(getDriverTrips(data));
    }

    if (action === "wallet" || action === "dashboard") {
      return json(getDriverWallet(data));
    }

    if (action === "saveMaintenance") {
      return json(runIfExists_("saveMaintenance", [data], { success: false, message: "saveMaintenance غير منشور في هذه النسخة" }));
    }

    if (action === "getMaintenance") {
      return json(getMaintenanceRequests(data));
    }

    if (action === "checkDoc") {
      return json(runIfExists_("checkDoc", [data], { success: false, message: "checkDoc غير منشور في هذه النسخة" }));
    }

    if (action === "reportIssue") {
      return json(reportIssue(data));
    }

    return json({
      success: false,
      message: "Unknown action: " + action,
      available_actions: [
        "health",
        "gps",
        "drivers",
        "get_drivers",
        "route",
        "cleanup",
        "alerts",
        "autoTrips",
        "trip",
        "history",
        "wallet",
        "dashboard",
        "reportIssue",
        "getMaintenance"
      ]
    });
  } catch (err) {
    return json({ success: false, message: err.toString(), action: action });
  }
}

function extractRequestData(e) {
  var data = {};

  if (e && e.parameter) {
    data = e.parameter;
  }

  if (e && e.postData && e.postData.contents) {
    var content = String(e.postData.contents || "").trim();

    if (content) {
      try {
        if (content.charAt(0) === "{") {
          var parsed = JSON.parse(content);
          if (parsed && typeof parsed === "object") {
            data = parsed;
          }
        }
      } catch (parseError) {
        Logger.log("JSON parse skipped: " + parseError);
      }
    }
  }

  return data || {};
}

function hasGpsPayload_(data) {
  return !!(
    data &&
    (data.driverName || data.driver) &&
    (data.carNumber || data.car) &&
    data.lat &&
    data.lng
  );
}

function json(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function runIfExists_(functionName, args, fallbackValue) {
  try {
    if (typeof this[functionName] === "function") {
      return this[functionName].apply(this, args || []);
    }
  } catch (err) {
    return { success: false, message: err.toString(), action: functionName };
  }

  return fallbackValue;
}

function normalizeCarValue(value) {
  return String(value || "").trim().toLowerCase();
}

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

function toNumber_(value) {
  if (value === null || value === "" || typeof value === "undefined") return 0;

  var s = String(value)
    .replace(/[^\d.\-]/g, "")
    .trim();

  var n = parseFloat(s);
  return isNaN(n) ? 0 : n;
}

function findRowByCar(sheet, car, carColumnIndex) {
  if (!sheet) return -1;

  var targetCar = normalizeCarValue(car);
  if (!targetCar) return -1;

  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    var rowCar = normalizeCarValue(values[i][carColumnIndex - 1]);
    if (rowCar === targetCar) {
      return i + 1;
    }
  }

  return -1;
}

function buildCompactRouteHistory(existingValue, lat, lng, status, speed, now) {
  var points = [];

  try {
    points = existingValue ? JSON.parse(existingValue) : [];
    if (!Array.isArray(points)) points = [];
  } catch (e) {
    points = [];
  }

  var nextPoint = {
    lat: lat,
    lng: lng,
    timestamp: now.getTime(),
    time: now.toISOString(),
    status: status,
    speed: speed
  };

  if (points.length === 0) {
    points.push(nextPoint);
    return JSON.stringify(points);
  }

  var lastPoint = points[points.length - 1];
  var movedMeters = distance(Number(lastPoint.lat || 0), Number(lastPoint.lng || 0), lat, lng);
  var lastStatus = String(lastPoint.status || "").trim().toLowerCase();
  var lastTimestamp = Number(lastPoint.timestamp || 0);
  var secondsDiff = lastTimestamp ? (now.getTime() - lastTimestamp) / 1000 : 999999;

  if (movedMeters >= 25 || lastStatus !== status || secondsDiff >= 60) {
    points.push(nextPoint);
  } else {
    points[points.length - 1] = nextPoint;
  }

  if (points.length > 120) {
    points = points.slice(points.length - 120);
  }

  return JSON.stringify(points);
}

function removeDuplicateRowsByCar(sheet, carColumnIndex) {
  if (!sheet) return 0;

  var data = sheet.getDataRange().getValues();
  var seen = {};
  var rowsToDelete = [];

  for (var i = data.length - 1; i > 0; i--) {
    var car = normalizeCarValue(data[i][carColumnIndex - 1]);
    if (!car) continue;

    if (seen[car]) {
      rowsToDelete.push(i + 1);
    } else {
      seen[car] = true;
    }
  }

  rowsToDelete.sort(function(a, b) { return b - a; });

  for (var j = 0; j < rowsToDelete.length; j++) {
    sheet.deleteRow(rowsToDelete[j]);
  }

  return rowsToDelete.length;
}

function ensureGpsSheets_(ss) {
  var trackingSheet = ss.getSheetByName("gps_tracking");
  if (!trackingSheet) {
    trackingSheet = ss.insertSheet("gps_tracking");
    trackingSheet.appendRow([
      "اسم السائق",
      "رقم السيارة",
      "lat",
      "lng",
      "الوقت",
      "الحالة",
      "trip_status",
      "moving_time",
      "stop_time",
      "last_update",
      "speed"
    ]);
  }

  var historySheet = ss.getSheetByName("gps_history");
  if (!historySheet) {
    historySheet = ss.insertSheet("gps_history");
    historySheet.appendRow([
      "driver",
      "carNumber",
      "lat",
      "lng",
      "time",
      "status",
      "speed",
      "route_points_json"
    ]);
  } else if (historySheet.getLastColumn() < 8) {
    historySheet.getRange(1, 8).setValue("route_points_json");
  }

  return {
    trackingSheet: trackingSheet,
    historySheet: historySheet
  };
}

function handleGPS(data) {
  if (!data) {
    return { success: false, message: "No data" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ensureGpsSheets_(ss);
  var trackingSheet = sheets.trackingSheet;
  var historySheet = sheets.historySheet;

  var driver = String(data.driverName || data.driver || "").trim();
  var car    = String(data.carNumber  || data.car    || "").trim();
  var lat    = Number(data.lat || 0);
  var lng    = Number(data.lng || 0);
  var status = String(data.status || "stopped").trim().toLowerCase();
  var speed  = Number(data.speed || 0);
  var now    = new Date();

  if (!driver || !car || !lat || !lng) {
    return {
      success: false,
      message: "Missing: driverName, carNumber, lat, lng"
    };
  }

  var foundRow = findRowByCar(trackingSheet, car, 2);

  if (foundRow > -1) {
    var currentTrip = String(trackingSheet.getRange(foundRow, 7).getValue() || "no_trip");

    trackingSheet.getRange(foundRow, 1, 1, 11).setValues([[
      driver,
      car,
      lat,
      lng,
      now,
      status,
      currentTrip,
      trackingSheet.getRange(foundRow, 8).getValue() || "",
      trackingSheet.getRange(foundRow, 9).getValue() || "",
      now,
      speed
    ]]);
  } else {
    trackingSheet.appendRow([
      driver, car, lat, lng, now,
      status, "no_trip", "", "", now, speed
    ]);
  }

  var historyRow = findRowByCar(historySheet, car, 2);
  var existingRouteJson = historyRow > -1
    ? String(historySheet.getRange(historyRow, 8).getValue() || "")
    : "";
  var compactRouteJson = buildCompactRouteHistory(existingRouteJson, lat, lng, status, speed, now);

  if (historyRow > -1) {
    historySheet.getRange(historyRow, 1, 1, 8).setValues([[
      driver,
      car,
      lat,
      lng,
      now,
      status,
      speed,
      compactRouteJson
    ]]);
  } else {
    historySheet.appendRow([
      driver,
      car,
      lat,
      lng,
      now,
      status,
      speed,
      compactRouteJson
    ]);
  }

  try { updateTrip(driver, car, status, now); } catch (e) {}
  try { checkAlerts(driver, car, status, speed, now); } catch (e) {}

  return {
    success: true,
    message: "GPS saved",
    driver: driver,
    carNumber: car,
    lat: lat,
    lng: lng,
    status: status,
    speed: speed,
    timestamp: now
  };
}

function getDriversLive() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var trackingSheet = ss.getSheetByName("gps_tracking");

  if (!trackingSheet) {
    return { success: true, count: 0, drivers: [] };
  }

  var data = trackingSheet.getDataRange().getValues();
  var drivers = [];

  for (var i = 1; i < data.length; i++) {
    if (!data[i][1]) continue;

    drivers.push({
      driver: String(data[i][0] || ""),
      carNumber: String(data[i][1] || ""),
      lat: Number(data[i][2] || 0),
      lng: Number(data[i][3] || 0),
      time: data[i][4],
      status: String(data[i][5] || "unknown"),
      trip_status: String(data[i][6] || "no_trip"),
      moving_time: data[i][7] || "",
      stop_time: data[i][8] || "",
      last_update: data[i][9],
      speed: Number(data[i][10] || 0)
    });
  }

  return {
    success: true,
    count: drivers.length,
    drivers: drivers
  };
}

function getVehicleRoute(params) {
  var vehicleId = params.vehicle || params.carNumber || params.id || "";
  var limit = params.limit || 160;
  return getVehicleRouteData(vehicleId, limit);
}

function getVehicleRouteData(vehicleId, limit) {
  var normalizedVehicle = String(vehicleId || "").trim().toLowerCase();
  var safeLimit = Math.max(10, Math.min(Number(limit) || 160, 500));

  if (!normalizedVehicle) {
    return { success: false, message: "Vehicle is required", points: [] };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var historySheet = ss.getSheetByName("gps_history");
  var trackingSheet = ss.getSheetByName("gps_tracking");
  var points = [];

  if (historySheet) {
    var rows = historySheet.getDataRange().getValues();

    for (var i = 1; i < rows.length; i++) {
      var rowCar = normalizeCarValue(rows[i][1]);
      if (rowCar !== normalizedVehicle) continue;

      var rawRouteJson = String(rows[i][7] || "").trim();
      if (rawRouteJson) {
        try {
          var parsedPoints = JSON.parse(rawRouteJson);
          if (Array.isArray(parsedPoints) && parsedPoints.length > 0) {
            points = parsedPoints;
            break;
          }
        } catch (e) {}
      }

      var lat = Number(rows[i][2] || 0);
      var lng = Number(rows[i][3] || 0);
      if (!lat || !lng) continue;

      var rawTime = rows[i][4] || new Date();
      var pointTime = new Date(rawTime);

      points.push({
        lat: lat,
        lng: lng,
        timestamp: pointTime.getTime(),
        time: pointTime,
        status: String(rows[i][5] || "unknown"),
        speed: Number(rows[i][6] || 0)
      });
      break;
    }
  }

  if (points.length === 0 && trackingSheet) {
    var trackingRows = trackingSheet.getDataRange().getValues();

    for (var j = 1; j < trackingRows.length; j++) {
      var trackingCar = String(trackingRows[j][1] || "").trim().toLowerCase();
      if (trackingCar !== normalizedVehicle) continue;

      var currentLat = Number(trackingRows[j][2] || 0);
      var currentLng = Number(trackingRows[j][3] || 0);
      if (!currentLat || !currentLng) continue;

      var trackingTime = new Date(trackingRows[j][4] || new Date());
      points.push({
        lat: currentLat,
        lng: currentLng,
        timestamp: trackingTime.getTime(),
        time: trackingTime,
        status: String(trackingRows[j][5] || "unknown"),
        speed: Number(trackingRows[j][10] || 0)
      });
      break;
    }
  }

  points.sort(function(a, b) {
    return a.timestamp - b.timestamp;
  });

  return {
    success: true,
    vehicle: vehicleId,
    points: points.slice(-safeLimit)
  };
}

function cleanupDuplicates() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var trackingSheet = ss.getSheetByName("gps_tracking");
  var historySheet = ss.getSheetByName("gps_history");

  if (!trackingSheet || !historySheet) {
    return { success: false, message: "Sheets not found" };
  }

  var trackingRemoved = removeDuplicateRowsByCar(trackingSheet, 2);
  var historyRemoved = removeDuplicateRowsByCar(historySheet, 2);

  return {
    success: true,
    message: "Cleanup completed",
    tracking_duplicates_removed: trackingRemoved,
    history_duplicates_removed: historyRemoved,
    tracking_rows_after_cleanup: Math.max(trackingSheet.getLastRow() - 1, 0),
    history_rows_after_cleanup: Math.max(historySheet.getLastRow() - 1, 0),
    timestamp: new Date()
  };
}

function getSystemHealth() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var trackingSheet = ss.getSheetByName("gps_tracking");
  var historySheet = ss.getSheetByName("gps_history");
  var alertsSheet = ss.getSheetByName("alerts");

  return {
    success: true,
    status: "healthy",
    tracking_rows: trackingSheet ? Math.max(trackingSheet.getLastRow() - 1, 0) : 0,
    history_rows: historySheet ? Math.max(historySheet.getLastRow() - 1, 0) : 0,
    alerts_rows: alertsSheet ? Math.max(alertsSheet.getLastRow() - 1, 0) : 0,
    timestamp: new Date()
  };
}

function getRecentAlerts() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("alerts");

  if (!sheet) {
    return { success: true, alerts: [] };
  }

  var values = sheet.getDataRange().getValues();
  var alerts = [];

  for (var i = 1; i < values.length; i++) {
    alerts.push({
      time: values[i][0] || "",
      driver: values[i][1] || "",
      carNumber: values[i][2] || "",
      type: values[i][3] || "",
      message: values[i][4] || "",
      level: values[i][5] || "info"
    });
  }

  alerts.reverse();
  return { success: true, alerts: alerts.slice(0, 50) };
}

function getAutoTrips() {
  var payload = getDriversLive();
  if (!payload.success) return payload;

  var active = 0;
  for (var i = 0; i < payload.drivers.length; i++) {
    var driver = payload.drivers[i];
    var speed = Number(driver.speed || 0);
    var tripStatus = String(driver.trip_status || "").toLowerCase();
    if (tripStatus === "active" || tripStatus === "in_trip" || speed >= 6) {
      active++;
    }
  }

  return {
    success: true,
    activeTrips: active,
    totalDrivers: payload.drivers.length
  };
}

function distance(lat1, lon1, lat2, lon2) {
  if (!lat1 || !lon1 || !lat2 || !lon2) return 0;

  var R = 6371000;
  var dLat = (lat2 - lat1) * Math.PI / 180;
  var dLon = (lon2 - lon1) * Math.PI / 180;

  var a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) *
    Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);

  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function updateTrip(driver, car, status, now) {
  // intentionally safe stub
}

function checkAlerts(driver, car, status, speed, now) {
  // intentionally safe stub
}

function saveTrip(data) {
  if (!data) {
    return { success: false, message: "No data" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var today = new Date();
  var sheetName = today.getFullYear() + "_" + ("0" + (today.getMonth() + 1)).slice(-2);

  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow([
      "رقم الوصل",
      "اسم السائق",
      "رقم السيارة",
      "تاريخ التحميل",
      "تاريخ التفريغ",
      "الكمية",
      "المالك",
      "المحطة",
      "رابط الصورة",
      "وقت الإرسال",
      "لترات الكاز",
      "رقم البوجر",
      "المسافة",
      "سعر النقل",
      "ملاحظات"
    ]);
  }

  var imageUrl = "";
  try {
    if (data.fileData && data.fileData !== "" && typeof processFile === "function") {
      imageUrl = processFile(data.fileData, (data.docNumber || "trip") + ".jpg");
    }
  } catch (e) {
    imageUrl = "";
  }

  var sendTime = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

  sheet.appendRow([
    data.docNumber || "",
    data.driverName || "",
    data.carNumber || "",
    data.loadDate || "",
    data.unloadDate || "",
    data.quantity || 0,
    data.owner || data.ownerType || "",
    data.station || data.destination || "",
    imageUrl,
    sendTime,
    data.liters || 0,
    data.bojer || data.bogerNumber || "",
    data.distance || 0,
    data.tripPrice || data.price || 0,
    data.notes || ""
  ]);

  return {
    success: true,
    message: "تم حفظ النقلة بنجاح",
    sheetName: sheetName,
    sendTime: sendTime
  };
}

function getMaintenanceRequests(params) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("سجل الصيانة");

  if (!sheet) {
    return { success: true, requests: [] };
  }

  var carNumber = "";
  if (params && params.carNumber) {
    carNumber = String(params.carNumber).trim();
  }

  var dataSheet = sheet.getDataRange().getValues();
  var requests = [];

  for (var i = 1; i < dataSheet.length; i++) {
    var vehicle = String(dataSheet[i][2] || "").trim();

    if (carNumber && vehicle !== carNumber) {
      continue;
    }

    requests.push({
      requestId: dataSheet[i][0],
      driver: dataSheet[i][1],
      vehicle: vehicle,
      problem: dataSheet[i][3],
      status: dataSheet[i][4],
      requestDate: dataSheet[i][5],
      repairDate: dataSheet[i][6] || "",
      type: dataSheet[i][7] || "",
      cost: String(dataSheet[i][8] || ""),
      location: dataSheet[i][9] || "",
      notes: dataSheet[i][10] || ""
    });
  }

  requests.sort(function(a, b) {
    return String(b.requestDate).localeCompare(String(a.requestDate));
  });

  return {
    success: true,
    requests: requests
  };
}

function getDriverWallet(data) {
  var rawName = data.driverName || data.name || data.driver || "";
  var driverName = normalizeText_(rawName);

  if (!driverName) {
    return { success: false, message: "driverName is required" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();

  var trips = 0;
  var quantity = 0;
  var liters = 0;
  var profit = 0;
  var maintenanceCost = 0;

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = sheet.getName();

    if (!/^\d{4}_\d{2}$/.test(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (values.length < 2) continue;

    for (var i = 1; i < values.length; i++) {
      var row = values[i];
      var rowDriver = normalizeText_(row[1]);

      if (!rowDriver || rowDriver !== driverName) continue;

      var rowQty = toNumber_(row[5]);
      var rowLiters = toNumber_(row[10]);
      var rowPrice = toNumber_(row[13]);

      trips++;
      quantity += rowQty;
      liters += rowLiters;
      profit += rowPrice;
    }
  }

  var maintenanceSheet = ss.getSheetByName("سجل الصيانة");
  if (maintenanceSheet) {
    var mValues = maintenanceSheet.getDataRange().getValues();
    for (var j = 1; j < mValues.length; j++) {
      var mDriver = normalizeText_(mValues[j][1]);
      var cost = toNumber_(mValues[j][8]);
      if (mDriver === driverName) {
        maintenanceCost += cost;
      }
    }
  }

  return {
    success: true,
    trips: trips,
    quantity: quantity,
    liters: liters,
    profit: profit,
    maintenance: maintenanceCost,
    netProfit: profit - maintenanceCost
  };
}

function getDriverTrips(data) {
  var rawName = data.driverName || data.name || data.driver || "";
  var driverName = normalizeText_(rawName);

  if (!driverName) {
    return { success: false, message: "driverName is required" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();
  var trips = [];

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = sheet.getName();

    if (!/^\d{4}_\d{2}$/.test(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (values.length < 2) continue;

    for (var i = 1; i < values.length; i++) {
      var row = values[i];
      var rowDriver = normalizeText_(row[1]);

      if (!rowDriver || rowDriver !== driverName) continue;

      trips.push({
        docNumber: String(row[0] || ""),
        driverName: String(row[1] || ""),
        carNumber: String(row[2] || ""),
        loadDate: String(row[3] || ""),
        unloadDate: String(row[4] || ""),
        quantity: String(row[5] || ""),
        owner: String(row[6] || ""),
        station: String(row[7] || ""),
        imageUrl: String(row[8] || ""),
        sendTime: String(row[9] || ""),
        liters: String(row[10] || ""),
        bogerNumber: String(row[11] || ""),
        distance: String(row[12] || ""),
        price: String(row[13] || ""),
        notes: String(row[14] || ""),
        date: String(row[9] || ""),
        status: String(row[14] || "").indexOf("REPORTED") > -1 ? "reported" : "ok"
      });
    }
  }

  trips.reverse();
  return { success: true, trips: trips };
}

function reportIssue(data) {
  var docNumber = String(data.docNumber || "").trim();
  var driverName = normalizeText_(data.driverName || "");
  var issueType = String(data.issueType || "").trim();

  if (!docNumber || !driverName) {
    return { success: false, message: "docNumber و driverName مطلوبان" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var issuesSheet = ss.getSheetByName("issues");

  if (!issuesSheet) {
    issuesSheet = ss.insertSheet("issues");
    issuesSheet.appendRow([
      "وقت التبليغ",
      "اسم السائق",
      "رقم الوصل",
      "نوع المشكلة",
      "ملاحظة",
      "الحالة"
    ]);
  }

  var time = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

  issuesSheet.appendRow([
    time,
    String(data.driverName || "").trim(),
    docNumber,
    issueType,
    String(data.note || "").trim(),
    "open"
  ]);

  var sheets = ss.getSheets();
  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = sheet.getName();

    if (!/^\d{4}_\d{2}$/.test(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (values.length < 2) continue;

    for (var i = 1; i < values.length; i++) {
      var row = values[i];
      var rowDoc = String(row[0] || "").trim();
      var rowDriver = normalizeText_(row[1]);

      if (rowDoc !== docNumber || rowDriver !== driverName) continue;

      var currentNotes = String(row[14] || "");
      var newNote = "REPORTED: " + issueType;

      if (currentNotes.indexOf("REPORTED") === -1) {
        sheet.getRange(i + 1, 15).setValue(currentNotes ? currentNotes + " | " + newNote : newNote);
      }

      return {
        success: true,
        message: "تم الإبلاغ عن المشكلة بنجاح",
        docNumber: docNumber,
        issueType: issueType
      };
    }
  }

  return {
    success: true,
    message: "تم حفظ البلاغ في issues لكن لم يتم العثور على الوصل في النقلات",
    docNumber: docNumber,
    issueType: issueType
  };
}
