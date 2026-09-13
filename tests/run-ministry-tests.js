#!/usr/bin/env node
"use strict";

var assert = require("assert");
var path = require("path");
var MinistryCore = require(path.join(__dirname, "..", "apps-script", "MinistryCore.gs"));

var failed = 0;
var passed = 0;

function test(name, fn) {
  try {
    fn();
    passed += 1;
    console.log("ok  - " + name);
  } catch (err) {
    failed += 1;
    console.error("fail - " + name);
    console.error("      " + (err && err.stack ? err.stack : err));
  }
}

test("18920 kg becomes 18.920 tons", function() {
  assert.strictEqual(MinistryCore.normalizeQtyTon(18920), 18.92);
  assert.strictEqual(MinistryCore.normalizeQtyTon("١٨٩٢٠"), 18.92);
  assert.strictEqual(MinistryCore.normalizeQtyTon(18.92), 18.92);
});

test("rejects date-like quantity values", function() {
  assert.strictEqual(MinistryCore.normalizeQtyTon(20260615), 0);
});

test("car numbers collapse to one key", function() {
  var a = MinistryCore.normalizeCarNumber("  ب  12345  ");
  var b = MinistryCore.normalizeCarNumber("ب-12345");
  var c = MinistryCore.normalizeCarNumber("ب١٢٣٤٥");
  assert.strictEqual(a, b);
  assert.strictEqual(b, c);
});

test("period15 splits mid-month and month-end", function() {
  assert.strictEqual(MinistryCore.getPeriod15("2026/09/01"), 1);
  assert.strictEqual(MinistryCore.getPeriod15("15/09/2026"), 1);
  assert.strictEqual(MinistryCore.getPeriod15("2026-09-16"), 2);
  assert.strictEqual(MinistryCore.getPeriod15("30/09/2026 14:00"), 2);
});

test("ministry amounts ignore 18% and old stored prices", function() {
  var station = MinistryCore.ministryAmount(18.92, false);
  var factory = MinistryCore.ministryAmount(18.92, true);
  assert.strictEqual(station, Math.round(18.92 * 34300));
  assert.strictEqual(factory, Math.round(18.92 * 8500));
  assert.notStrictEqual(station, Math.round((18.92 * 0.82) * 41800));
});

test("gas is liters x 430", function() {
  assert.strictEqual(MinistryCore.gasAmount(20), 8600);
});

test("fleet match classifies agent vs company vs unclassified", function() {
  var fleet = [
    { carNumber: "ب-11", ownerKind: "معتمد", agentName: "معتمد النور", active: 1 },
    { carNumber: "ك 22", ownerKind: "شركة", agentName: "", active: 1 }
  ];
  var agent = MinistryCore.classifyReceipt({ carNumber: "ب 11", destination: "حلفاية" }, fleet);
  var company = MinistryCore.classifyReceipt({ carNumber: "ك-22", destination: "معمل" }, fleet);
  var unknown = MinistryCore.classifyReceipt({ carNumber: "ز-99" }, fleet);
  assert.strictEqual(agent.classified, true);
  assert.strictEqual(agent.agentName, "معتمد النور");
  assert.strictEqual(company.ownerKind, "شركة");
  assert.strictEqual(company.agentName, "شركة");
  assert.strictEqual(unknown.unclassified, true);
  assert.strictEqual(unknown.agentName, "غير مصنف");
});

test("period stats split agents, company cars, gas, and net", function() {
  var fleet = [
    { carNumber: "A1", ownerKind: "معتمد", agentName: "الرافدين", active: 1 },
    { carNumber: "C1", ownerKind: "شركة", agentName: "شركة", active: 1 }
  ];
  var rows = [
    MinistryCore.enrichReceipt({
      docNumber: "100",
      carNumber: "A1",
      driverName: "أحمد",
      quantity: 10,
      liters: 10,
      destination: "حلفاية",
      unloadDate: "2026-09-03",
      sheetName: "2026_09"
    }, fleet),
    MinistryCore.enrichReceipt({
      docNumber: "101",
      carNumber: "C1",
      driverName: "سامي",
      quantity: 5,
      liters: 4,
      destination: "معمل بغداد",
      unloadDate: "2026-09-18",
      sheetName: "F_2026_09"
    }, fleet),
    MinistryCore.enrichReceipt({
      docNumber: "102",
      carNumber: "ZZ",
      quantity: 2,
      destination: "",
      unloadDate: "2026-09-04",
      sheetName: "2026_09"
    }, fleet)
  ];

  var first = MinistryCore.buildPeriodStats(MinistryCore.filterPeriod(rows, "1"));
  var second = MinistryCore.buildPeriodStats(MinistryCore.filterPeriod(rows, "2"));
  var all = MinistryCore.buildPeriodStats(rows);

  assert.strictEqual(first.totals.trips, 2);
  assert.strictEqual(first.agents[0].name, "الرافدين");
  assert.strictEqual(first.agents[0].totals.stationAmount, 10 * 34300);
  assert.strictEqual(first.unclassified.length, 1);
  assert.strictEqual(first.missingDestination.length, 1);
  assert.strictEqual(second.company.totals.factoryAmount, 5 * 8500);
  assert.strictEqual(second.company.cars[0].carNumber, "C1");
  assert.strictEqual(all.totals.gas, (10 * 430) + (4 * 430));
  assert.strictEqual(all.totals.net, (10 * 34300) + (5 * 8500) + (2 * 34300));
});

