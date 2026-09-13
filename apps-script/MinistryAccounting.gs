/**
 * Ministry accounting layer: Agents/Fleet registry, period stats,
 * 10-receipt statements, and ministry pack export.
 * Does not replace TPL_/STMT_ or modify template sheet 60.
 */

var AGENTS_SHEET_NAME = "Agents";
var FLEET_SHEET_NAME = "Fleet";
var TEMPLATE_60_SHEET_NAME = "60";

function ensureAgentsSheet_(ss) {
  ss = ss || SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName(AGENTS_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(AGENTS_SHEET_NAME);
    sheet.appendRow(["الاسم", "النوع", "فعال", "ملاحظات"]);
    sheet.appendRow(["شركة", "شركة", "1", "جهة سيارات الشركة"]);
  } else if (sheet.getLastRow() === 0) {
    sheet.appendRow(["الاسم", "النوع", "فعال", "ملاحظات"]);
  }
  return sheet;
}

function ensureFleetSheet_(ss) {
  ss = ss || SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName(FLEET_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(FLEET_SHEET_NAME);
    sheet.appendRow(["رقم السيارة", "السائق الافتراضي", "تابع لـ", "اسم المعتمد", "فعال", "ملاحظات"]);
  } else if (sheet.getLastRow() === 0) {
    sheet.appendRow(["رقم السيارة", "السائق الافتراضي", "تابع لـ", "اسم المعتمد", "فعال", "ملاحظات"]);
  }
  return sheet;
}

function readAgents_(ss) {
  var sheet = ensureAgentsSheet_(ss);
  var values = sheet.getDataRange().getValues();
  var out = [];
  for (var i = 1; i < values.length; i++) {
    var name = String(values[i][0] || "").trim();
    if (!name) continue;
    var kind = MinistryCore.normalizeOwnerKind(values[i][1]) || MinistryCore.AGENT;
    out.push({
      row: i + 1,
      name: name,
      kind: kind,
      active: MinistryCore.isTruthyActive(values[i][2]),
      notes: String(values[i][3] || ""),
      dbSheet: MinistryCore.agentDatabaseSheetName(name, kind)
    });
  }
  return out;
}

function readFleet_(ss) {
  var sheet = ensureFleetSheet_(ss);
  var values = sheet.getDataRange().getValues();
  var out = [];
  for (var i = 1; i < values.length; i++) {
    var carNumber = String(values[i][0] || "").trim();
    if (!carNumber) continue;
    var ownerKind = MinistryCore.normalizeOwnerKind(values[i][2]) || MinistryCore.AGENT;
    var agentName = String(values[i][3] || "").trim();
    if (ownerKind === MinistryCore.COMPANY && !agentName) agentName = MinistryCore.COMPANY;
    out.push({
      row: i + 1,
      carNumber: carNumber,
      carNumberNormalized: MinistryCore.normalizeCarNumber(carNumber),
      defaultDriver: String(values[i][1] || "").trim(),
      ownerKind: ownerKind,
      agentName: agentName,
      active: MinistryCore.isTruthyActive(values[i][4]),
      notes: String(values[i][5] || "")
    });
  }
  return out;
}

function listAgents(data) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  return { success: true, data: readAgents_(ss) };
}

function listFleet(data) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  return { success: true, data: readFleet_(ss) };
}

function getMinistryRegistry(data) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var agents = readAgents_(ss);
  return {
    success: true,
    agents: attachAgentDbCounts_(ss, agents),
    fleet: readFleet_(ss),
    unclassifiedDb: MinistryCore.UNCLASSIFIED_DB,
    companyDb: MinistryCore.COMPANY_DB
  };
}

function saveAgent(data) {
  data = data || {};
  var name = String(data.name || "").trim();
  if (!name) return { success: false, message: "اسم المعتمد مطلوب" };

  var kind = MinistryCore.normalizeOwnerKind(data.kind) || MinistryCore.AGENT;
  var active = data.active == null ? true : MinistryCore.isTruthyActive(data.active);
  var notes = String(data.notes || "");
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ensureAgentsSheet_(ss);
  var agents = readAgents_(ss);
  var targetRow = Number(data.row || 0);

  if (!targetRow) {
    for (var i = 0; i < agents.length; i++) {
      if (MinistryCore.normalizeText(agents[i].name) === MinistryCore.normalizeText(name)) {
        targetRow = agents[i].row;
        break;
      }
    }
  }

  if (targetRow > 1) {
    sheet.getRange(targetRow, 1, 1, 4).setValues([[name, kind, active ? "1" : "0", notes]]);
  } else {
    sheet.appendRow([name, kind, active ? "1" : "0", notes]);
  }

  var dbSheet = ensureAgentDatabaseSheet_(ss, name, kind);
  return {
    success: true,
    data: readAgents_(ss),
    dbSheet: dbSheet.getName()
  };
}

