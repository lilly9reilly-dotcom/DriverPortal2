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
