/**
 * Client payout methods, settlements, and money notifications.
 * Additive sheets only — never wipes live trip/login/gps data.
 */

var CLIENT_PAYOUT_SHEET_ = "وسائل_استلام_العملاء";
var CLIENT_SETTLEMENT_SHEET_ = "تسويات_العملاء";
var CLIENT_MONEY_NOTICE_SHEET_ = "إشعارات_الأموال";

function ensureClientFinanceSheets_(ss) {
  ss = ss || getCompanySpreadsheet_();
  ensureNamedSheetWithHeaders_(ss, CLIENT_PAYOUT_SHEET_, [
    "الوقت", "كود_العميل", "اسم_العميل", "القاعدة",
    "زين_كاش", "ماستركارد", "بطاقة_مصرفية", "حساب_مصرفي", "اسم_المصرف", "ملاحظات"
  ]);
  ensureNamedSheetWithHeaders_(ss, CLIENT_SETTLEMENT_SHEET_, [
    "الوقت", "معرف", "كود_العميل", "اسم_العميل", "القاعدة",
    "المبلغ", "الوسيلة", "تفاصيل_الوسيلة", "حالة", "ملاحظة_الإرسال", "ملاحظة_الاستلام", "وقت_الاستلام"
  ]);
  ensureNamedSheetWithHeaders_(ss, CLIENT_MONEY_NOTICE_SHEET_, [
    "الوقت", "معرف", "كود_العميل", "اسم_العميل", "القاعدة",
    "الاتجاه", "العنوان", "النص", "المبلغ", "الوسيلة", "مقروء", "مرتبط_بتسوية"
  ]);
  return { success: true };
}

function clientSavePayoutMethods_(data) {
  data = data || {};
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var client = login.data;
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var sheet = ss.getSheetByName(CLIENT_PAYOUT_SHEET_);
  var values = sheet.getDataRange().getValues();
  var rowIndex = -1;
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][1] || "").toUpperCase() === String(client.loginCode || "").toUpperCase()) {
      rowIndex = i + 1;
      break;
    }
  }
  var row = [
    new Date(),
    client.loginCode,
    client.name,
    client.dbSheet,
    String(data.zainCash || ""),
    String(data.mastercard || ""),
    String(data.bankCard || ""),
    String(data.bankAccount || ""),
    String(data.bankName || ""),
    String(data.notes || "")
  ];
  if (rowIndex > 0) sheet.getRange(rowIndex, 1, 1, row.length).setValues([row]);
  else sheet.appendRow(row);

  appendAuditLog_({
    actor: client.name,
    role: "عميل",
    action: "savePayoutMethods",
    target: client.dbSheet,
    detail: "تحديث وسائل الاستلام"
  });
  return { success: true, message: "تم حفظ وسائل الاستلام", data: clientGetPayoutMethods_(data).data };
}

function clientGetPayoutMethods_(data) {
  data = data || {};
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var client = login.data;
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var sheet = ss.getSheetByName(CLIENT_PAYOUT_SHEET_);
  var values = sheet.getDataRange().getValues();
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][1] || "").toUpperCase() === String(client.loginCode || "").toUpperCase()) {
      return {
        success: true,
        data: {
          zainCash: String(values[i][4] || ""),
          mastercard: String(values[i][5] || ""),
          bankCard: String(values[i][6] || ""),
          bankAccount: String(values[i][7] || ""),
          bankName: String(values[i][8] || ""),
          notes: String(values[i][9] || "")
        }
      };
    }
  }
  return {
    success: true,
    data: { zainCash: "", mastercard: "", bankCard: "", bankAccount: "", bankName: "", notes: "" }
  };
}

