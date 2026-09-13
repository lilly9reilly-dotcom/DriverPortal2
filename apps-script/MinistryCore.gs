/**
 * Pure ministry-accounting helpers.
 * No SpreadsheetApp / DriveApp calls. Safe to eval from Node tests.
 */
var MinistryCore = {
  STATION_RATE: 34300,
  FACTORY_RATE: 8500,
  GAS_RATE: 430,
  PAGE_SIZE: 10,
  UNCLASSIFIED: "غير مصنف",
  COMPANY: "شركة",
  AGENT: "معتمد",
  MISSING_DESTINATION: "بلا وجهة",
  AGENT_DB_PREFIX: "DB_",
  COMPANY_DB: "DB_شركة",
  UNCLASSIFIED_DB: "DB_غير_مصنف",
  HISTORY_START_MONTH: "2026_06",
  ORGANIZATION_SHEET: "تنظيم_المعتمدين"
};

MinistryCore.round3 = function(v) {
  return Math.round((Number(v || 0) + Number.EPSILON) * 1000) / 1000;
};

MinistryCore.round0 = function(v) {
  return Math.round(Number(v || 0));
};

MinistryCore.toNumber = function(v) {
  if (v == null || v === "") return 0;
  var s = String(v)
    .replace(/[٠-٩]/g, function(d) { return "٠١٢٣٤٥٦٧٨٩".indexOf(d); })
    .replace(/[^\d.\-]/g, "")
    .trim();
  var n = parseFloat(s);
  return isNaN(n) ? 0 : n;
};

MinistryCore.normalizeText = function(value) {
  return String(value || "")
    .replace(/\u00A0/g, " ")
    .replace(/\s+/g, " ")
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .trim()
    .toLowerCase();
};

MinistryCore.normalizeHeaderToken = function(value) {
  return MinistryCore.normalizeText(value).replace(/[^a-z0-9\u0600-\u06FF]/g, "");
};

MinistryCore.normalizeCarNumber = function(value) {
  var s = String(value == null ? "" : value)
    .replace(/[٠-٩]/g, function(d) { return "٠١٢٣٤٥٦٧٨٩".indexOf(d); })
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .replace(/[\u064B-\u065F\u0670]/g, "")
    .replace(/[^\u0600-\u06FFa-zA-Z0-9]/g, "")
    .trim()
    .toUpperCase();
  return s;
};

MinistryCore.normalizeQtyTon = function(value) {
  var n = MinistryCore.toNumber(value);
  if (!isFinite(n) || n <= 0) return 0;
  if (n > 1000000) return 0;
  if (n >= 1000) n = n / 1000;
  return MinistryCore.round3(n);
};

MinistryCore.normalizeLiters = function(value) {
  var n = MinistryCore.toNumber(value);
  if (n <= 0 || n > 5000) return 0;
  return MinistryCore.round3(n);
};

MinistryCore.parseDateParts = function(raw) {
  if (raw && Object.prototype.toString.call(raw) === "[object Date]" && !isNaN(raw.getTime())) {
    return { year: raw.getFullYear(), month: raw.getMonth() + 1, day: raw.getDate() };
  }

  var text = String(raw || "")
    .replace(/[٠-٩]/g, function(d) { return "٠١٢٣٤٥٦٧٨٩".indexOf(d); })
    .trim();
  if (!text) return null;

  text = text.split("T")[0].split(" ")[0].replace(/\./g, "/").replace(/-/g, "/");

  var m = text.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})$/);
  if (m) return { year: Number(m[1]), month: Number(m[2]), day: Number(m[3]) };

  m = text.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (m) return { year: Number(m[3]), month: Number(m[2]), day: Number(m[1]) };

  return null;
};

MinistryCore.getPeriod15 = function(dateValue) {
  var parts = MinistryCore.parseDateParts(dateValue);
  if (!parts || !parts.day) return 1;
  return parts.day <= 15 ? 1 : 2;
};