function saveFleet(data) {
  data = data || {};
  var carNumber = String(data.carNumber || "").trim();
  if (!carNumber) return { success: false, message: "رقم السيارة مطلوب" };

  var ownerKind = MinistryCore.normalizeOwnerKind(data.ownerKind) || MinistryCore.AGENT;
  var agentName = String(data.agentName || "").trim();
  if (ownerKind === MinistryCore.COMPANY && !agentName) agentName = MinistryCore.COMPANY;
  if (ownerKind === MinistryCore.AGENT && !agentName) {
    return { success: false, message: "اسم المعتمد مطلوب لسيارة المعتمد" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ensureFleetSheet_(ss);
  var fleet = readFleet_(ss);
  var targetRow = Number(data.row || 0);
  var carKey = MinistryCore.normalizeCarNumber(carNumber);

  if (!targetRow) {
    for (var i = 0; i < fleet.length; i++) {
      if (fleet[i].carNumberNormalized === carKey) {
        targetRow = fleet[i].row;
        break;
      }
    }
  }

  var rowValues = [
    carNumber,
    String(data.defaultDriver || ""),
    ownerKind,
    agentName,
    (data.active == null ? true : MinistryCore.isTruthyActive(data.active)) ? "1" : "0",
    String(data.notes || "")
  ];

  if (targetRow > 1) {
    sheet.getRange(targetRow, 1, 1, 6).setValues([rowValues]);
  } else {
    sheet.appendRow(rowValues);
  }

  if (ownerKind === MinistryCore.AGENT) {
    saveAgent({ name: agentName, kind: MinistryCore.AGENT, active: true });
  } else {
    ensureAgentDatabaseSheet_(ss, MinistryCore.COMPANY, MinistryCore.COMPANY);
  }

  return { success: true, data: readFleet_(ss) };
}

function bootstrapAgentRegistry(data) {
  data = data || {};
  var applySeed = String(data.applySeed == null ? "true" : data.applySeed).toLowerCase() !== "false";
  var imported = applySeed ? importAgentFleetList(MinistryCore.officialAgentFleetSeed()) : { success: true };

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  ensureAgentsSheet_(ss);
  ensureFleetSheet_(ss);
  ensureAgentDatabaseSheet_(ss, MinistryCore.COMPANY, MinistryCore.COMPANY);
  ensureAgentDatabaseSheet_(ss, MinistryCore.UNCLASSIFIED, "");
  var agents = readAgents_(ss);
  for (var i = 0; i < agents.length; i++) {
    ensureAgentDatabaseSheet_(ss, agents[i].name, agents[i].kind);
  }
  return {
    success: true,
    spreadsheetId: ss.getId(),
    spreadsheetUrl: ss.getUrl(),
    seedApplied: applySeed,
    imported: imported,
    agents: attachAgentDbCounts_(ss, readAgents_(ss)),
    fleet: readFleet_(ss)
  };
}

function organizeHistoricalAgentLedgers(data) {
  data = data || {};
  var startMonth = MinistryCore.resolveMonthKey(data.startMonth || MinistryCore.HISTORY_START_MONTH) || MinistryCore.HISTORY_START_MONTH;
  var boot = bootstrapAgentRegistry({ applySeed: true });
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var available = getAvailableMonths() || [];
  var months = MinistryCore.monthsFromInclusive(available, startMonth);
  var monthResults = [];

  for (var i = 0; i < months.length; i++) {
    monthResults.push(syncAgentDatabases({ month: months[i], period: "all" }));
  }

  writeOrganizationControlSheet_(ss, months, monthResults);

  return {
    success: true,
    spreadsheetId: ss.getId(),
    spreadsheetUrl: ss.getUrl(),
    startMonth: startMonth,
    months: months,
    availableMonths: available,
    bootstrapped: !!(boot && boot.success),
    monthResults: monthResults,
    inventory: MinistryCore.buildOrganizationInventoryRows(),
    agents: attachAgentDbCounts_(ss, readAgents_(ss)),
    fleet: readFleet_(ss)
  };
}

function writeOrganizationControlSheet_(ss, months, monthResults) {
  var sheet = ss.getSheetByName(MinistryCore.ORGANIZATION_SHEET);
  if (!sheet) sheet = ss.insertSheet(MinistryCore.ORGANIZATION_SHEET);
  sheet.clear();

  var rows = [];
  rows.push(["تنظيم المعتمدين والسيارات من " + MinistryCore.HISTORY_START_MONTH + " حتى الشهر الحالي"]);
  rows.push(["الشيت", ss.getName()]);
  rows.push(["عدد المعتمدين/الجهات", (MinistryCore.officialAgentFleetSeed().agents || []).length]);
  rows.push([]);
  rows.push(["سجل كل سيارة ومعتمدها"]);
  rows.push(["المعتمد / الجهة", "النوع", "رقم السيارة", "ورقة القاعدة"]);

  var inventory = MinistryCore.buildOrganizationInventoryRows();
  for (var i = 0; i < inventory.length; i++) {
    rows.push([inventory[i].agentName, inventory[i].kind, inventory[i].carNumber, inventory[i].dbSheet]);
  }

  rows.push([]);
  rows.push(["ترحيل الوصولات حسب الشهر"]);
  rows.push(["الشهر", "وصولات مقروءة", "مرحّل جديد", "مكرر/موجود", "غير مصنف"]);
  var results = monthResults || [];
  for (var m = 0; m < results.length; m++) {
    var r = results[m] || {};
    rows.push([
      r.month || (months && months[m]) || "",
      r.trips || 0,
      r.routed || 0,
      r.skipped || 0,
      r.unclassified || 0
    ]);
  }

  rows.push([]);
  rows.push(["قواعد البيانات المنشأة"]);
  rows.push(["الجهة", "ورقة القاعدة", "عدد الوصولات في القاعدة"]);
  var agents = attachAgentDbCounts_(ss, readAgents_(ss));
  for (var a = 0; a < agents.length; a++) {
    rows.push([agents[a].name, agents[a].dbSheet, agents[a].receiptCount || 0]);
  }
  rows.push(["غير مصنف", MinistryCore.UNCLASSIFIED_DB, ""]);

  sheet.getRange(1, 1, rows.length, 5).setValues(padRows_(rows, 5));
  sheet.getRange(1, 1, 1, 5).merge();
  sheet.setFrozenRows(6);
  sheet.autoResizeColumns(1, 5);
  return sheet.getName();
}

function getOfficialAgentSeed(data) {
  var seed = MinistryCore.officialAgentFleetSeed();
  return {
    success: true,
    agents: MinistryCore.normalizeAgentImportList(seed)
  };
}

function importAgentFleetList(data) {
  data = data || {};
  var list = MinistryCore.normalizeAgentImportList(data);
  if (!list.length) {
    return { success: false, message: "أرسل قائمة المعتمدين: agents[{ name, kind, cars:[{ carNumber, defaultDriver }] }]" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  ensureAgentsSheet_(ss);
  ensureFleetSheet_(ss);

  var createdAgents = 0;
  var createdCars = 0;
  var databases = [];

  for (var i = 0; i < list.length; i++) {
    var item = list[i] || {};
    var name = String(item.name || item.agentName || "").trim();
    var kind = MinistryCore.normalizeOwnerKind(item.kind || item.ownerKind) || MinistryCore.AGENT;
    if (!name) continue;

    saveAgent({ name: name, kind: kind, notes: String(item.notes || ""), active: true });
    createdAgents += 1;
    var db = ensureAgentDatabaseSheet_(ss, name, kind);
    databases.push(db.getName());

    var cars = item.cars || item.vehicles || [];
    for (var c = 0; c < cars.length; c++) {
      var car = cars[c];
      var carNumber = typeof car === "string" ? car : String((car && (car.carNumber || car.number)) || "").trim();
      if (!carNumber) continue;
      saveFleet({
        carNumber: carNumber,
        defaultDriver: typeof car === "object" ? String(car.defaultDriver || car.driver || "") : "",
        ownerKind: kind,
        agentName: name,
        active: true,
        notes: typeof car === "object" ? String(car.notes || "") : ""
      });
      createdCars += 1;
    }
  }

  ensureAgentDatabaseSheet_(ss, MinistryCore.UNCLASSIFIED, "");

  return {
    success: true,
    spreadsheetId: ss.getId(),
    createdAgents: createdAgents,
    createdCars: createdCars,
    databases: databases,
    agents: attachAgentDbCounts_(ss, readAgents_(ss)),
    fleet: readFleet_(ss)
  };
}

function resolveFleetOwnerForCar_(carNumber) {
  var fleet = readFleet_(SpreadsheetApp.openById(SPREADSHEET_ID));
  var match = MinistryCore.resolveFleetMatch(carNumber, fleet);
  if (!match) return null;
  return {
    found: true,
    ownerKind: match.ownerKind,
    agentName: match.agentName || (match.ownerKind === MinistryCore.COMPANY ? MinistryCore.COMPANY : ""),
    defaultDriver: match.defaultDriver || "",
    dbSheet: MinistryCore.agentDatabaseSheetName(
      match.agentName || (match.ownerKind === MinistryCore.COMPANY ? MinistryCore.COMPANY : ""),
      match.ownerKind
    )
  };
}

function ensureAgentDatabaseSheet_(ss, agentName, ownerKind) {
  ss = ss || SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheetName = MinistryCore.agentDatabaseSheetName(agentName, ownerKind);
  var sheet = ss.getSheetByName(sheetName);
  var headers = MinistryCore.agentLedgerHeaders();
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow(headers);
    sheet.setFrozenRows(1);
    return sheet;
  }
  if (sheet.getLastRow() === 0) {
    sheet.appendRow(headers);
    sheet.setFrozenRows(1);
  }
  return sheet;
}

function attachAgentDbCounts_(ss, agents) {
  var list = agents || [];
  for (var i = 0; i < list.length; i++) {
    var name = list[i].dbSheet || MinistryCore.agentDatabaseSheetName(list[i].name, list[i].kind);
    var sheet = ss.getSheetByName(name);
    list[i].dbSheet = name;
    list[i].receiptCount = sheet && sheet.getLastRow() > 1 ? sheet.getLastRow() - 1 : 0;
  }
  return list;
}

function getAgentDbKeySet_(sheet, cache) {
  cache = cache || {};
  var name = sheet.getName();
  if (cache[name]) return cache[name];
  var keys = {};
  var last = sheet.getLastRow();
  if (last > 1) {
    var vals = sheet.getRange(2, 17, last - 1, 1).getValues();
    for (var i = 0; i < vals.length; i++) {
      var key = String(vals[i][0] || "").trim();
      if (key) keys[key] = true;
    }
  }
  cache[name] = keys;
  return keys;
}

function routeReceiptToAgentDb_(ss, receipt, keyCache) {
  ss = ss || SpreadsheetApp.openById(SPREADSHEET_ID);
  var fleet = readFleet_(ss);
  var enriched = receipt && (receipt.classified != null || receipt.unclassified != null)
    ? receipt
    : MinistryCore.enrichReceipt(receipt || {}, fleet);
  var target = MinistryCore.resolveRoutingTarget(enriched, fleet);
  var sheet = ensureAgentDatabaseSheet_(ss, enriched.agentName, enriched.ownerKind);
  var keys = getAgentDbKeySet_(sheet, keyCache || {});
  if (keys[target.routingKey]) {
    return { success: true, routed: false, skipped: true, sheetName: sheet.getName(), agentName: target.agentName };
  }
  sheet.appendRow(MinistryCore.buildAgentLedgerRow(enriched, nowBaghdad_()));
  keys[target.routingKey] = true;
  return {
    success: true,
    routed: true,
    skipped: false,
    sheetName: sheet.getName(),
    agentName: target.agentName,
    unclassified: target.unclassified
  };
}

function routeSavedReceipt_(receipt) {
  try {
    var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
    return routeReceiptToAgentDb_(ss, receipt || {});
  } catch (err) {
    return { success: false, message: String(err) };
  }
}

function syncAgentDatabases(data) {
  data = typeof data === "string" ? { month: data } : (data || {});
  var loaded = loadMinistryReceipts_(data.month, data.period || "all");
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  ensureAgentsSheet_(ss);
  ensureFleetSheet_(ss);
  ensureAgentDatabaseSheet_(ss, MinistryCore.COMPANY, MinistryCore.COMPANY);
  ensureAgentDatabaseSheet_(ss, MinistryCore.UNCLASSIFIED, "");

  var agents = readAgents_(ss);
  for (var a = 0; a < agents.length; a++) {
    ensureAgentDatabaseSheet_(ss, agents[a].name, agents[a].kind);
  }

  var cache = {};
  var routed = 0;
  var skipped = 0;
  var unclassified = 0;
  var bySheet = {};
  var rows = loaded.rows || [];

  for (var i = 0; i < rows.length; i++) {
    var result = routeReceiptToAgentDb_(ss, rows[i], cache);
    if (result && result.routed) routed += 1;
    else skipped += 1;
    if (result && result.unclassified) unclassified += 1;
    if (result && result.sheetName) {
      bySheet[result.sheetName] = (bySheet[result.sheetName] || 0) + (result.routed ? 1 : 0);
    }
  }

  return {
    success: true,
    month: loaded.month,
    period: loaded.period,
    periodLabel: loaded.periodLabel,
    trips: rows.length,
    routed: routed,
    skipped: skipped,
    unclassified: unclassified,
    bySheet: bySheet,
    agents: attachAgentDbCounts_(ss, readAgents_(ss))
  };
}

function getAllReceiptsData(month) {
  var monthKey = MinistryCore.resolveMonthKey(typeof month === "object" && month ? (month.month || month.monthKey) : month);
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  ensureAgentsSheet_(ss);
  ensureFleetSheet_(ss);

  if (!monthKey) {
    return { success: true, data: [], month: "" };
  }

  var fleet = readFleet_(ss);
  var sheets = getMonthDataSheets_(ss, monthKey);
  var result = [];

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var values = sheet.getDataRange().getValues();
    if (!values || values.length === 0) continue;

    var sheetName = String(sheet.getName() || "");
    var header = values[0] || [];
    var hasHeader = isLikelyHeaderRow_(header);
    var colMap = hasHeader ? buildColumnMap_(header) : {};
    var startRow = hasHeader ? 1 : 0;
    var factorySheet = /^f_/i.test(sheetName);

    for (var i = startRow; i < values.length; i++) {
      var row = values[i];
      if (!row || !row.length) continue;

      var docNumber = String(getCellByAliases_(row, colMap, ["docnumber", "doc", "document", "receipt", "رقمالوصل"], 0) || "").trim();
      var driverName = String(getCellByAliases_(row, colMap, ["drivername", "driver", "name", "السائق", "اسمالسائق"], 1) || "").trim();
      var carNumber = String(getCellByAliases_(row, colMap, ["carnumber", "car", "vehicle", "رقمالسيارة"], 2) || "").trim();
      if (!docNumber && !driverName && !carNumber) continue;

      var quantityRaw = getCellByAliases_(row, colMap, ["quantity", "qty", "الكمية"], 5);
      var destination = String(getCellByAliases_(row, colMap, ["station", "destination", "المحطة", "الوجهة", "المعمل"], 7) || "").trim();
      var factory = String(getCellByAliases_(row, colMap, ["factory", "اسمالمعمل", "اسمالجهه"], factorySheet ? 7 : -1) || "").trim();
      var source = String(getCellByAliases_(row, colMap, ["source", "type", "rowtype"], factorySheet ? 12 : -1) || "");
      var owner = String(getCellByAliases_(row, colMap, ["owner", "ownertype", "المالك"], 6) || "").trim();
      if (!owner && factorySheet) {
        owner = String(getCellByAliases_(row, colMap, ["vehicleowner", "مالكالسيارة", "مالكالسيارةاوالمالك"], 10) || "").trim();
      }

      var loadDate = formatSheetDate_(getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], 3));
      var unloadDate = formatSheetDate_(getCellByAliases_(row, colMap, ["unloaddate", "unload", "تاريخالتفريغ"], 4));
      var timestamp = formatSheetDate_(getCellByAliases_(row, colMap, ["sendtime", "timestamp", "createdat", "وقتالارسال"], 9));
      var liters = factorySheet ? 0 : MinistryCore.toNumber(getCellByAliases_(row, colMap, ["liters", "gas", "لتراتالكاز"], 10));

      var isFactory = MinistryCore.isFactorySource(sheetName, source, destination, factory);
      var grossQty = MinistryCore.toNumber(quantityRaw);
      if (isFactory && grossQty <= 0) {
        var shiftedQty = MinistryCore.toNumber(getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], 3));
        if (shiftedQty > 0) grossQty = shiftedQty;
      }

      var qtyTons = MinistryCore.normalizeQtyTon(grossQty);
      var deductionQty = MinistryCore.round3(qtyTons * DEDUCTION_RATE);
      var netQty = MinistryCore.round3(qtyTons - deductionQty);
      var storedPrice = MinistryCore.toNumber(getCellByAliases_(row, colMap, ["price", "profit", "amount", "finalamount", "سعرالنقل"], 13));
      var legacyPrice = MinistryCore.round0(netQty * (isFactory ? PRICE_PER_TON_FACTORY : PRICE_PER_TON_HALAFAYA));

      var enriched = MinistryCore.enrichReceipt({
        row: i + 1,
        sheetName: sheetName,
        docNumber: docNumber,
        driverName: driverName,
        carNumber: carNumber,
        loadDate: loadDate,
        unloadDate: unloadDate,
        quantity: qtyTons,
        deductionQty: deductionQty,
        netQuantity: netQty,
        owner: owner,
        destination: destination || factory,
        factory: factory,
        source: source,
        imageUrl: String(getCellByAliases_(row, colMap, ["imageurl", "image", "photo", "رابطالصورة", "صوره"], 8) || ""),
        timestamp: timestamp,
        liters: liters,
        bojer: String(getCellByAliases_(row, colMap, ["bogernumber", "bojer", "رقمالبوجر"], 11) || ""),
        distance: MinistryCore.toNumber(getCellByAliases_(row, colMap, ["distance", "المسافة"], 12)),
        storedPrice: storedPrice,
        driverTripPrice: storedPrice,
        price: legacyPrice,
        gasCost: MinistryCore.gasAmount(liters),
        kroa: 0,
        notes: String(getCellByAliases_(row, colMap, ["notes", "note", "remarks", "ملاحظات"], factorySheet ? 11 : 14) || ""),
        month: monthKey
      }, fleet);

      result.push(enriched);
    }
  }

  result.sort(function(a, b) {
    return String(b.timestamp || "").localeCompare(String(a.timestamp || ""));
  });

  return {
    success: true,
    data: result,
    month: monthKey
  };
}

