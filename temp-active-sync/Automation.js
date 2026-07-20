/*
 * Report Automation (safe, standalone)
 * Purpose: one-click issuance of monthly driver statement files (PDF + XLSX)
 * This file does not modify tracking APIs (doGet/doPost) and does not touch existing data rows.
 */

var REPORTS_MENU_TITLE = "Report Issuance";
var REPORTS_ROOT_FOLDER_NAME = "Fleet_Official_Reports";
var REPORTS_LOG_SHEET_NAME = "report_issuance_log";

/**
 * Run once manually from Apps Script editor.
 * Installs an onOpen trigger that shows a custom menu in the spreadsheet UI.
 */
function installReportAutomation() {
  var ss = SpreadsheetApp.getActive();
  var projectTriggers = ScriptApp.getProjectTriggers();

  for (var i = 0; i < projectTriggers.length; i++) {
    var t = projectTriggers[i];
    if (t.getHandlerFunction && t.getHandlerFunction() === "showReportAutomationMenu") {
      if (t.getTriggerSourceId && t.getTriggerSourceId() === ss.getId()) {
        showReportAutomationMenu();
        SpreadsheetApp.getUi().alert("Report automation already installed.");
        return;
      }
    }
  }

  ScriptApp.newTrigger("showReportAutomationMenu").forSpreadsheet(ss).onOpen().create();
  showReportAutomationMenu();
  SpreadsheetApp.getUi().alert("Report automation installed successfully.");
}

/**
 * Called by onOpen trigger created by installReportAutomation.
 */
function showReportAutomationMenu() {
  SpreadsheetApp.getUi()
    .createMenu(REPORTS_MENU_TITLE)
    .addItem("Issue report for active sheet", "issueCurrentMonthlyReport")
    .addItem("Issue report for 2026_05", "issueReport_2026_05")
    .addSeparator()
    .addItem("Open reports folder", "openReportsFolder")
    .addItem("Open issuance log", "openReportIssuanceLog")
    .addToUi();
}

function issueReport_2026_05() {
  issueReportForSheetName("2026_05");
}

/**
 * One-click issuance for active sheet.
 */
function issueCurrentMonthlyReport() {
  var sheet = SpreadsheetApp.getActiveSheet();
  issueReportForSheet_(sheet);
}

/**
 * One-click issuance for a specific sheet name.
 */
function issueReportForSheetName(sheetName) {
  var ss = SpreadsheetApp.getActive();
  var sheet = ss.getSheetByName(String(sheetName || "").trim());
  if (!sheet) {
    SpreadsheetApp.getUi().alert("Sheet not found: " + sheetName);
    return;
  }
  issueReportForSheet_(sheet);
}

function issueReportForSheet_(sheet) {
  var ss = sheet.getParent();
  var sheetName = sheet.getName();
  var sheetId = sheet.getSheetId();

  var folder = getOrCreateReportsFolder_(ss);
  var issuedAt = new Date();
  var timestampText = Utilities.formatDate(issuedAt, Session.getScriptTimeZone(), "yyyy-MM-dd_HH-mm-ss");
  var baseName = buildReportFileBaseName_(sheetName, timestampText);

  var pdfFile = exportSheetAsPdf_(ss, sheet, folder, baseName + ".pdf");
  var xlsxFile = exportSingleSheetAsXlsx_(sheet, folder, baseName + ".xlsx");

  var actor = Session.getActiveUser().getEmail() || "unknown_user";
  var logSheet = getOrCreateReportLogSheet_(ss);
  logSheet.appendRow([
    issuedAt,
    actor,
    sheetName,
    sheetId,
    pdfFile ? pdfFile.getName() : "",
    pdfFile ? pdfFile.getUrl() : "",
    xlsxFile ? xlsxFile.getName() : "",
    xlsxFile ? xlsxFile.getUrl() : ""
  ]);

  SpreadsheetApp.getUi().alert(
    "Report issued successfully.\n\nPDF: " + (pdfFile ? pdfFile.getName() : "failed") +
    "\nXLSX: " + (xlsxFile ? xlsxFile.getName() : "failed") +
    "\n\nSaved in folder: " + folder.getName()
  );
}

