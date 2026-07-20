var SPREADSHEET_ID = "1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM";

// ========================================
// json helper
// ========================================
function json(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

// ========================================
// رفع صورة
// ========================================
function processFile(base64Data, fileName) {
  try {
    var blob = Utilities.newBlob(
      Utilities.base64Decode(base64Data),
      "image/jpeg",
      fileName
    );
    var file = DriveApp.createFile(blob);
    file.setSharing(
      DriveApp.Access.ANYONE_WITH_LINK,
      DriveApp.Permission.VIEW
    );
    return file.getUrl();
  } catch (e) {
    return "";
  }
}

// ========================================
// فحص رقم الوصل
// ========================================
function checkDoc(data) {
  var ss        = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets    = ss.getSheets();
  var docNumber = String(data.docNumber || "").trim();

  if (!docNumber) {
    return { success: false, message: "docNumber مطلوب" };
  }

  for (var s = 0; s < sheets.length; s++) {
    var name = sheets[s].getName();
    if (!name.match(/^20/) && !name.match(/^F_20/)) continue;

    var rows = sheets[s].getDataRange().getValues();
    for (var i = 1; i < rows.length; i++) {
      if (String(rows[i][0]).trim() === docNumber) {
        return { success: true, exists: true };
      }
    }
  }

  return { success: true, exists: false };
}

// ========================================
// سجل السائق
// ========================================
function getDriverTrips(data) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();
  var trips = [];

  // يقبل object أو string
  var driverName = "";
  if (typeof data === "string") {
    driverName = String(data).trim();
  } else {
    driverName = String(
      data.driverName || data.name || data.driver || ""
    ).trim();
  }

  if (!driverName) {
    return { success: false, message: "driverName مطلوب" };
  }

  for (var s = 0; s < sheets.length; s++) {
    var name = sheets[s].getName();

    // شيتات الرحلات العادية فقط (2026_04) وليس المعمل (F_2026_04)
    if (!name.match(/^20/) || name.match(/^F_20/)) continue;

    var rows = sheets[s].getDataRange().getValues();

    for (var i = 1; i < rows.length; i++) {
      var rowDriver = String(rows[i][1] || "").trim();
      if (rowDriver !== driverName) continue;

      trips.push({
        docNumber:  String(rows[i][0]  || ""),
        driverName: String(rows[i][1]  || ""),
        carNumber:  String(rows[i][2]  || ""),
        loadDate:   String(rows[i][3]  || ""),
        unloadDate: String(rows[i][4]  || ""),
        quantity:   String(rows[i][5]  || ""),
        ownerType:  String(rows[i][6]  || ""),
        station:    String(rows[i][7]  || ""),
        fileUrl:    String(rows[i][8]  || ""),
        date:       String(rows[i][9]  || ""),
        liters:     String(rows[i][10] || ""),
        bojer:      String(rows[i][11] || ""),
        price:      String(rows[i][13] || ""),
        total:      String(rows[i][14] || ""),
        notes:      String(rows[i][15] || ""),
        status:     String(rows[i][15] || "").indexOf("REPORTED") > -1
                      ? "reported" : "ok"
      });
    }
  }

  trips.reverse();
  return { success: true, trips: trips };
}

// ========================================
// updateTrip
// ========================================
function updateTrip(driver, car, status, time) {
  var ss            = SpreadsheetApp.openById(SPREADSHEET_ID);
  var trackingSheet = ss.getSheetByName("gps_tracking");
  var tripSheet     = ss.getSheetByName("trips_auto");

  if (!trackingSheet || !tripSheet) return;

  var data = trackingSheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() !== String(driver).trim()) continue;

    var tripState  = data[i][6] || "no_trip";
    var movingTime = data[i][7] || "";
    var stopTime   = data[i][8] || "";

    if (status === "moving") {
      if (!movingTime) {
        trackingSheet.getRange(i + 1, 8).setValue(time);
        trackingSheet.getRange(i + 1, 7).setValue("moving");
        return;
      }

      var diff = (time - new Date(movingTime)) / 1000 / 60;

      if (diff >= 2 && tripState !== "in_trip") {
        tripSheet.appendRow([
          driver, car, movingTime, "", "", "", "open"
        ]);
        trackingSheet.getRange(i + 1, 7).setValue("in_trip");
      }

      trackingSheet.getRange(i + 1, 9).setValue("");
    }

    if (status === "stopped" && tripState === "in_trip") {
      if (!stopTime) {
        trackingSheet.getRange(i + 1, 9).setValue(time);
        return;
      }

      var diffStop = (time - new Date(stopTime)) / 1000 / 60;

      if (diffStop >= 5) {
        closeTrip(driver, new Date(stopTime));
        trackingSheet.getRange(i + 1, 7).setValue("no_trip");
        trackingSheet.getRange(i + 1, 8).setValue("");
        trackingSheet.getRange(i + 1, 9).setValue("");
      }
    }

    break;
  }
}