function getMaintenanceData(month) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("سجل الصيانة");
  if (!sheet) return { success: true, data: [] };

  var monthKey = MinistryCore.resolveMonthKey(typeof month === "object" && month ? (month.month || month.monthKey) : month);
  var values = sheet.getDataRange().getValues();
  var list = [];

  for (var i = 1; i < values.length; i++) {
    var rowDate = values[i][5];
    if (monthKey && !sameMonthKey_(rowDate, monthKey)) continue;
    list.push({
      row: i + 1,
      requestId: String(values[i][0] || ""),
      driverName: String(values[i][1] || ""),
      carNumber: String(values[i][2] || ""),
      type: String(values[i][7] || values[i][3] || ""),
      cost: MinistryCore.toNumber(values[i][8]),
      date: formatSheetDate_(values[i][5]),
      notes: String(values[i][10] || ""),
      imageUrl: String(values[i][11] || "")
    });
  }

  return { success: true, data: list };
}

function loadMinistryReceipts_(month, period) {
  var receipts = getAllReceiptsData(month);
  var monthKey = receipts.month || MinistryCore.resolveMonthKey(month);
  var rows = MinistryCore.filterPeriod(receipts.data || [], period);
  return {
    success: receipts.success,
    month: monthKey,
    period: String(period == null ? "all" : period),
    periodLabel: MinistryCore.periodLabel(period),
    rows: rows
  };
}