test("agent statements paginate 10 receipts and total each page", function() {
  var fleet = [{ carNumber: "A1", ownerKind: "معتمد", agentName: "دجلة", active: 1 }];
  var rows = [];
  for (var i = 1; i <= 23; i++) {
    rows.push(MinistryCore.enrichReceipt({
      docNumber: String(1000 + i),
      carNumber: "A1",
      driverName: "سائق",
      quantity: 1,
      destination: "حلفاية",
      loadDate: "2026-09-" + (i < 10 ? "0" + i : i > 30 ? "15" : String(i)),
      sheetName: "2026_09"
    }, fleet));
  }
  var statements = MinistryCore.buildAgentStatements(rows, 10);
  assert.strictEqual(statements.length, 1);
  assert.strictEqual(statements[0].pages.length, 3);
  assert.strictEqual(statements[0].pages[0].rows.length, 10);
  assert.strictEqual(statements[0].pages[0].totalQty, 10);
  assert.strictEqual(statements[0].pages[0].totalAmount, 10 * 34300);
  assert.strictEqual(statements[0].pages[2].rows.length, 3);
  assert.strictEqual(statements[0].pages[2].totalAmount, 3 * 34300);
  assert.strictEqual(statements[0].pages[0].rows[0].seq, 1);
  assert.ok(statements[0].pages[0].rows[0].docNumber);
  assert.ok(statements[0].pages[0].rows[0].pricePerTon);
});

test("company cars get a separate 10-receipt statement", function() {
  var fleet = [{ carNumber: "C9", ownerKind: "شركة", agentName: "شركة", active: 1 }];
  var rows = [
    MinistryCore.enrichReceipt({
      docNumber: "9",
      carNumber: "C9",
      quantity: 2,
      destination: "الرصافة",
      loadDate: "2026-09-02",
      sheetName: "2026_09"
    }, fleet)
  ];
  var company = MinistryCore.buildCompanyStatement(rows, 10);
  assert.strictEqual(company.name, "سيارات الشركة");
  assert.strictEqual(company.pages.length, 1);
  assert.strictEqual(company.totals.stationAmount, 2 * 34300);
});

test("organization covers June through current months and lists every car under its agent", function() {
  var months = MinistryCore.monthsFromInclusive(["2026_05", "2026_06", "F_2026_07", "2026_09", "2026_06"], "2026_06");
  assert.deepStrictEqual(months, ["2026_06", "2026_07", "2026_09"]);
  var inventory = MinistryCore.buildOrganizationInventoryRows();
  var cars = inventory.map(function(r) { return r.carNumber; });
  assert.ok(cars.indexOf("15871") >= 0);
  assert.ok(cars.indexOf("24189") >= 0);
  assert.ok(cars.indexOf("22973") >= 0);
  assert.strictEqual(inventory.filter(function(r) { return r.agentName === "علي صبار"; }).length, 11);
  assert.strictEqual(inventory.filter(function(r) { return r.dbSheet === "DB_شركة"; }).length, 9);
  assert.strictEqual(inventory.filter(function(r) { return r.agentName === "قيصر شمري"; }).length, 4);
});

