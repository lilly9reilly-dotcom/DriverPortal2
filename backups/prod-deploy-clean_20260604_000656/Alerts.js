function checkAlerts(driver, car, status, speed, time) {

  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ss.getSheetByName("alerts");
  if (!sheet) return;

  const cache = CacheService.getScriptCache();

  function alreadySent(key) {
    return cache.get(key);
  }

  function markSent(key) {
    cache.put(key, "1", 60); // 60 ثانية منع تكرار
  }

  const alertsToAdd = [];

  // Overspeed
  if (speed > 100) {
    const key = "overspeed_" + car;
    if (!alreadySent(key)) {
      alertsToAdd.push([new Date(), car, driver, "Overspeed", "سرعة عالية"]);
      markSent(key);
    }
  }

  // Long Stop (بعد 2 دقيقة مثلاً)
  if (status == "stopped") {
    const key = "stop_" + car;
    if (!alreadySent(key)) {
      alertsToAdd.push([new Date(), car, driver, "Long Stop", "توقف طويل"]);
      markSent(key);
    }
  }

  // Offline
  if (status == "offline") {
    const key = "offline_" + car;
    if (!alreadySent(key)) {
      alertsToAdd.push([new Date(), car, driver, "Offline", "الجهاز غير متصل"]);
      markSent(key);
    }
  }

  // كتابة دفعة واحدة
  if (alertsToAdd.length > 0) {
    sheet.getRange(sheet.getLastRow() + 1, 1, alertsToAdd.length, 5)
      .setValues(alertsToAdd);
  }
}
function getRecentAlerts() {
  const ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  const sheet = ss.getSheetByName("alerts");

  if (!sheet) return { success: true, alerts: [] };

  const lastRow = sheet.getLastRow();
  const start = Math.max(2, lastRow - 20);

  const data = sheet.getRange(start, 1, lastRow - start + 1, 5).getValues();

  const alerts = data.reverse().map(row => ({
    time: row[0] || "",
    car: row[1] || "",
    driver: row[2] || "",
    type: row[3] || "",
    message: row[4] || ""
  }));

  return { success: true, alerts: alerts };
}