function getPeriodStats(data) {
  data = typeof data === "string" ? { month: data } : (data || {});
  var loaded = loadMinistryReceipts_(data.month, data.period || data.half || "all");
  var stats = MinistryCore.buildPeriodStats(loaded.rows);
  return {
    success: true,
    month: loaded.month,
    period: loaded.period,
    periodLabel: loaded.periodLabel,
    totals: stats.totals,
    classifiedTotals: stats.classifiedTotals,
    agents: stats.agents,
    company: stats.company,
    unclassifiedCount: stats.unclassified.length,
    missingDestinationCount: stats.missingDestination.length,
    unclassified: stats.unclassified,
    missingDestination: stats.missingDestination
  };
}

function getUnclassifiedReceipts(data) {
  data = typeof data === "string" ? { month: data } : (data || {});
  var stats = getPeriodStats(data);
  return {
    success: true,
    month: stats.month,
    period: stats.period,
    unclassified: stats.unclassified || [],
    missingDestination: stats.missingDestination || [],
    unclassifiedCount: stats.unclassifiedCount || 0,
    missingDestinationCount: stats.missingDestinationCount || 0
  };
}

function generateAgentStatements(data) {
  data = data || {};
  var loaded = loadMinistryReceipts_(data.month, data.period || "all");
  var statements = MinistryCore.buildAgentStatements(loaded.rows, MinistryCore.PAGE_SIZE);
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var written = writeStatementSheets_(ss, statements, "AGT10", loaded.month, loaded.period);
  return {
    success: true,
    month: loaded.month,
    period: loaded.period,
    periodLabel: loaded.periodLabel,
    count: written.length,
    sheets: written,
    spreadsheetUrl: ss.getUrl()
  };
}

