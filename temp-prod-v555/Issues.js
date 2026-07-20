// ========================================
// Issues Engine
// ========================================

function saveIssue(data) {
  var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = getOrCreateIssuesSheet_(ss);
  var time  = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

  sheet.appendRow([
    time,
    String(data.driverName || "").trim(),
    String(data.docNumber  || "").trim(),
    String(data.issueType  || "").trim(),
    String(data.note       || "").trim(),
    "open"
  ]);

  return { success: true };
}

function reportIssue(data) {
  try {
    if (!data.driverName || !data.docNumber || !data.issueType) {
      return { success: false, message: "Missing data" };
    }

    var ss    = SpreadsheetApp.openById(SPREADSHEET_ID);
    var sheet = getOrCreateIssuesSheet_(ss);
    var time  = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

    sheet.appendRow([
      time,
      String(data.driverName || "").trim(),
      String(data.docNumber  || "").trim(),
      String(data.issueType  || "").trim(),
      String(data.note       || "").trim(),
      "open"
    ]);

    updateTripStatus_(ss, data.docNumber);

    return {
      success:   true,
      message:   "تم الإبلاغ عن المشكلة بنجاح",
      docNumber: data.docNumber,
      issueType: data.issueType
    };

  } catch (err) {
    return { success: false, message: err.toString() };
  }
}

function updateTripStatus_(ss, docNumber) {
  var sheets = ss.getSheets();

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var name  = sheet.getName();

    if (!/^\d{4}_\d{2}$/.test(name)) continue;

    var values = sheet.getDataRange().getValues();

    for (var i = 1; i < values.length; i++) {
      if (String(values[i][0]).trim() === String(docNumber).trim()) {
        var currentNotes = String(values[i][14] || "");

        if (currentNotes.indexOf("REPORTED") === -1) {
          sheet.getRange(i + 1, 15).setValue(
            currentNotes ? currentNotes + " | REPORTED" : "REPORTED"
          );
        }
        return true;
      }
    }
  }
  return false;
}

function getOrCreateIssuesSheet_(ss) {
  var sheet = ss.getSheetByName("issues");
if (!sheet) {
  sheet = ss.insertSheet("issues");
  sheet.appendRow(["وقت التبليغ","اسم السائق","رقم الوصل","نوع المشكلة","ملاحظة","الحالة"]);
}

  return sheet;
}