var SPREADSHEET_ID = "1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM";


// ========================================
// getDriversLive
// ========================================
function getDriversLive() {
  var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("gps_tracking");

  if (!sheet) return { success: true, drivers: [] };

  var data = sheet.getDataRange().getValues();
  if (data.length < 2) return { success: true, drivers: [] };

  var drivers = [];
  var now     = new Date();

  for (var i = 1; i < data.length; i++) {
    var row = data[i];

    var driver     = String(row[0]  || "").trim();
    var carNumber  = String(row[1]  || "").trim();
    var lat        = Number(row[2]  || 0);
    var lng        = Number(row[3]  || 0);
    var time       = row[4]         || "";
    var status     = String(row[5]  || "offline").trim().toLowerCase();
    var tripStatus = String(row[6]  || "no_trip").trim();
    var speed      = Number(row[10] || 0);

    // ===== الحقول الإضافية الاختيارية للداشبورد (لا تؤثر على النظام القديم) =====
    // عدّل أرقام الأعمدة 11..16 حسب ترتيبك الفعلي في شيت gps_tracking
    var cargoType       = row[11] || ""; // نوع الحمولة
    var fromStation     = row[12] || ""; // محطة التحميل
    var toStation       = row[13] || ""; // محطة التفريغ
    var segment         = row[14] || ""; // loading / en_route_full / unloading / back_empty
    var eta             = row[15] || ""; // وقت الوصول المتوقع
    var distanceRem     = row[16] || ""; // المسافة المتبقية كم (اختياري)

    if (!driver || !carNumber || !lat || !lng) continue;

    try {
      var diff = (now - new Date(time)) / 1000 / 60;
      if (diff > 10) status = "offline";
    } catch (e) {}

    drivers.push({
      driver:     driver,
      carNumber:  carNumber,
      lat:        lat,
      lng:        lng,
      time:       time,
      status:     status,
      tripStatus: tripStatus,
      speed:      speed,

      // الحقول الجديدة (dashboard فقط)
      cargoType:         cargoType,
      fromStation:       fromStation,
      toStation:         toStation,
      segment:           segment,
      eta:               eta,
      distanceRemaining: distanceRem
    });
  }

  return { success: true, drivers: drivers };
}


// ========================================
// getVehicleRoute
// ========================================
function getVehicleRoute(params) {
  var carNumber  = "";
  var driverName = "";

  if (typeof params === "string") {
    carNumber = String(params).trim();
  } else if (params) {
    carNumber  = String(params.carNumber  || params.vehicle || "").trim();
    driverName = String(params.driverName || "").trim();
  }

  if (!carNumber && !driverName) {
    return { success: false, message: "vehicle is required", points: [] };
  }

  var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("gps_history");

  if (!sheet) return { success: true, points: [] };

  var data = sheet.getDataRange().getValues();
  if (data.length < 2) return { success: true, points: [] };

  var points = [];
  var start = 1;

  for (var i = start; i < data.length; i++) {
    var rowDriver = String(data[i][0] || "").trim();
    var rowCar    = String(data[i][1] || "").trim();

    if (carNumber) {
      if (rowCar !== carNumber) continue;
    } else {
      if (rowDriver !== driverName) continue;
    }

    var lat = parseFloat(String(data[i][2] || "0").replace(/٫/g, ".").replace(/,/g, "."));
    var lng = parseFloat(String(data[i][3] || "0").replace(/٫/g, ".").replace(/,/g, "."));

    if (!lat || !lng) continue;

    points.push({
      driver:    rowDriver,
      carNumber: rowCar,
      lat:       lat,
      lng:       lng,
      time:      data[i][4] || "",
      status:    String(data[i][5] || ""),
      speed:     Number(data[i][6] || 0)
    });
  }

  return { success: true, points: points };
}


// ========================================
// getRecentAlerts
// ========================================
function getRecentAlerts() {
  var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("alerts");

  if (!sheet) return { success: true, alerts: [] };

  var data = sheet.getDataRange().getValues();
  if (data.length < 2) return { success: true, alerts: [] };

  var alerts = [];

  for (var i = data.length - 1; i >= 1; i--) {
    alerts.push({
      time:      data[i][0] || "",
      driver:    data[i][1] || "",
      carNumber: data[i][2] || "",
      type:      data[i][3] || "",
      title:     data[i][3] || "تنبيه",
      message:   data[i][4] || ""
    });

    if (alerts.length >= 20) break;
  }

  return { success: true, alerts: alerts };
}


// ========================================
// getAutoTrips
// ========================================
function getAutoTrips() {
  var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("trips_auto");

  if (!sheet) return { success: true, trips: [] };

  var data = sheet.getDataRange().getValues();
  if (data.length < 2) return { success: true, trips: [] };

  var trips = [];

  for (var i = data.length - 1; i >= 1; i--) {
    trips.push({
      driverName: data[i][0] || "",
      carNumber:  data[i][1] || "",
      startTime:  data[i][2] || "",
      endTime:    data[i][3] || "",
      duration:   data[i][4] || "",
      distance:   data[i][5] || "",
      status:     data[i][6] || ""
    });

    if (trips.length >= 100) break;
  }

  return { success: true, trips: trips };
}