function generateCompanyStatements(data) {
  data = data || {};
  var loaded = loadMinistryReceipts_(data.month, data.period || "all");
  var statement = MinistryCore.buildCompanyStatement(loaded.rows, MinistryCore.PAGE_SIZE);
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var written = writeStatementSheets_(ss, [statement], "CO10", loaded.month, loaded.period);
  return {
    success: true,
    month: loaded.month,
    period: loaded.period,
    periodLabel: loaded.periodLabel,
    count: written.length,
    sheets: written,
    spreadsheetUrl: ss.getUrl()
  };
}

function exportMinistryPack(data) {
  data = data || {};
  var loaded = loadMinistryReceipts_(data.month, data.period || "all");
  var stats = MinistryCore.buildPeriodStats(loaded.rows);
  var force = String(data.force || "").toLowerCase() === "true" || data.force === true;

  if (MinistryCore.shouldBlockMinistryExport(stats, force)) {
    return {
      success: false,
      blocked: true,
      message: "يوجد " + stats.unclassified.length + " وصل غير مصنف. صنّف السيارات في السجل أو أكّد التصدير.",
      unclassifiedCount: stats.unclassified.length,
      missingDestinationCount: stats.missingDestination.length,
      unclassified: stats.unclassified.slice(0, 50)
    };
  }

  var sourceSs = SpreadsheetApp.openById(SPREADSHEET_ID);
  var stamp = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd_HHmmss");
  var packName = "MINISTRY_" + loaded.month + "_" + String(loaded.period || "all") + "_" + stamp;
  var pack = SpreadsheetApp.create(packName);
  var packSs = SpreadsheetApp.openById(pack.getId());

  writePeriodSummarySheet_(packSs, loaded, stats);

  var coverCopied = false;
  var template = sourceSs.getSheetByName(TEMPLATE_60_SHEET_NAME);
  if (template) {
    var copied = template.copyTo(packSs);
    copied.setName("غلاف_60");
    coverCopied = true;
  }

  var agentStatements = MinistryCore.buildAgentStatements(loaded.rows, MinistryCore.PAGE_SIZE);
  var companyStatement = MinistryCore.buildCompanyStatement(loaded.rows, MinistryCore.PAGE_SIZE);
  var agentSheets = writeStatementSheets_(packSs, agentStatements, "AGT10", loaded.month, loaded.period);
  var companySheets = writeStatementSheets_(packSs, [companyStatement], "CO10", loaded.month, loaded.period);

  var defaultSheet = packSs.getSheetByName("Sheet1");
  if (defaultSheet && packSs.getSheets().length > 1) {
    packSs.deleteSheet(defaultSheet);
  }

  var pdfUrl = "https://docs.google.com/spreadsheets/d/" + pack.getId() + "/export?format=pdf";
  var xlsUrl = "https://docs.google.com/spreadsheets/d/" + pack.getId() + "/export?format=xlsx";

  return {
    success: true,
    month: loaded.month,
    period: loaded.period,
    periodLabel: loaded.periodLabel,
    packName: packName,
    packId: pack.getId(),
    packUrl: pack.getUrl(),
    excelUrl: xlsUrl,
    pdfUrl: pdfUrl,
    template60Touched: false,
    coverCopied: coverCopied,
    totals: stats.totals,
    agentSheets: agentSheets,
    companySheets: companySheets,
    unclassifiedCount: stats.unclassified.length,
    missingDestinationCount: stats.missingDestination.length
  };
}