function adminSendClientSettlement_(data) {
  data = data || {};
  var code = String(data.code || data.loginCode || "").trim().toUpperCase();
  var amount = MinistryCore.toNumber(data.amount);
  var method = String(data.method || "").trim();
  if (!code) return { success: false, message: "كود العميل مطلوب" };
  if (!(amount > 0)) return { success: false, message: "المبلغ غير صالح" };
  if (!method) return { success: false, message: "وسيلة التحويل مطلوبة" };

  var login = clientPortalLogin_({ code: code });
  if (!login.success) return login;
  var client = login.data;
  var payout = clientGetPayoutMethods_({ code: code }).data || {};
  var methodDetail = "";
  if (method.indexOf("زين") >= 0) methodDetail = payout.zainCash || "";
  else if (method.indexOf("ماستر") >= 0) methodDetail = payout.mastercard || "";
  else if (method.indexOf("بطاقة") >= 0) methodDetail = payout.bankCard || "";
  else if (method.indexOf("حساب") >= 0 || method.indexOf("مصرف") >= 0) {
    methodDetail = (payout.bankName ? payout.bankName + " / " : "") + (payout.bankAccount || "");
  }

  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var settlementId = "STL-" + Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd-HHmmss") + "-" + Math.floor(Math.random() * 900 + 100);
  ss.getSheetByName(CLIENT_SETTLEMENT_SHEET_).appendRow([
    new Date(),
    settlementId,
    client.loginCode,
    client.name,
    client.dbSheet,
    amount,
    method,
    methodDetail,
    "مرسل",
    String(data.note || "تحويل من الإدارة"),
    "",
    ""
  ]);

  var noticeId = "NTC-" + Utilities.getUuid().replace(/-/g, "").slice(0, 10);
  ss.getSheetByName(CLIENT_MONEY_NOTICE_SHEET_).appendRow([
    new Date(),
    noticeId,
    client.loginCode,
    client.name,
    client.dbSheet,
    "إلى_العميل",
    "تم إرسال مبلغ إليك",
    "تم تحويل " + amount + " د.ع عبر " + method + (methodDetail ? " (" + methodDetail + ")" : ""),
    amount,
    method,
    0,
    settlementId
  ]);

  // Admin-facing notice that money was sent
  ss.getSheetByName(CLIENT_MONEY_NOTICE_SHEET_).appendRow([
    new Date(),
    "NTC-" + Utilities.getUuid().replace(/-/g, "").slice(0, 10),
    client.loginCode,
    client.name,
    client.dbSheet,
    "إلى_الإدارة",
    "تم تسجيل إرسال للعميل",
    "أُرسل " + amount + " د.ع إلى " + client.name + " عبر " + method,
    amount,
    method,
    0,
    settlementId
  ]);

  appendAuditLog_({
    actor: String(data.actor || "admin"),
    role: "إدارة",
    action: "sendClientSettlement",
    target: client.dbSheet,
    detail: settlementId + " / " + amount + " / " + method
  });

  return {
    success: true,
    message: "تم إرسال التسوية وإنشاء الإشعار",
    data: { settlementId: settlementId, client: client.name, amount: amount, method: method, methodDetail: methodDetail }
  };
}

function clientConfirmSettlementReceived_(data) {
  data = data || {};
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var settlementId = String(data.settlementId || "").trim();
  if (!settlementId) return { success: false, message: "معرف التسوية مطلوب" };

  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var sheet = ss.getSheetByName(CLIENT_SETTLEMENT_SHEET_);
  var values = sheet.getDataRange().getValues();
  var found = -1;
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][1] || "") === settlementId &&
        String(values[i][2] || "").toUpperCase() === String(login.data.loginCode || "").toUpperCase()) {
      found = i + 1;
      break;
    }
  }
  if (found < 0) return { success: false, message: "التسوية غير موجودة لهذا العميل" };

  var amount = MinistryCore.toNumber(values[found - 1][5]);
  var method = String(values[found - 1][6] || "");
  sheet.getRange(found, 9).setValue("مستلم");
  sheet.getRange(found, 11).setValue(String(data.note || "أكد العميل الاستلام من التطبيق"));
  sheet.getRange(found, 12).setValue(new Date());

  // Client notice
  ss.getSheetByName(CLIENT_MONEY_NOTICE_SHEET_).appendRow([
    new Date(),
    "NTC-" + Utilities.getUuid().replace(/-/g, "").slice(0, 10),
    login.data.loginCode,
    login.data.name,
    login.data.dbSheet,
    "إلى_العميل",
    "تم تأكيد وصول المبلغ",
    "تم تسجيل استلام " + amount + " د.ع عبر " + method,
    amount,
    method,
    0,
    settlementId
  ]);
  // Admin notice
  ss.getSheetByName(CLIENT_MONEY_NOTICE_SHEET_).appendRow([
    new Date(),
    "NTC-" + Utilities.getUuid().replace(/-/g, "").slice(0, 10),
    login.data.loginCode,
    login.data.name,
    login.data.dbSheet,
    "إلى_الإدارة",
    "العميل أكد استلام المبلغ",
    login.data.name + " أكّد استلام " + amount + " د.ع عبر " + method,
    amount,
    method,
    0,
    settlementId
  ]);

  // Also log into client moves
  ensureClientMovesSheet_(ss).appendRow([
    new Date(),
    login.data.name,
    login.data.dbSheet,
    "استلام",
    method,
    amount,
    "تسوية " + settlementId
  ]);

  appendAuditLog_({
    actor: login.data.name,
    role: "عميل",
    action: "confirmSettlementReceived",
    target: settlementId,
    detail: amount + " / " + method
  });

  return { success: true, message: "تم تأكيد الاستلام وإشعار الإدارة" };
}