// ========================================
// closeTrip
// ========================================
function closeTrip(driver, endTime) {
  var ss        = SpreadsheetApp.openById(SPREADSHEET_ID);
  var tripSheet = ss.getSheetByName("trips_auto");
  if (!tripSheet) return;

  var data = tripSheet.getDataRange().getValues();

  for (var i = data.length - 1; i >= 1; i--) {
    if (String(data[i][0]).trim() !== String(driver).trim()) continue;
    if (String(data[i][6]).trim() !== "open") continue;

    var startTime       = new Date(data[i][2]);
    var durationMinutes = (endTime - startTime) / 1000 / 60;
    var durationHours   = (durationMinutes / 60).toFixed(2);
    var dist            = calculateTripDistance(driver, startTime, endTime);

    tripSheet.getRange(i + 1, 4).setValue(endTime);
    tripSheet.getRange(i + 1, 5).setValue(durationHours);
    tripSheet.getRange(i + 1, 6).setValue(dist);
    tripSheet.getRange(i + 1, 7).setValue("closed");

    break;
  }
}

// ========================================
// calculateTripDistance
// ========================================
function calculateTripDistance(driver, startTime, endTime) {
  var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("gps_history");
  if (!sheet) return 0;

  var data   = sheet.getDataRange().getValues();
  var points = [];

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0]).trim() !== String(driver).trim()) continue;

    var t = new Date(data[i][4]);
    if (t >= startTime && t <= endTime) {
      points.push({
        lat: Number(data[i][2]),
        lng: Number(data[i][3])
      });
    }
  }

  var total = 0;
  for (var i = 1; i < points.length; i++) {
    total += distance(
      points[i - 1].lat,
      points[i - 1].lng,
      points[i].lat,
      points[i].lng
    );
  }

  return (total / 1000).toFixed(2);
}

// ========================================
// saveTrip
// ========================================
function saveTrip(data) {
  if (!data) {
    return { success: false, message: "No data" };
  }

  var rawDocNumber = String(data.docNumber || "").trim();
  var normalizedDocNumber = normalizeDocNumber_(rawDocNumber);
  if (!normalizedDocNumber) {
    return { success: false, message: "رقم الوصل مطلوب" };
  }

  var monthPolicy = resolveSubmissionMonthPolicy_(data);
  if (!monthPolicy.success) {
    return monthPolicy;
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheetName = monthPolicy.monthKey;
  var sheet = ensureTripsSheet_(ss, sheetName);

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(15000);

    var duplicate = findExistingDocNumber_(ss, normalizedDocNumber);
    if (duplicate.found) {
      return {
        success: false,
        exists: true,
        isExists: true,
        message: "رقم الوصل مكرر وموجود مسبقًا",
        docNumber: rawDocNumber,
        existingSheet: duplicate.sheetName,
        existingRow: duplicate.rowNumber
      };
    }

    var imageUrl = "";
    try {
      if (data.fileData && data.fileData !== "") {
        imageUrl = processFile(data.fileData, (rawDocNumber || "trip") + ".jpg");
      }
    } catch (e) {
      imageUrl = "";
    }

    var sendTime = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

    sheet.appendRow([
      rawDocNumber,
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
      data.kroa || data.tripPrice || data.driverFare || 0,
      data.notes || ""
    ]);

    return {
      success: true,
      exists: false,
      isExists: false,
      message: "تم حفظ النقلة بنجاح",
      sheetName: sheetName,
      tripMonth: monthPolicy.monthKey,
      sendTime: sendTime
    };
  } finally {
    try {
      lock.releaseLock();
    } catch (e) {
    }
  }
}

// ========================================
// saveFactory
// ========================================
function saveFactory(data) {
  if (!data) return { success: false, message: "No data" };

  var ss        = SpreadsheetApp.openById(SPREADSHEET_ID);
  var today     = new Date();
  var sheetName = "F_" + today.getFullYear() + "_" +
                  ("0" + (today.getMonth() + 1)).slice(-2);

  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow([
      "رقم الوصل",    // A
      "اسم السائق",   // B
      "رقم السيارة",  // C
      "الكمية",       // D
      "اسم المعمل",   // E
      "رابط الصورة",  // F
      "تاريخ التفريغ" // G
    ]);
  }

  var imageUrl = "";
  try {
    if (data.fileData && data.fileData !== "") {
      var fileName = "F_" + (data.docNumber || new Date().getTime()) + ".jpg";
      imageUrl = processFile(data.fileData, fileName);
    }
  } catch (e) {
    imageUrl = "";
  }

  var sendTime = Utilities.formatDate(
    new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss"
  );

  sheet.appendRow([
    data.docNumber                        || "",
    data.driverName  || data.driver       || "",
    data.carNumber                        || "",
    data.quantity                         || 0,
    data.factory     || data.factoryName  || "",
    imageUrl,
    data.unloadDate  || sendTime
  ]);

  return {
    success:   true,
    message:   "تم حفظ وصل المعمل بنجاح",
    sheetName: sheetName,
    sendTime:  sendTime
  };
}