function writeStatementSheets_(ss, statements, prefix, monthKey, period) {
  var written = [];
  var list = statements || [];
  for (var i = 0; i < list.length; i++) {
    var statement = list[i];
    if (!statement || !statement.pages || !statement.pages.length) continue;
    var sheetName = uniqueSheetName_(ss, MinistryCore.statementSheetName(prefix, monthKey, period, statement.name));
    replaceOrCreateSheet_(ss, sheetName);
    var sheet = ss.getSheetByName(sheetName);
    writeStatementSheet_(sheet, statement, monthKey, period);
    written.push({
      name: statement.name,
      kind: statement.kind,
      sheetName: sheetName,
      pages: statement.pages.length,
      totals: statement.totals
    });
  }
  return written;
}

function writeStatementSheet_(sheet, statement, monthKey, period) {
  var rows = [];
  rows.push(["كشف حساب الوزارة"]);
  rows.push(["الجهة", statement.name || ""]);
  rows.push(["الشهر", monthKey || ""]);
  rows.push(["الفترة", MinistryCore.periodLabel(period)]);
  rows.push(["النوع", statement.kind === MinistryCore.COMPANY ? "سيارات الشركة" : "معتمد"]);
  rows.push([]);
  rows.push(["ت", "رقم الوصل", "السائق", "رقم السيارة", "تاريخ التحميل", "تاريخ التفريغ", "الكمية", "سعر الطن", "المبلغ"]);

  var pages = statement.pages || [];
  for (var p = 0; p < pages.length; p++) {
    var page = pages[p];
    if (p > 0) {
      rows.push([]);
      rows.push(["صفحة " + page.page, "", "", "", "", "", "", "", ""]);
      rows.push(["ت", "رقم الوصل", "السائق", "رقم السيارة", "تاريخ التحميل", "تاريخ التفريغ", "الكمية", "سعر الطن", "المبلغ"]);
    }
    for (var i = 0; i < page.rows.length; i++) {
      var r = page.rows[i];
      rows.push([
        r.seq,
        r.docNumber,
        r.driverName,
        r.carNumber,
        r.loadDate,
        r.unloadDate,
        r.qty,
        r.pricePerTon,
        r.amount
      ]);
    }
    rows.push(["", "", "", "", "", "مجموع الصفحة", page.totalQty, "", page.totalAmount]);
  }

  var totals = statement.totals || MinistryCore.emptyTotals();
  rows.push([]);
  rows.push(["", "", "", "", "", "المجموع الكلي", totals.qty, "", totals.net]);

  sheet.clear();
  sheet.getRange(1, 1, rows.length, 9).setValues(padRows_(rows, 9));
  sheet.getRange(1, 1, 1, 9).merge();
  sheet.setFrozenRows(7);
  sheet.autoResizeColumns(1, 9);
}

