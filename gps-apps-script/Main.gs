var GPS_SPREADSHEET_ID = '1a7r3rXY7dPyUjKCdvNopK2Y9ufKYhda0o6DCYBukv2o';

function doGet(e) {
  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function handleRequest(e) {
  var data = extractRequestData(e);
  var action = String(data.action || '').trim();

  if (!action) {
    action = hasGpsPayload_(data) ? 'gps' : 'health';
  }

  try {
    if (action === 'health') return json(getSystemHealth());
    if (action === 'gps') return json(handleGPS(data));
    if (action === 'drivers' || action === 'get_drivers') return json(getDriversLive());
    if (action === 'route') return json(getVehicleRoute(data));
    if (action === 'cleanup') return json(cleanupDuplicates());

    return json({
      success: false,
      message: 'Unknown GPS action: ' + action,
      available_actions: ['health', 'gps', 'drivers', 'get_drivers', 'route', 'cleanup']
    });
  } catch (err) {
    return json({ success: false, message: String(err), action: action });
  }
}

function extractRequestData(e) {
  var data = {};

  if (e && e.parameter) {
    data = e.parameter;
  }

  if (e && e.postData && e.postData.contents) {
    var content = String(e.postData.contents || '').trim();
    if (content) {
      try {
        if (content.charAt(0) === '{') {
          var parsed = JSON.parse(content);
          if (parsed && typeof parsed === 'object') data = parsed;
        }
      } catch (err) {
        Logger.log('JSON parse skipped: ' + err);
      }
    }
  }

  return data || {};
}

function hasGpsPayload_(data) {
  return !!(data && (data.driverName || data.driver) && (data.carNumber || data.car) && data.lat && data.lng);
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

function openGpsSpreadsheet_() {
  var id = String(GPS_SPREADSHEET_ID || '').trim();
  if (!id || id === 'REPLACE_WITH_GPS_SPREADSHEET_ID') {
    throw new Error('GPS_SPREADSHEET_ID is not configured');
  }

  return SpreadsheetApp.openById(id);
}

function normalizeCarValue(value) {
  return String(value || '').trim().toLowerCase();
}

function findRowByCar(sheet, car, carColumnIndex) {
  if (!sheet) return -1;

  var targetCar = normalizeCarValue(car);
  if (!targetCar) return -1;

  var values = sheet.getDataRange().getValues();
  for (var index = 1; index < values.length; index++) {
    var rowCar = normalizeCarValue(values[index][carColumnIndex - 1]);
    if (rowCar === targetCar) return index + 1;
  }

  return -1;
}

function toRadians(value) {
  return value * Math.PI / 180;
}

function distance(lat1, lng1, lat2, lng2) {
  var earthRadius = 6371000;
  var dLat = toRadians(lat2 - lat1);
  var dLng = toRadians(lng2 - lng1);
  var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadius * c;
}

function buildCompactRouteHistory(existingValue, lat, lng, status, speed, now) {
  var points = [];

  try {
    points = existingValue ? JSON.parse(existingValue) : [];
    if (!Array.isArray(points)) points = [];
  } catch (err) {
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
  var lastStatus = String(lastPoint.status || '').trim().toLowerCase();
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

  for (var index = data.length - 1; index > 0; index--) {
    var car = normalizeCarValue(data[index][carColumnIndex - 1]);
    if (!car) continue;

    if (seen[car]) {
      rowsToDelete.push(index + 1);
    } else {
      seen[car] = true;
    }
  }

  rowsToDelete.sort(function(a, b) { return b - a; });
  for (var item = 0; item < rowsToDelete.length; item++) {
    sheet.deleteRow(rowsToDelete[item]);
  }

  return rowsToDelete.length;
}

function ensureGpsSheets_(ss) {
  var trackingSheet = ss.getSheetByName('gps_tracking');
  if (!trackingSheet) {
    trackingSheet = ss.insertSheet('gps_tracking');
    trackingSheet.appendRow(['اسم السائق', 'رقم السيارة', 'lat', 'lng', 'الوقت', 'الحالة', 'trip_status', 'moving_time', 'stop_time', 'last_update', 'speed']);
  }

  var historySheet = ss.getSheetByName('gps_history');
  if (!historySheet) {
    historySheet = ss.insertSheet('gps_history');
    historySheet.appendRow(['driver', 'carNumber', 'lat', 'lng', 'time', 'status', 'speed', 'route_points_json']);
  } else if (historySheet.getLastColumn() < 8) {
    historySheet.getRange(1, 8).setValue('route_points_json');
  }

  return { trackingSheet: trackingSheet, historySheet: historySheet };
}

function handleGPS(data) {
  if (!data) return { success: false, message: 'No data' };

  var ss = openGpsSpreadsheet_();
  var sheets = ensureGpsSheets_(ss);
  var trackingSheet = sheets.trackingSheet;
  var historySheet = sheets.historySheet;

  var driver = String(data.driverName || data.driver || '').trim();
  var car = String(data.carNumber || data.car || '').trim();
  var lat = Number(data.lat || 0);
  var lng = Number(data.lng || 0);
  var status = String(data.status || 'stopped').trim().toLowerCase();
  var speed = Number(data.speed || 0);
  var now = new Date();

  if (!driver || !car || !lat || !lng) {
    return { success: false, message: 'Missing: driverName, carNumber, lat, lng' };
  }

  var foundRow = findRowByCar(trackingSheet, car, 2);

  if (foundRow > -1) {
    var currentTrip = String(trackingSheet.getRange(foundRow, 7).getValue() || 'no_trip');
    trackingSheet.getRange(foundRow, 1, 1, 11).setValues([[driver, car, lat, lng, now, status, currentTrip, trackingSheet.getRange(foundRow, 8).getValue() || '', trackingSheet.getRange(foundRow, 9).getValue() || '', now, speed]]);
  } else {
    trackingSheet.appendRow([driver, car, lat, lng, now, status, 'no_trip', '', '', now, speed]);
  }

  var historyRow = findRowByCar(historySheet, car, 2);
  var existingRouteJson = historyRow > -1 ? String(historySheet.getRange(historyRow, 8).getValue() || '') : '';
  var compactRouteJson = buildCompactRouteHistory(existingRouteJson, lat, lng, status, speed, now);

  if (historyRow > -1) {
    historySheet.getRange(historyRow, 1, 1, 8).setValues([[driver, car, lat, lng, now, status, speed, compactRouteJson]]);
  } else {
    historySheet.appendRow([driver, car, lat, lng, now, status, speed, compactRouteJson]);
  }

  return { success: true, message: 'GPS saved', driver: driver, carNumber: car, lat: lat, lng: lng, status: status, speed: speed, timestamp: now };
}

function getDriversLive() {
  var ss = openGpsSpreadsheet_();
  var trackingSheet = ss.getSheetByName('gps_tracking');

  if (!trackingSheet) return { success: true, count: 0, drivers: [] };

  var data = trackingSheet.getDataRange().getValues();
  var drivers = [];

  for (var index = 1; index < data.length; index++) {
    if (!data[index][1]) continue;
    drivers.push({
      driver: String(data[index][0] || ''),
      carNumber: String(data[index][1] || ''),
      lat: Number(data[index][2] || 0),
      lng: Number(data[index][3] || 0),
      time: data[index][4],
      status: String(data[index][5] || 'unknown'),
      trip_status: String(data[index][6] || 'no_trip'),
      moving_time: data[index][7] || '',
      stop_time: data[index][8] || '',
      last_update: data[index][9],
      speed: Number(data[index][10] || 0)
    });
  }

  return { success: true, count: drivers.length, drivers: drivers };
}

function getVehicleRoute(params) {
  var vehicleId = params.vehicle || params.carNumber || params.id || params.driverName || '';
  var limit = params.limit || 160;
  return getVehicleRouteData(vehicleId, limit);
}

function getVehicleRouteData(vehicleId, limit) {
  var normalizedVehicle = normalizeCarValue(vehicleId);
  var safeLimit = Math.max(10, Math.min(Number(limit) || 160, 500));

  if (!normalizedVehicle) return { success: false, message: 'Vehicle is required', points: [] };

  var ss = openGpsSpreadsheet_();
  var historySheet = ss.getSheetByName('gps_history');
  var trackingSheet = ss.getSheetByName('gps_tracking');
  var points = [];

  if (historySheet) {
    var rows = historySheet.getDataRange().getValues();
    for (var index = 1; index < rows.length; index++) {
      var rowCar = normalizeCarValue(rows[index][1]);
      if (rowCar !== normalizedVehicle) continue;

      var rawRouteJson = String(rows[index][7] || '').trim();
      if (rawRouteJson) {
        try {
          var parsedPoints = JSON.parse(rawRouteJson);
          if (Array.isArray(parsedPoints) && parsedPoints.length > 0) {
            points = parsedPoints;
            break;
          }
        } catch (err) {}
      }

      var lat = Number(rows[index][2] || 0);
      var lng = Number(rows[index][3] || 0);
      if (!lat || !lng) continue;

      var pointTime = new Date(rows[index][4] || new Date());
      points.push({ lat: lat, lng: lng, timestamp: pointTime.getTime(), time: pointTime, status: String(rows[index][5] || 'unknown'), speed: Number(rows[index][6] || 0) });
      break;
    }
  }

  if (points.length === 0 && trackingSheet) {
    var trackingRows = trackingSheet.getDataRange().getValues();
    for (var item = 1; item < trackingRows.length; item++) {
      var trackingCar = normalizeCarValue(trackingRows[item][1]);
      if (trackingCar !== normalizedVehicle) continue;

      var currentLat = Number(trackingRows[item][2] || 0);
      var currentLng = Number(trackingRows[item][3] || 0);
      if (!currentLat || !currentLng) continue;

      var trackingTime = new Date(trackingRows[item][4] || new Date());
      points.push({ lat: currentLat, lng: currentLng, timestamp: trackingTime.getTime(), time: trackingTime, status: String(trackingRows[item][5] || 'unknown'), speed: Number(trackingRows[item][10] || 0) });
      break;
    }
  }

  points.sort(function(a, b) { return a.timestamp - b.timestamp; });
  return { success: true, vehicle: vehicleId, points: points.slice(-safeLimit) };
}

function cleanupDuplicates() {
  var ss = openGpsSpreadsheet_();
  var trackingSheet = ss.getSheetByName('gps_tracking');
  var historySheet = ss.getSheetByName('gps_history');

  if (!trackingSheet || !historySheet) return { success: false, message: 'Sheets not found' };

  var trackingRemoved = removeDuplicateRowsByCar(trackingSheet, 2);
  var historyRemoved = removeDuplicateRowsByCar(historySheet, 2);

  return {
    success: true,
    message: 'Cleanup completed',
    tracking_duplicates_removed: trackingRemoved,
    history_duplicates_removed: historyRemoved,
    tracking_rows_after_cleanup: Math.max(trackingSheet.getLastRow() - 1, 0),
    history_rows_after_cleanup: Math.max(historySheet.getLastRow() - 1, 0),
    timestamp: new Date()
  };
}

function getSystemHealth() {
  var ss = openGpsSpreadsheet_();
  var trackingSheet = ss.getSheetByName('gps_tracking');
  var historySheet = ss.getSheetByName('gps_history');

  return {
    success: true,
    status: 'healthy',
    spreadsheetName: ss.getName(),
    tracking_rows: trackingSheet ? Math.max(trackingSheet.getLastRow() - 1, 0) : 0,
    history_rows: historySheet ? Math.max(historySheet.getLastRow() - 1, 0) : 0,
    timestamp: new Date()
  };
}