test("سجاد صويره cars 15871 16414 16416 route to his database", function() {
  var seed = MinistryCore.normalizeAgentImportList(MinistryCore.officialAgentFleetSeed());
  assert.strictEqual(seed.length, 7);
  assert.strictEqual(seed[0].name, "سجاد صويره");
  assert.deepStrictEqual(seed[0].cars.map(function(c) { return c.carNumber; }), ["15871", "16414", "16416"]);
  assert.strictEqual(seed[0].dbSheet, "DB_سجاد_صويره");
  assert.strictEqual(seed[1].name, "ابراهيم");
  assert.deepStrictEqual(seed[1].cars.map(function(c) { return c.carNumber; }), ["31378", "22549", "36375", "32028"]);
  assert.strictEqual(seed[1].dbSheet, "DB_ابراهيم");
  assert.strictEqual(seed[2].name, "رواد ريادة");
  assert.deepStrictEqual(seed[2].cars.map(function(c) { return c.carNumber; }), ["22351", "31669", "28123"]);
  assert.strictEqual(seed[2].dbSheet, "DB_رواد_ريادة");
  assert.strictEqual(seed[3].name, "قيصر وارد");
  assert.deepStrictEqual(seed[3].cars.map(function(c) { return c.carNumber; }), ["29684", "33687", "29635", "36290"]);
  assert.strictEqual(seed[3].dbSheet, "DB_قيصر_وارد");
  assert.strictEqual(seed[4].name, "علي صبار");
  assert.deepStrictEqual(seed[4].cars.map(function(c) { return c.carNumber; }), ["12207", "31896", "27591", "36341", "23589", "35837", "22006", "30706", "27912", "23917", "24189"]);
  assert.strictEqual(seed[4].dbSheet, "DB_علي_صبار");
  assert.strictEqual(seed[5].name, "قيصر شمري");
  assert.deepStrictEqual(seed[5].cars.map(function(c) { return c.carNumber; }), ["20585", "27685", "20756", "20858"]);
  assert.strictEqual(seed[5].dbSheet, "DB_قيصر_شمري");
  assert.strictEqual(seed[6].name, "شركة");
  assert.strictEqual(seed[6].kind, "شركة");
  assert.deepStrictEqual(seed[6].cars.map(function(c) { return c.carNumber; }), ["22973", "24057", "24382", "25710", "30353", "29744", "29555", "13417", "27740"]);
  assert.strictEqual(seed[6].dbSheet, "DB_شركة");

  var fleet = seed[0].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "معتمد", agentName: "سجاد صويره", active: 1 };
  });
  ["15871", "16414", "16416", "15871 "].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "1",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, fleet);
    assert.strictEqual(target.sheetName, "DB_سجاد_صويره");
    assert.strictEqual(target.agentName, "سجاد صويره");
    assert.strictEqual(target.classified, true);
  });

  var ibrahimFleet = seed[1].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "معتمد", agentName: "ابراهيم", active: 1 };
  });
  ["31378", "22549", "36375", "32028"].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "2",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, ibrahimFleet);
    assert.strictEqual(target.sheetName, "DB_ابراهيم");
    assert.strictEqual(target.agentName, "ابراهيم");
  });

  var rawadFleet = seed[2].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "معتمد", agentName: "رواد ريادة", active: 1 };
  });
  ["22351", "31669", "28123"].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "3",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, rawadFleet);
    assert.strictEqual(target.sheetName, "DB_رواد_ريادة");
    assert.strictEqual(target.agentName, "رواد ريادة");
  });

  var qaysarFleet = seed[3].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "معتمد", agentName: "قيصر وارد", active: 1 };
  });
  ["29684", "33687", "29635", "36290"].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "4",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, qaysarFleet);
    assert.strictEqual(target.sheetName, "DB_قيصر_وارد");
    assert.strictEqual(target.agentName, "قيصر وارد");
  });

  var aliFleet = seed[4].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "معتمد", agentName: "علي صبار", active: 1 };
  });
  ["12207", "31896", "27591", "36341", "23589", "35837", "22006", "30706", "27912", "23917", "24189"].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "5",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, aliFleet);
    assert.strictEqual(target.sheetName, "DB_علي_صبار");
    assert.strictEqual(target.agentName, "علي صبار");
  });

  var shamriFleet = seed[5].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "معتمد", agentName: "قيصر شمري", active: 1 };
  });
  ["20585", "27685", "20756", "20858"].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "6",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, shamriFleet);
    assert.strictEqual(target.sheetName, "DB_قيصر_شمري");
    assert.strictEqual(target.agentName, "قيصر شمري");
  });

  var companyFleet = seed[6].cars.map(function(c) {
    return { carNumber: c.carNumber, ownerKind: "شركة", agentName: "شركة", active: 1 };
  });
  ["22973", "24057", "24382", "25710", "30353", "29744", "29555", "13417", "27740"].forEach(function(car) {
    var target = MinistryCore.resolveRoutingTarget({
      docNumber: "7",
      carNumber: car,
      loadDate: "2026-09-04",
      destination: "حلفاية",
      sheetName: "2026_09"
    }, companyFleet);
    assert.strictEqual(target.sheetName, "DB_شركة");
    assert.strictEqual(target.ownerKind, "شركة");
  });
});