function writePeriodSummarySheet_(ss, loaded, stats) {
  var sheet = replaceOrCreateSheet_(ss, MinistryCore.sanitizeSheetName("MINP_" + loaded.month + "_" + loaded.period));
  var rows = [];
  rows.push(["إجمالي فترة الوزارة"]);
  rows.push(["الشهر", loaded.month]);
  rows.push(["الفترة", loaded.periodLabel]);
  rows.push(["عدد الوصولات", stats.totals.trips]);
  rows.push(["الكمية الكلية (طن)", stats.totals.qty]);
  rows.push(["حساب المحطات", stats.totals.stationAmount]);
  rows.push(["حساب المعامل", stats.totals.factoryAmount]);
  rows.push(["الكاز", stats.totals.gas]);
  rows.push(["الصافي", stats.totals.net]);
  rows.push(["غير مصنف", stats.unclassified.length]);
  rows.push(["بلا وجهة", stats.missingDestination.length]);
  rows.push([]);
  rows.push(["تفصيل المعتمدين"]);
  rows.push(["المعتمد", "وصولات", "كمية", "محطات", "معامل", "كاز", "صافي"]);
  for (var i = 0; i < stats.agents.length; i++) {
    var a = stats.agents[i];
    rows.push([a.name, a.totals.trips, a.totals.qty, a.totals.stationAmount, a.totals.factoryAmount, a.totals.gas, a.totals.net]);
  }
  rows.push([]);
  rows.push(["تفصيل سيارات الشركة"]);
  rows.push(["رقم السيارة", "السائق", "وصولات", "كمية", "محطات", "معامل", "كاز", "صافي"]);
  for (var c = 0; c < stats.company.cars.length; c++) {
    var car = stats.company.cars[c];
    rows.push([
      car.carNumber,
      car.driverName,
      car.totals.trips,
      car.totals.qty,
      car.totals.stationAmount,
      car.totals.factoryAmount,
      car.totals.gas,
      car.totals.net
    ]);
  }

  sheet.clear();
  sheet.getRange(1, 1, rows.length, 8).setValues(padRows_(rows, 8));
  sheet.getRange(1, 1, 1, 8).merge();
  sheet.autoResizeColumns(1, 8);
}

function getMonthDataSheets_(ss, monthKey) {
  var key = MinistryCore.resolveMonthKey(monthKey);
  if (!key) return [];
  var sheets = ss.getSheets();
  var matched = [];
  for (var i = 0; i < sheets.length; i++) {
    var name = String(sheets[i].getName() || "").trim();
    if (MinistryCore.isMonthDataSheet(name) && MinistryCore.extractMonthKeyFromSheetName(name) === key) {
      matched.push(sheets[i]);
    }
  }
  matched.sort(function(a, b) {
    return String(a.getName()).localeCompare(String(b.getName()));
  });
  return matched;
}

