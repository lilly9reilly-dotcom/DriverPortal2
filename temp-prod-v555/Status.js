// ========================================
// Trip Engine - يعتمد على رقم السيارة
// Status.gs
// ========================================

function updateTrip(driver, car, status, time, dist) {

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var trackingSheet = ss.getSheetByName("gps_tracking");
  var tripSheet = ss.getSheetByName("trips_auto");

  var data = trackingSheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {

    var sheetDriver = data[i][0];
    var sheetCar = String(data[i][1]);

    if (sheetCar == String(car)) {

      var tripState = data[i][6];
      var movingTime = data[i][7];
      var stopTime = data[i][8];
      var totalDistance = Number(data[i][9] || 0);

      // ============================
      // بداية الحركة
      // ============================
      if (status == "moving") {

        // سجل بداية الحركة
        if (!movingTime) {
          trackingSheet.getRange(i+1,8).setValue(time);
          trackingSheet.getRange(i+1,7).setValue("moving");
          return;
        }

        var diff = (time - new Date(movingTime)) / 1000 / 60;

        // بعد دقيقتين حركة = بدء رحلة
        if (diff >= 2 && tripState != "in_trip") {

          tripSheet.appendRow([
            sheetDriver,     // A
            sheetCar,        // B
            movingTime,      // C start
            "",              // D end
            "",              // E duration
            0,               // F distance
            "open"           // G status
          ]);

          trackingSheet.getRange(i+1,7).setValue("in_trip");
        }

        // إزالة وقت التوقف
        trackingSheet.getRange(i+1,9).setValue("");

        // تحديث مسافة الرحلة المفتوحة
        updateOpenTripDistance(sheetCar, dist);
      }

      // ============================
      // Idle (السيارة شغالة بدون حركة)
      // ============================
      if (status == "idle" && tripState == "in_trip") {
        updateOpenTripDistance(sheetCar, dist);
      }

      // ============================
      // توقف السيارة
      // ============================
      if (status == "stopped" && tripState == "in_trip") {

        if (!stopTime) {
          trackingSheet.getRange(i+1,9).setValue(time);
          return;
        }

        var diffStop = (time - new Date(stopTime)) / 1000 / 60;

        // توقف 5 دقائق = نهاية الرحلة
        if (diffStop >= 5) {
          closeTrip(sheetCar, stopTime);
          trackingSheet.getRange(i+1,7).setValue("no_trip");
          trackingSheet.getRange(i+1,8).setValue("");
          trackingSheet.getRange(i+1,9).setValue("");
        }
      }
    }
  }
}

// ========================================
// تحديث مسافة الرحلة المفتوحة
// ========================================
function updateOpenTripDistance(car, dist) {

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var tripSheet = ss.getSheetByName("trips_auto");
  var data = tripSheet.getDataRange().getValues();

  for (var i = data.length - 1; i >= 1; i--) {
    if (String(data[i][1]) == String(car) && data[i][6] == "open") {

      var current = Number(data[i][5] || 0);
      current += (dist / 1000);

      tripSheet.getRange(i + 1, 6).setValue(current.toFixed(2));
      break;
    }
  }
}

// ========================================
// إغلاق الرحلة
// ========================================
function closeTrip(car, endTime) {

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var tripSheet = ss.getSheetByName("trips_auto");
  var data = tripSheet.getDataRange().getValues();

  for (var i = data.length - 1; i >= 1; i--) {

    var sheetCar = String(data[i][1]);

    if (sheetCar == String(car) && data[i][6] == "open") {

      var startTime = new Date(data[i][2]);

      if (!startTime || !endTime) return;

      var durationMinutes = (endTime - startTime) / 1000 / 60;
      var durationHours = (durationMinutes / 60).toFixed(2);

      tripSheet.getRange(i + 1, 4).setValue(endTime);
      tripSheet.getRange(i + 1, 5).setValue(durationHours);
      tripSheet.getRange(i + 1, 7).setValue("closed");

      break;
    }
  }
}

// ========================================
// حساب مسافة الرحلة (احتياطي)
// ========================================
function calculateTripDistance(car, startTime, endTime) {

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("gps_history");
  var data = sheet.getDataRange().getValues();

  var points = [];

  for (var i = 1; i < data.length; i++) {

    var sheetCar = String(data[i][1]);

    if (sheetCar == String(car)) {
      var t = new Date(data[i][4]);

      if (t >= startTime && t <= endTime) {
        points.push({
          lat: Number(data[i][2]),
          lng: Number(data[i][3])
        });
      }
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