test("agent import list prepares a database per agent and company cars", function() {
  var list = MinistryCore.normalizeAgentImportList({
    agents: [
      { name: "الرافدين", kind: "معتمد", cars: ["ب 11", { carNumber: "ب-12", driver: "أحمد" }] },
      { name: "شركة", kind: "شركة", cars: ["ك 22"] }
    ]
  });
  assert.strictEqual(list.length, 2);
  assert.strictEqual(list[0].dbSheet, "DB_الرافدين");
  assert.strictEqual(list[0].cars.length, 2);
  assert.strictEqual(list[1].dbSheet, "DB_شركة");
});

test("each agent and company gets a stable database sheet name", function() {
  assert.strictEqual(MinistryCore.agentDatabaseSheetName("معتمد النور", "معتمد"), "DB_معتمد_النور");
  assert.strictEqual(MinistryCore.agentDatabaseSheetName("شركة", "شركة"), "DB_شركة");
  assert.strictEqual(MinistryCore.agentDatabaseSheetName("", ""), "DB_غير_مصنف");
  assert.strictEqual(MinistryCore.agentDatabaseSheetName("غير مصنف", ""), "DB_غير_مصنف");
  assert.strictEqual(MinistryCore.isAgentDatabaseSheet("DB_معتمد_النور"), true);
  assert.strictEqual(MinistryCore.isProtectedRegistrySheet("DB_شركة"), true);
});

test("car number routes the receipt to that agent's database", function() {
  var fleet = [
    { carNumber: "ب 11", ownerKind: "معتمد", agentName: "الرافدين", active: 1 },
    { carNumber: "ك-22", ownerKind: "شركة", agentName: "شركة", active: 1 }
  ];
  var agent = MinistryCore.resolveRoutingTarget({
    docNumber: "500",
    carNumber: "ب-11",
    loadDate: "2026-09-04",
    destination: "حلفاية",
    sheetName: "2026_09"
  }, fleet);
  var company = MinistryCore.resolveRoutingTarget({
    docNumber: "501",
    carNumber: "ك 22",
    loadDate: "2026-09-04",
    destination: "معمل",
    sheetName: "F_2026_09"
  }, fleet);
  var unknown = MinistryCore.resolveRoutingTarget({
    docNumber: "502",
    carNumber: "ز99",
    loadDate: "2026-09-04",
    sheetName: "2026_09"
  }, fleet);

  assert.strictEqual(agent.sheetName, "DB_الرافدين");
  assert.strictEqual(agent.classified, true);
  assert.strictEqual(company.sheetName, "DB_شركة");
  assert.strictEqual(unknown.sheetName, "DB_غير_مصنف");
  assert.strictEqual(unknown.unclassified, true);
  assert.strictEqual(
    MinistryCore.receiptRoutingKey({ docNumber: "500", carNumber: "ب 11", loadDate: "2026-09-04" }),
    MinistryCore.receiptRoutingKey({ docNumber: "500", carNumber: "ب-11", loadDate: "2026-09-04" })
  );
  var ledger = MinistryCore.buildAgentLedgerRow(MinistryCore.enrichReceipt({
    docNumber: "500",
    carNumber: "ب-11",
    driverName: "أحمد",
    quantity: 18920,
    destination: "حلفاية",
    loadDate: "2026-09-04",
    sheetName: "2026_09",
    month: "2026_09"
  }, fleet), "2026-09-04 10:00:00");
  assert.strictEqual(ledger[5], 18.92);
  assert.strictEqual(ledger[11], Math.round(18.92 * 34300));
  assert.strictEqual(ledger[14], "الرافدين");
  assert.strictEqual(ledger[16], agent.routingKey);
});

test("ministry pack blocks unclassified receipts unless forced", function() {
  var stats = { unclassified: [{ docNumber: "1" }] };
  assert.strictEqual(MinistryCore.shouldBlockMinistryExport(stats, false), true);
  assert.strictEqual(MinistryCore.shouldBlockMinistryExport(stats, true), false);
  assert.strictEqual(MinistryCore.shouldBlockMinistryExport({ unclassified: [] }, false), false);
});

test("generated sheet names stay safe and template 60 is not a generated prefix", function() {
  var name = MinistryCore.statementSheetName("AGT10", "2026_09", "1", "معتمد/النور*");
  assert.ok(name.indexOf("AGT10_2026_09_1_") === 0);
  assert.ok(name.indexOf("*") < 0);
  assert.ok(name.indexOf("/") < 0);
  assert.strictEqual(MinistryCore.isGeneratedSupportSheet("60"), false);
  assert.strictEqual(MinistryCore.isProtectedRegistrySheet("Agents"), true);
  assert.strictEqual(MinistryCore.isProtectedRegistrySheet("Fleet"), true);
});

console.log("");
console.log(passed + " passed, " + failed + " failed");
process.exit(failed ? 1 : 0);