function openReportsFolder() {
  var folder = getOrCreateReportsFolder_(SpreadsheetApp.getActive());
  var html = HtmlService.createHtmlOutput(
    '<script>window.open("' + folder.getUrl() + '","_blank");google.script.host.close();</script>'
  ).setWidth(10).setHeight(10);
  SpreadsheetApp.getUi().showModalDialog(html, "Open folder");
}

function openReportIssuanceLog() {
  var ss = SpreadsheetApp.getActive();
  var logSheet = getOrCreateReportLogSheet_(ss);
  ss.setActiveSheet(logSheet);
}

function getOrCreateReportsFolder_(ss) {
  var rootFolders = DriveApp.getFoldersByName(REPORTS_ROOT_FOLDER_NAME);
  var root = rootFolders.hasNext() ? rootFolders.next() : DriveApp.createFolder(REPORTS_ROOT_FOLDER_NAME);

  var bookFolderName = "Book_" + ss.getId();
  var children = root.getFoldersByName(bookFolderName);
  return children.hasNext() ? children.next() : root.createFolder(bookFolderName);
}

function getOrCreateReportLogSheet_(ss) {
  var sheet = ss.getSheetByName(REPORTS_LOG_SHEET_NAME);
  if (sheet) return sheet;

  sheet = ss.insertSheet(REPORTS_LOG_SHEET_NAME);
  sheet.appendRow([
    "issued_at",
    "issued_by",
    "source_sheet",
    "source_gid",
    "pdf_name",
    "pdf_url",
    "xlsx_name",
    "xlsx_url"
  ]);
  sheet.setFrozenRows(1);
  return sheet;
}

function buildReportFileBaseName_(sheetName, timestampText) {
  var safeSheet = String(sheetName || "sheet").replace(/[\\/:*?"<>|]+/g, "_");
  return "Driver_Statement_" + safeSheet + "_" + timestampText;
}

function exportSheetAsPdf_(ss, sheet, folder, fileName) {
  var ssId = ss.getId();
  var gid = sheet.getSheetId();
  var exportUrl =
    "https://docs.google.com/spreadsheets/d/" + ssId +
    "/export?format=pdf" +
    "&gid=" + gid +
    "&size=A4" +
    "&portrait=false" +
    "&fitw=true" +
    "&sheetnames=false" +
    "&printtitle=false" +
    "&pagenumbers=false" +
    "&gridlines=false" +
    "&fzr=false" +
    "&horizontal_alignment=CENTER" +
    "&vertical_alignment=TOP";

  var response = UrlFetchApp.fetch(exportUrl, {
    method: "get",
    headers: {
      Authorization: "Bearer " + ScriptApp.getOAuthToken()
    },
    muteHttpExceptions: true
  });

  if (response.getResponseCode() >= 300) {
    throw new Error("PDF export failed: HTTP " + response.getResponseCode());
  }

  var blob = response.getBlob().setName(fileName);
  return folder.createFile(blob);
}

function exportSingleSheetAsXlsx_(sourceSheet, folder, fileName) {
  var tempSS = SpreadsheetApp.create("tmp_export_" + new Date().getTime());
  var tempId = tempSS.getId();

  try {
    var copied = sourceSheet.copyTo(tempSS);
    copied.setName(sourceSheet.getName());

    var all = tempSS.getSheets();
    for (var i = 0; i < all.length; i++) {
      if (all[i].getSheetId() !== copied.getSheetId()) {
        tempSS.deleteSheet(all[i]);
      }
    }

    var exportUrl = "https://docs.google.com/spreadsheets/d/" + tempId + "/export?format=xlsx";
    var response = UrlFetchApp.fetch(exportUrl, {
      method: "get",
      headers: {
        Authorization: "Bearer " + ScriptApp.getOAuthToken()
      },
      muteHttpExceptions: true
    });

    if (response.getResponseCode() >= 300) {
      throw new Error("XLSX export failed: HTTP " + response.getResponseCode());
    }

    var blob = response.getBlob().setName(fileName);
    return folder.createFile(blob);
  } finally {
    DriveApp.getFileById(tempId).setTrashed(true);
  }
}