function clientListSettlements_(data) {
  data = data || {};
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var values = ss.getSheetByName(CLIENT_SETTLEMENT_SHEET_).getDataRange().getValues();
  var out = [];
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][2] || "").toUpperCase() !== String(login.data.loginCode || "").toUpperCase()) continue;
    out.push({
      time: values[i][0],
      settlementId: values[i][1],
      amount: values[i][5],
      method: values[i][6],
      methodDetail: values[i][7],
      status: values[i][8],
      sendNote: values[i][9],
      receiveNote: values[i][10],
      receivedAt: values[i][11]
    });
  }
  out.reverse();
  return { success: true, data: out };
}

function clientListMoneyNotices_(data) {
  data = data || {};
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var values = ss.getSheetByName(CLIENT_MONEY_NOTICE_SHEET_).getDataRange().getValues();
  var out = [];
  for (var i = 1; i < values.length; i++) {
    if (String(values[i][2] || "").toUpperCase() !== String(login.data.loginCode || "").toUpperCase()) continue;
    if (String(values[i][5] || "") !== "إلى_العميل") continue;
    out.push({
      time: values[i][0],
      id: values[i][1],
      title: values[i][6],
      text: values[i][7],
      amount: values[i][8],
      method: values[i][9],
      read: !!values[i][10],
      settlementId: values[i][11]
    });
  }
  out.reverse();
  return { success: true, data: out };
}

function adminListMoneyNotices_(data) {
  data = data || {};
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var values = ss.getSheetByName(CLIENT_MONEY_NOTICE_SHEET_).getDataRange().getValues();
  var out = [];
  var limit = Math.min(values.length - 1, Number(data.limit || 100));
  for (var i = values.length - 1; i >= 1 && out.length < limit; i--) {
    if (String(values[i][5] || "") !== "إلى_الإدارة") continue;
    out.push({
      time: values[i][0],
      id: values[i][1],
      clientCode: values[i][2],
      clientName: values[i][3],
      dbSheet: values[i][4],
      title: values[i][6],
      text: values[i][7],
      amount: values[i][8],
      method: values[i][9],
      read: !!values[i][10],
      settlementId: values[i][11]
    });
  }
  return { success: true, data: out };
}

function adminListClientSettlements_(data) {
  data = data || {};
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var values = ss.getSheetByName(CLIENT_SETTLEMENT_SHEET_).getDataRange().getValues();
  var codeFilter = String(data.code || "").trim().toUpperCase();
  var out = [];
  for (var i = values.length - 1; i >= 1; i--) {
    if (codeFilter && String(values[i][2] || "").toUpperCase() !== codeFilter) continue;
    out.push({
      time: values[i][0],
      settlementId: values[i][1],
      clientCode: values[i][2],
      clientName: values[i][3],
      dbSheet: values[i][4],
      amount: values[i][5],
      method: values[i][6],
      methodDetail: values[i][7],
      status: values[i][8],
      sendNote: values[i][9],
      receiveNote: values[i][10],
      receivedAt: values[i][11]
    });
    if (out.length >= Number(data.limit || 200)) break;
  }
  return { success: true, data: out };
}

function adminListClientPayoutMethods_(data) {
  data = data || {};
  var ss = getCompanySpreadsheet_();
  ensureClientFinanceSheets_(ss);
  var values = ss.getSheetByName(CLIENT_PAYOUT_SHEET_).getDataRange().getValues();
  var out = [];
  for (var i = 1; i < values.length; i++) {
    out.push({
      updatedAt: values[i][0],
      clientCode: values[i][1],
      clientName: values[i][2],
      dbSheet: values[i][3],
      zainCash: values[i][4],
      mastercard: values[i][5],
      bankCard: values[i][6],
      bankAccount: values[i][7],
      bankName: values[i][8],
      notes: values[i][9]
    });
  }
  return { success: true, data: out };
}


// Public wrappers for Admin.html google.script.run (no trailing underscore).
function ensureClientFinanceSheets(data) { return ensureClientFinanceSheets_(data); }
function clientSavePayoutMethods(data) { return clientSavePayoutMethods_(data); }
function clientGetPayoutMethods(data) { return clientGetPayoutMethods_(data); }
function adminSendClientSettlement(data) { return adminSendClientSettlement_(data); }
function clientConfirmSettlementReceived(data) { return clientConfirmSettlementReceived_(data); }
function clientListSettlements(data) { return clientListSettlements_(data); }
function clientListMoneyNotices(data) { return clientListMoneyNotices_(data); }
function adminListMoneyNotices(data) { return adminListMoneyNotices_(data); }
function adminListClientSettlements(data) { return adminListClientSettlements_(data); }
function adminListClientPayoutMethods(data) { return adminListClientPayoutMethods_(data); }
