/**
 * Client portal API — read-first, agent-scoped.
 * Does not modify driver login / GPS / trip save paths.
 */

function clientPortalLogin_(data) {
  data = data || {};
  var code = String(data.code || data.loginCode || "").trim().toUpperCase();
  if (!code) return { success: false, message: "كود الدخول مطلوب" };

  var ss = getCompanySpreadsheet_();
  var agents = readAgents_(ss) || [];
  var match = null;
  for (var i = 0; i < agents.length; i++) {
    var a = agents[i] || {};
    var agentCode = String(a.loginCode || a.code || a.clientCode || "").trim().toUpperCase();
    if (!agentCode && a.cars && a.cars.length) {
      agentCode = ("C" + String(a.cars[0]).replace(/\D/g, "").slice(0, 4)).toUpperCase();
    }
    if (agentCode && agentCode === code) {
      match = a;
      break;
    }
  }

  // Fallback to official seed (offline-compatible mapping).
  if (!match) {
    var seed = {};
  try { seed = getOfficialAgentSeed({}) || {}; } catch (e1) { seed = {}; }
    var list = seed.agents || seed.data || [];
    for (var s = 0; s < list.length; s++) {
      var item = list[s] || {};
      var cars = item.cars || [];
      var seedCode = String(item.loginCode || "").trim().toUpperCase();
      if (!seedCode && cars.length) seedCode = ("C" + String(cars[0]).replace(/\D/g, "").slice(0, 4)).toUpperCase();
      if (seedCode === code) {
        match = {
          name: item.name,
          kind: item.kind || item.ownerKind || "معتمد",
          dbSheet: item.dbSheet || MinistryCore.agentDatabaseSheetName(item.name, item.kind || "معتمد"),
          cars: cars,
          loginCode: seedCode
        };
        break;
      }
    }
  }

  if (!match) return { success: false, message: "كود الدخول غير صحيح" };

  var dbSheet = match.dbSheet || MinistryCore.agentDatabaseSheetName(match.name, match.kind);
  return {
    success: true,
    data: {
      id: dbSheet,
      name: match.name,
      kind: match.kind || "معتمد",
      dbSheet: dbSheet,
      cars: match.cars || [],
      loginCode: code
    }
  };
}

function clientPortalGetState_(data) {
  data = data || {};
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var client = login.data;
  var ss = getCompanySpreadsheet_();
  var sheet = ss.getSheetByName(client.dbSheet);
  var receipts = [];
  if (sheet && sheet.getLastRow() > 1) {
    var values = sheet.getDataRange().getValues();
    var headers = values[0] || [];
    for (var r = 1; r < values.length; r++) {
      var row = values[r];
      if (!row || !row.join("")) continue;
      var obj = clientPortalRowToReceipt_(headers, row);
      if (obj) receipts.push(obj);
    }
  }

  appendAuditLog_({
    actor: client.name,
    role: "عميل",
    action: "clientGetState",
    target: client.dbSheet,
    detail: "قراءة " + receipts.length + " وصل"
  });

  return {
    success: true,
    data: {
      dbSheet: client.dbSheet,
      count: receipts.length,
      receipts: receipts
    }
  };
}

function clientPortalRowToReceipt_(headers, row) {
  function col() {
    for (var i = 0; i < arguments.length; i++) {
      var key = MinistryCore.normalizeHeaderToken(arguments[i]);
      for (var h = 0; h < headers.length; h++) {
        if (MinistryCore.normalizeHeaderToken(headers[h]) === key) return row[h];
      }
    }
    return "";
  }
  var docNumber = String(col("رقم الوصل", "الوصل", "docNumber") || "").trim();
  if (!docNumber) return null;
  var qty = MinistryCore.normalizeQtyTon(col("الكمية", "quantity"));
  var amount = MinistryCore.toNumber(col("المبلغ", "amount"));
  var movement = String(col("النوع", "الحركة", "movement") || "محطة");
  return {
    docNumber: docNumber,
    driverName: String(col("السائق", "driverName") || ""),
    carNumber: MinistryCore.normalizeCarNumber(col("رقم السيارة", "السيارة", "carNumber")),
    loadDate: String(col("تاريخ التحميل", "loadDate") || ""),
    unloadDate: String(col("تاريخ التفريغ", "unloadDate") || ""),
    quantity: qty,
    destination: String(col("الوجهة", "destination") || ""),
    movement: movement,
    month: String(col("الشهر", "month") || ""),
    period: String(col("الفترة", "period") || ""),
    rate: MinistryCore.toNumber(col("السعر", "rate")),
    amount: amount,
    gasLiters: MinistryCore.toNumber(col("لترات الكاز", "gasLiters")),
    gasValue: MinistryCore.toNumber(col("قيمة الكاز", "gasValue")),
    agent: String(col("المعتمد", "agent") || ""),
    status: String(col("الحالة", "status") || "")
  };
}

function clientPortalRegisterReceive_(data) {
  data = data || {};
  // Default: refuse live write unless explicitly enabled by admin flag sheet/cell later.
  if (!data.forceWrite) {
    return {
      success: false,
      message: "تسجيل الاستلام على الحي مغلق حالياً (معاينة فقط). فعّل forceWrite بعد اعتماد الإدارة."
    };
  }
  var login = clientPortalLogin_(data);
  if (!login.success) return login;
  var amount = MinistryCore.toNumber(data.amount);
  if (!(amount > 0)) return { success: false, message: "مبلغ غير صالح" };
  var ss = getCompanySpreadsheet_();
  var sheet = ensureClientMovesSheet_(ss);
  sheet.appendRow([
    new Date(),
    login.data.name,
    login.data.dbSheet,
    "استلام",
    String(data.method || ""),
    amount,
    String(data.note || "من بوابة العميل")
  ]);
  appendAuditLog_({
    actor: login.data.name,
    role: "عميل",
    action: "clientRegisterReceive",
    target: login.data.dbSheet,
    detail: amount + " / " + String(data.method || "")
  });
  return { success: true, message: "تم تسجيل الاستلام" };
}

function ensureClientMovesSheet_(ss) {
  var name = "حركات_العملاء";
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.appendRow(["الوقت", "العميل", "القاعدة", "النوع", "الوسيلة", "المبلغ", "ملاحظة"]);
    sheet.setFrozenRows(1);
  }
  return sheet;
}