function isLikelyHeaderRow_(row) {
  if (!row || !row.length) return false;
  var known = {
    docnumber: true,
    drivername: true,
    carnumber: true,
    loaddate: true,
    unloaddate: true,
    quantity: true,
    station: true,
    destination: true,
    price: true,
    liters: true,
    رقمالوصل: true,
    اسمالسائق: true,
    رقمالسيارة: true,
    تاريخالتحميل: true,
    تاريخالتفريغ: true,
    الكمية: true,
    المحطة: true,
    سعرالنقل: true
  };
  var hits = 0;
  for (var i = 0; i < row.length; i++) {
    var token = MinistryCore.normalizeHeaderToken(row[i]);
    if (token && known[token]) hits += 1;
  }
  return hits >= 2;
}

function buildColumnMap_(headerRow) {
  var map = {};
  if (!headerRow || !headerRow.length) return map;
  for (var i = 0; i < headerRow.length; i++) {
    var key = MinistryCore.normalizeHeaderToken(headerRow[i]);
    if (!key) continue;
    if (typeof map[key] === "undefined") map[key] = i;
  }
  return map;
}

function getCellByAliases_(row, colMap, aliases, fallbackIndex) {
  for (var i = 0; i < aliases.length; i++) {
    var key = MinistryCore.normalizeHeaderToken(aliases[i]);
    if (typeof colMap[key] !== "undefined") return row[colMap[key]];
  }
  if (typeof fallbackIndex === "number" && fallbackIndex >= 0) return row[fallbackIndex];
  return "";
}

function formatSheetDate_(value) {
  if (value && Object.prototype.toString.call(value) === "[object Date]" && !isNaN(value.getTime())) {
    return Utilities.formatDate(value, "Asia/Baghdad", "yyyy-MM-dd");
  }
  return String(value || "").trim();
}

function sameMonthKey_(dateValue, monthKey) {
  var key = MinistryCore.resolveMonthKey(monthKey);
  if (!key) return true;
  if (dateValue && Object.prototype.toString.call(dateValue) === "[object Date]" && !isNaN(dateValue.getTime())) {
    return Utilities.formatDate(dateValue, "Asia/Baghdad", "yyyy_MM") === key;
  }
  var parsed = MinistryCore.parseDateParts(dateValue);
  if (!parsed) return false;
  return parsed.year + "_" + ("0" + parsed.month).slice(-2) === key;
}

function replaceOrCreateSheet_(ss, name) {
  var existing = ss.getSheetByName(name);
  if (existing) ss.deleteSheet(existing);
  return ss.insertSheet(name);
}

function uniqueSheetName_(ss, name) {
  var base = MinistryCore.sanitizeSheetName(name);
  if (!ss.getSheetByName(base)) return base;
  var i = 2;
  while (ss.getSheetByName((base + "_" + i).slice(0, 90))) i += 1;
  return (base + "_" + i).slice(0, 90);
}

function padRows_(rows, width) {
  var out = [];
  for (var i = 0; i < rows.length; i++) {
    var row = (rows[i] || []).slice();
    while (row.length < width) row.push("");
    out.push(row.slice(0, width));
  }
  return out;
}

function cleanupSafetyPreview(data) {
  data = data || {};
  var monthKey = MinistryCore.resolveMonthKey(data.month);
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var generated = listGeneratedSheets_(ss, monthKey);
  return {
    success: true,
    month: monthKey,
    summary: {
      generatedSheetsCount: generated.length,
      routedEmptySheetsCount: 0,
      sourceSheetsWithTestRows: 0
    },
    generatedSheets: generated,
    protectedSheets: [AGENTS_SHEET_NAME, FLEET_SHEET_NAME, TEMPLATE_60_SHEET_NAME]
  };
}

function cleanupSafetyApply(data) {
  data = data || {};
  var monthKey = MinistryCore.resolveMonthKey(data.month);
  if (!monthKey) return { success: false, message: "month مطلوب" };
  var backupFileId = String(data.backupFileId || "").trim();
  if (!backupFileId) return { success: false, message: "يجب إنشاء نسخة احتياطية أولاً" };

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var generated = listGeneratedSheets_(ss, monthKey);
  var deleted = [];
  for (var i = 0; i < generated.length; i++) {
    var name = generated[i];
    if (name === TEMPLATE_60_SHEET_NAME || MinistryCore.isProtectedRegistrySheet(name) || MinistryCore.isMonthDataSheet(name)) {
      continue;
    }
    var sh = ss.getSheetByName(name);
    if (sh) {
      ss.deleteSheet(sh);
      deleted.push(name);
    }
  }

  return {
    success: true,
    month: monthKey,
    generatedCleanup: { success: true, deletedCount: deleted.length, deleted: deleted },
    testArtifactsCleanup: { success: true, deletedRowsCount: 0 }
  };
}

function listGeneratedSheets_(ss, monthKey) {
  var sheets = ss.getSheets();
  var out = [];
  for (var i = 0; i < sheets.length; i++) {
    var name = String(sheets[i].getName() || "").trim();
    if (!MinistryCore.isGeneratedSupportSheet(name)) continue;
    if (monthKey && MinistryCore.extractMonthKeyFromSheetName(name) !== monthKey) continue;
    out.push(name);
  }
  return out;
}