MinistryCore.resolveMonthKey = function(value) {
  var s = String(value || "").trim();
  if (!s) return "";
  s = s.replace(/[٠-٩]/g, function(d) { return "٠١٢٣٤٥٦٧٨٩".indexOf(d); });
  s = s.replace(/\//g, "_").replace(/-/g, "_").replace(/\s+/g, "");
  var m = s.match(/^(\d{4})_(\d{1,2})$/);
  if (m) return m[1] + "_" + ("0" + m[2]).slice(-2);
  m = s.match(/^(\d{1,2})_(\d{4})$/);
  if (m) return m[2] + "_" + ("0" + m[1]).slice(-2);
  m = s.match(/(\d{4}_\d{2})/);
  return m ? m[1] : "";
};

MinistryCore.extractMonthKeyFromSheetName = function(name) {
  var s = String(name || "").trim();
  var m = s.match(/(\d{4}_\d{2})/);
  if (m) return m[1];
  var m2 = s.match(/(\d{1,2}_\d{4})/);
  return m2 ? MinistryCore.resolveMonthKey(m2[1]) : "";
};

MinistryCore.isMonthDataSheet = function(name) {
  return /^(F_)?\d{4}_\d{2}$/i.test(String(name || "").trim());
};

MinistryCore.isFactorySource = function(sheetName, sourceValue, destinationValue, factoryValue) {
  var bySheet = /^f_/i.test(String(sheetName || ""));
  var dest = String(destinationValue || "");
  var factory = String(factoryValue || "");
  var source = String(sourceValue || "").toLowerCase();
  return bySheet || source.indexOf("factory") > -1 || dest.indexOf("معمل") > -1 || factory.indexOf("معمل") > -1;
};

MinistryCore.ministryPricePerTon = function(isFactory) {
  return isFactory ? MinistryCore.FACTORY_RATE : MinistryCore.STATION_RATE;
};

MinistryCore.ministryAmount = function(qtyTons, isFactory) {
  return MinistryCore.round0(MinistryCore.normalizeQtyTon(qtyTons) * MinistryCore.ministryPricePerTon(isFactory));
};

MinistryCore.gasAmount = function(liters) {
  return MinistryCore.round0(MinistryCore.normalizeLiters(liters) * MinistryCore.GAS_RATE);
};

MinistryCore.normalizeOwnerKind = function(value) {
  var n = MinistryCore.normalizeText(value);
  if (!n) return "";
  if (n.indexOf("شرك") >= 0 || n === "company") return MinistryCore.COMPANY;
  if (n.indexOf("معتمد") >= 0 || n === "agent" || n.indexOf("وكيل") >= 0) return MinistryCore.AGENT;
  return String(value || "").trim();
};

MinistryCore.isCompanyKind = function(value) {
  return MinistryCore.normalizeOwnerKind(value) === MinistryCore.COMPANY;
};

MinistryCore.isTruthyActive = function(value) {
  var s = String(value == null ? "1" : value).trim().toLowerCase();
  return s !== "0" && s !== "false" && s !== "off" && s !== "موقوف" && s !== "معطل";
};

MinistryCore.resolveFleetMatch = function(carNumber, fleetRows) {
  var key = MinistryCore.normalizeCarNumber(carNumber);
  if (!key) return null;
  var list = fleetRows || [];
  for (var i = 0; i < list.length; i++) {
    var row = list[i] || {};
    if (!MinistryCore.isTruthyActive(row.active == null ? 1 : row.active)) continue;
    if (MinistryCore.normalizeCarNumber(row.carNumber) === key) return row;
  }
  return null;
};

MinistryCore.classifyReceipt = function(receipt, fleetRows) {
  var row = receipt || {};
  var fleet = MinistryCore.resolveFleetMatch(row.carNumber, fleetRows);
  var destination = String(row.destination || row.station || row.factory || "").trim();
  var ownerKind = "";
  var agentName = "";
  var classified = false;

  if (fleet) {
    ownerKind = MinistryCore.normalizeOwnerKind(fleet.ownerKind) || MinistryCore.AGENT;
    if (ownerKind === MinistryCore.COMPANY) {
      agentName = String(fleet.agentName || "").trim() || MinistryCore.COMPANY;
    } else {
      agentName = String(fleet.agentName || "").trim();
    }
    classified = !!agentName;
  }

  if (!classified) {
    ownerKind = "";
    agentName = MinistryCore.UNCLASSIFIED;
  }

  return {
    carNumberNormalized: MinistryCore.normalizeCarNumber(row.carNumber),
    ownerKind: classified ? ownerKind : "",
    agentName: agentName,
    classified: classified,
    unclassified: !classified,
    missingDestination: !destination,
    defaultDriver: fleet ? String(fleet.defaultDriver || "") : ""
  };
};

MinistryCore.enrichReceipt = function(receipt, fleetRows) {
  var row = receipt || {};
  var isFactory = MinistryCore.isFactorySource(row.sheetName, row.source, row.destination || row.station, row.factory);
  var qty = MinistryCore.normalizeQtyTon(row.quantity);
  var liters = MinistryCore.normalizeLiters(row.liters);
  var classInfo = MinistryCore.classifyReceipt(row, fleetRows);
  var pricePerTon = MinistryCore.ministryPricePerTon(isFactory);
  var amount = MinistryCore.round0(qty * pricePerTon);
  var gas = MinistryCore.gasAmount(liters);
  var period15 = MinistryCore.getPeriod15(row.unloadDate || row.loadDate || row.timestamp);

  var out = {};
  for (var k in row) {
    if (Object.prototype.hasOwnProperty.call(row, k)) out[k] = row[k];
  }
  out.carNumberRaw = String(row.carNumber || "");
  out.carNumberNormalized = classInfo.carNumberNormalized;
  out.carNumber = classInfo.carNumberNormalized || String(row.carNumber || "");
  out.ownerKind = classInfo.ownerKind;
  out.agentName = classInfo.agentName;
  out.classified = classInfo.classified;
  out.unclassified = classInfo.unclassified;
  out.missingDestination = classInfo.missingDestination;
  out.isFactory = isFactory;
  out.ministryQty = qty;
  out.ministryPricePerTon = pricePerTon;
  out.ministryAmount = amount;
  out.ministryGas = gas;
  out.period15 = period15;
  return out;
};

MinistryCore.matchesPeriod = function(period15, periodFilter) {
  var p = String(periodFilter == null ? "all" : periodFilter).trim().toLowerCase();
  if (!p || p === "all" || p === "full" || p === "month") return true;
  if (p === "1" || p === "first" || p === "h1") return Number(period15) === 1;
  if (p === "2" || p === "second" || p === "h2") return Number(period15) === 2;
  return Number(period15) === Number(p);
};

MinistryCore.periodLabel = function(periodFilter) {
  var p = String(periodFilter == null ? "all" : periodFilter).trim().toLowerCase();
  if (p === "1" || p === "first" || p === "h1") return "1-15";
  if (p === "2" || p === "second" || p === "h2") return "16-نهاية الشهر";
  return "كامل الشهر";
};

MinistryCore.filterPeriod = function(rows, periodFilter) {
  var list = rows || [];
  var out = [];
  for (var i = 0; i < list.length; i++) {
    if (MinistryCore.matchesPeriod(list[i] && list[i].period15, periodFilter)) out.push(list[i]);
  }
  return out;
};

MinistryCore.emptyTotals = function() {
  return {
    trips: 0,
    qty: 0,
    stationQty: 0,
    factoryQty: 0,
    stationAmount: 0,
    factoryAmount: 0,
    gas: 0,
    net: 0
  };
};

MinistryCore.addTotals = function(target, row) {
  var t = target || MinistryCore.emptyTotals();
  var qty = MinistryCore.normalizeQtyTon(row && (row.ministryQty != null ? row.ministryQty : row.quantity));
  var isFactory = !!(row && row.isFactory);
  var amount = row && row.ministryAmount != null
    ? MinistryCore.round0(row.ministryAmount)
    : MinistryCore.ministryAmount(qty, isFactory);
  var gas = row && row.ministryGas != null
    ? MinistryCore.round0(row.ministryGas)
    : MinistryCore.gasAmount(row && row.liters);
  t.trips += 1;
  t.qty = MinistryCore.round3(t.qty + qty);
  if (isFactory) {
    t.factoryQty = MinistryCore.round3(t.factoryQty + qty);
    t.factoryAmount += amount;
  } else {
    t.stationQty = MinistryCore.round3(t.stationQty + qty);
    t.stationAmount += amount;
  }
  t.gas += gas;
  t.net = t.stationAmount + t.factoryAmount;
  return t;
};

MinistryCore.summarizeRows = function(rows) {
  var totals = MinistryCore.emptyTotals();
  var list = rows || [];
  for (var i = 0; i < list.length; i++) MinistryCore.addTotals(totals, list[i]);
  return totals;
};

MinistryCore.groupReceipts = function(rows) {
  var agents = {};
  var company = [];
  var unclassified = [];
  var missingDestination = [];
  var list = rows || [];

  for (var i = 0; i < list.length; i++) {
    var row = list[i] || {};
    if (row.missingDestination) missingDestination.push(row);
    if (row.unclassified || !row.classified) {
      unclassified.push(row);
      continue;
    }
    if (row.ownerKind === MinistryCore.COMPANY) {
      company.push(row);
      continue;
    }
    var name = String(row.agentName || "").trim() || MinistryCore.UNCLASSIFIED;
    if (!agents[name]) agents[name] = [];
    agents[name].push(row);
  }

  return {
    agents: agents,
    company: company,
    unclassified: unclassified,
    missingDestination: missingDestination
  };
};

MinistryCore.sortStatementRows = function(rows) {
  var list = (rows || []).slice();
  list.sort(function(a, b) {
    var da = String((a && (a.loadDate || a.unloadDate)) || "");
    var db = String((b && (b.loadDate || b.unloadDate)) || "");
    if (da !== db) return da.localeCompare(db);
    return String((a && a.docNumber) || "").localeCompare(String((b && b.docNumber) || ""), "ar");
  });
  return list;
};

MinistryCore.statementRow = function(row, index) {
  var qty = MinistryCore.normalizeQtyTon(row && (row.ministryQty != null ? row.ministryQty : row.quantity));
  var isFactory = !!(row && row.isFactory);
  var price = row && row.ministryPricePerTon != null
    ? MinistryCore.round0(row.ministryPricePerTon)
    : MinistryCore.ministryPricePerTon(isFactory);
  var amount = row && row.ministryAmount != null
    ? MinistryCore.round0(row.ministryAmount)
    : MinistryCore.round0(qty * price);
  return {
    seq: index,
    docNumber: String((row && row.docNumber) || ""),
    driverName: String((row && row.driverName) || ""),
    carNumber: String((row && (row.carNumberNormalized || row.carNumber)) || ""),
    loadDate: String((row && row.loadDate) || ""),
    unloadDate: String((row && row.unloadDate) || ""),
    qty: qty,
    pricePerTon: price,
    amount: amount
  };
};

MinistryCore.buildStatementPages = function(rows, pageSize) {
  var size = Number(pageSize || MinistryCore.PAGE_SIZE);
  if (!size || size < 1) size = 10;
  var sorted = MinistryCore.sortStatementRows(rows);
  var pages = [];
  for (var i = 0; i < sorted.length; i += size) {
    var slice = sorted.slice(i, i + size);
    var mapped = [];
    var qty = 0;
    var amount = 0;
    for (var j = 0; j < slice.length; j++) {
      var item = MinistryCore.statementRow(slice[j], i + j + 1);
      mapped.push(item);
      qty += item.qty;
      amount += item.amount;
    }
    pages.push({
      page: pages.length + 1,
      rows: mapped,
      totalQty: MinistryCore.round3(qty),
      totalAmount: MinistryCore.round0(amount)
    });
  }
  return pages;
};

MinistryCore.buildAgentStatements = function(rows, pageSize) {
  var grouped = MinistryCore.groupReceipts(rows);
  var names = Object.keys(grouped.agents).sort(function(a, b) {
    return String(a).localeCompare(String(b), "ar");
  });
  var statements = [];
  for (var i = 0; i < names.length; i++) {
    var name = names[i];
    var pages = MinistryCore.buildStatementPages(grouped.agents[name], pageSize);
    statements.push({
      kind: MinistryCore.AGENT,
      name: name,
      pages: pages,
      totals: MinistryCore.summarizeRows(grouped.agents[name])
    });
  }
  return statements;
};

MinistryCore.buildCompanyStatement = function(rows, pageSize) {
  var grouped = MinistryCore.groupReceipts(rows);
  return {
    kind: MinistryCore.COMPANY,
    name: "سيارات الشركة",
    pages: MinistryCore.buildStatementPages(grouped.company, pageSize),
    totals: MinistryCore.summarizeRows(grouped.company)
  };
};

MinistryCore.shouldBlockMinistryExport = function(stats, force) {
  if (force) return false;
  return !!(stats && stats.unclassified && stats.unclassified.length);
};

MinistryCore.buildPeriodStats = function(rows) {
  var grouped = MinistryCore.groupReceipts(rows);
  var agentNames = Object.keys(grouped.agents).sort(function(a, b) {
    return String(a).localeCompare(String(b), "ar");
  });
  var agents = [];
  for (var i = 0; i < agentNames.length; i++) {
    agents.push({
      name: agentNames[i],
      kind: MinistryCore.AGENT,
      totals: MinistryCore.summarizeRows(grouped.agents[agentNames[i]])
    });
  }

  var cars = {};
  for (var c = 0; c < grouped.company.length; c++) {
    var row = grouped.company[c] || {};
    var car = String(row.carNumberNormalized || row.carNumber || "").trim() || "بدون رقم";
    if (!cars[car]) cars[car] = [];
    cars[car].push(row);
  }
  var companyCars = Object.keys(cars).sort().map(function(car) {
    return {
      carNumber: car,
      driverName: String((cars[car][0] && cars[car][0].driverName) || ""),
      totals: MinistryCore.summarizeRows(cars[car])
    };
  });

  return {
    totals: MinistryCore.summarizeRows(rows),
    classifiedTotals: MinistryCore.summarizeRows(
      (rows || []).filter(function(r) { return r && r.classified; })
    ),
    agents: agents,
    company: {
      name: "سيارات الشركة",
      totals: MinistryCore.summarizeRows(grouped.company),
      cars: companyCars
    },
    unclassified: grouped.unclassified,
    missingDestination: grouped.missingDestination
  };
};

MinistryCore.monthsFromInclusive = function(availableMonths, startKey) {
  var start = MinistryCore.resolveMonthKey(startKey || MinistryCore.HISTORY_START_MONTH) || MinistryCore.HISTORY_START_MONTH;
  var list = availableMonths || [];
  var out = [];
  for (var i = 0; i < list.length; i++) {
    var key = MinistryCore.resolveMonthKey(list[i]);
    if (key && key >= start) out.push(key);
  }
  out.sort();
  var unique = [];
  for (var u = 0; u < out.length; u++) {
    if (unique.indexOf(out[u]) < 0) unique.push(out[u]);
  }
  return unique;
};

MinistryCore.buildOrganizationGroups = function(payload) {
  return MinistryCore.normalizeAgentImportList(payload || MinistryCore.officialAgentFleetSeed());
};

MinistryCore.buildOrganizationInventoryRows = function(payload) {
  var list = MinistryCore.normalizeAgentImportList(payload || MinistryCore.officialAgentFleetSeed());
  var rows = [];
  for (var i = 0; i < list.length; i++) {
    var agent = list[i];
    var cars = agent.cars || [];
    if (!cars.length) {
      rows.push({
        agentName: agent.name,
        kind: agent.kind,
        carNumber: "",
        dbSheet: agent.dbSheet
      });
      continue;
    }
    for (var c = 0; c < cars.length; c++) {
      rows.push({
        agentName: agent.name,
        kind: agent.kind,
        carNumber: cars[c].carNumber,
        dbSheet: agent.dbSheet
      });
    }
  }
  return rows;
};

MinistryCore.officialAgentFleetSeed = function() {
  return {
    agents: [
      {
        name: "سجاد صويره",
        kind: "معتمد",
        cars: ["15871", "16414", "16416"]
      },
      {
        name: "ابراهيم",
        kind: "معتمد",
        cars: ["31378", "22549", "36375", "32028"]
      },
      {
        name: "رواد ريادة",
        kind: "معتمد",
        cars: ["22351", "31669", "28123"]
      },
      {
        name: "قيصر وارد",
        kind: "معتمد",
        cars: ["29684", "33687", "29635", "36290"]
      },
      {
        name: "علي صبار",
        kind: "معتمد",
        cars: ["12207", "31896", "27591", "36341", "23589", "35837", "22006", "30706", "27912", "23917", "24189"]
      },
      {
        name: "قيصر شمري",
        kind: "معتمد",
        cars: ["20585", "27685", "20756", "20858"]
      },
      {
        name: "شركة",
        kind: "شركة",
        cars: ["22973", "24057", "24382", "25710", "30353", "29744", "29555", "13417", "27740"]
      }
    ]
  };
};

MinistryCore.normalizeAgentImportList = function(payload) {
  var raw = payload || {};
  var list = raw.agents || raw.data || [];
  if (!Array.isArray(list)) list = [];
  var out = [];
  for (var i = 0; i < list.length; i++) {
    var item = list[i] || {};
    var name = String(item.name || item.agentName || "").trim();
    if (!name) continue;
    var kind = MinistryCore.normalizeOwnerKind(item.kind || item.ownerKind) || MinistryCore.AGENT;
    var carsIn = item.cars || item.vehicles || [];
    var cars = [];
    for (var c = 0; c < carsIn.length; c++) {
      var car = carsIn[c];
      var carNumber = typeof car === "string" ? car : String((car && (car.carNumber || car.number)) || "").trim();
      if (!carNumber) continue;
      cars.push({
        carNumber: carNumber,
        defaultDriver: typeof car === "object" ? String(car.defaultDriver || car.driver || "") : ""
      });
    }
    out.push({ name: name, kind: kind, cars: cars, dbSheet: MinistryCore.agentDatabaseSheetName(name, kind) });
  }
  return out;
};

MinistryCore.agentDatabaseSheetName = function(agentName, ownerKind) {
  var name = String(agentName || "").trim();
  var kind = MinistryCore.normalizeOwnerKind(ownerKind);
  if (!name || name === MinistryCore.UNCLASSIFIED || !kind && name === MinistryCore.UNCLASSIFIED) {
    return MinistryCore.UNCLASSIFIED_DB;
  }
  if (kind === MinistryCore.COMPANY || name === MinistryCore.COMPANY) {
    return MinistryCore.COMPANY_DB;
  }
  var safe = MinistryCore.sanitizeSheetName(name.replace(/\s+/g, "_"));
  if (!safe || safe === "كشف") return MinistryCore.UNCLASSIFIED_DB;
  return MinistryCore.sanitizeSheetName(MinistryCore.AGENT_DB_PREFIX + safe);
};

MinistryCore.isAgentDatabaseSheet = function(name) {
  return /^DB_/.test(String(name || "").trim());
};

MinistryCore.receiptRoutingKey = function(row) {
  var r = row || {};
  return [
    String(r.docNumber || "").trim(),
    MinistryCore.normalizeCarNumber(r.carNumber || r.carNumberNormalized),
    String(r.loadDate || r.unloadDate || "").trim(),
    r.isFactory ? "factory" : "station"
  ].join("|");
};

MinistryCore.resolveRoutingTarget = function(receipt, fleetRows) {
  var row = receipt || {};
  var enriched = row.classified != null || row.unclassified != null
    ? row
    : MinistryCore.enrichReceipt(row, fleetRows);
  var sheetName = MinistryCore.agentDatabaseSheetName(enriched.agentName, enriched.ownerKind);
  return {
    sheetName: sheetName,
    agentName: enriched.agentName || MinistryCore.UNCLASSIFIED,
    ownerKind: enriched.ownerKind || "",
    classified: !!enriched.classified,
    unclassified: !enriched.classified,
    routingKey: MinistryCore.receiptRoutingKey(enriched)
  };
};

MinistryCore.agentLedgerHeaders = function() {
  return [
    "رقم الوصل",
    "السائق",
    "رقم السيارة",
    "تاريخ التحميل",
    "تاريخ التفريغ",
    "الكمية طن",
    "الوجهة",
    "نوع الحركة",
    "الشهر",
    "الفترة",
    "سعر الطن",
    "المبلغ",
    "لترات الكاز",
    "قيمة الكاز",
    "المعتمد",
    "وقت الترحيل",
    "مفتاح الترحيل"
  ];
};

MinistryCore.buildAgentLedgerRow = function(receipt, routedAt) {
  var row = receipt || {};
  var qty = MinistryCore.normalizeQtyTon(row.ministryQty != null ? row.ministryQty : row.quantity);
  var isFactory = !!row.isFactory;
  var price = row.ministryPricePerTon != null
    ? MinistryCore.round0(row.ministryPricePerTon)
    : MinistryCore.ministryPricePerTon(isFactory);
  var amount = row.ministryAmount != null
    ? MinistryCore.round0(row.ministryAmount)
    : MinistryCore.round0(qty * price);
  var liters = MinistryCore.normalizeLiters(row.liters);
  var gas = row.ministryGas != null ? MinistryCore.round0(row.ministryGas) : MinistryCore.gasAmount(liters);
  return [
    String(row.docNumber || ""),
    String(row.driverName || ""),
    String(row.carNumberNormalized || row.carNumber || ""),
    String(row.loadDate || ""),
    String(row.unloadDate || ""),
    qty,
    String(row.destination || row.station || row.factory || ""),
    isFactory ? "معمل" : "محطة",
    String(row.month || ""),
    MinistryCore.periodLabel(row.period15),
    price,
    amount,
    liters,
    gas,
    String(row.agentName || MinistryCore.UNCLASSIFIED),
    String(routedAt || ""),
    MinistryCore.receiptRoutingKey(row)
  ];
};

MinistryCore.sanitizeSheetName = function(name) {
  var s = String(name || "")
    .replace(/[:\\\/\?\*\[\]]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  if (!s) s = "كشف";
  return s.slice(0, 90);
};

MinistryCore.statementSheetName = function(prefix, monthKey, periodFilter, title) {
  var period = String(periodFilter == null ? "all" : periodFilter).trim() || "all";
  if (period === "1" || period === "first") period = "1";
  else if (period === "2" || period === "second") period = "2";
  else period = "all";
  return MinistryCore.sanitizeSheetName(
    String(prefix || "AGT10") + "_" + String(monthKey || "") + "_" + period + "_" + String(title || "")
  );
};

MinistryCore.isProtectedRegistrySheet = function(name) {
  var n = String(name || "").trim();
  return n === "Agents" || n === "Fleet" || n === MinistryCore.ORGANIZATION_SHEET || MinistryCore.isAgentDatabaseSheet(n);
};

MinistryCore.isGeneratedSupportSheet = function(name) {
  return /^(TPL_|STMT_|AGT10_|CO10_|MINP_|مح_|مع_)/.test(String(name || "").trim());
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = MinistryCore;
}
