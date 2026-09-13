#!/usr/bin/env node
"use strict";

/**
 * ملف تيست تيست — قائمة العملاء والسيارات كما أُرسلت.
 * المسار: tests/test-test.js
 * التشغيل: node tests/test-test.js
 */

var assert = require("assert");
var path = require("path");
var MinistryCore = require(path.join(__dirname, "..", "apps-script", "MinistryCore.gs"));

var CLIENTS = [
  {
    name: "سجاد صويره",
    kind: "معتمد",
    dbSheet: "DB_سجاد_صويره",
    cars: ["15871", "16414", "16416"]
  },
  {
    name: "ابراهيم",
    kind: "معتمد",
    dbSheet: "DB_ابراهيم",
    cars: ["31378", "22549", "36375", "32028"]
  },
  {
    name: "رواد ريادة",
    kind: "معتمد",
    dbSheet: "DB_رواد_ريادة",
    cars: ["22351", "31669", "28123"]
  },
  {
    name: "قيصر وارد",
    kind: "معتمد",
    dbSheet: "DB_قيصر_وارد",
    cars: ["29684", "33687", "29635", "36290"]
  },
  {
    name: "علي صبار",
    kind: "معتمد",
    dbSheet: "DB_علي_صبار",
    cars: ["12207", "31896", "27591", "36341", "23589", "35837", "22006", "30706", "27912", "23917", "24189"]
  },
  {
    name: "قيصر شمري",
    kind: "معتمد",
    dbSheet: "DB_قيصر_شمري",
    cars: ["20585", "27685", "20756", "20858"]
  },
  {
    name: "شركة",
    kind: "شركة",
    dbSheet: "DB_شركة",
    cars: ["22973", "24057", "24382", "25710", "30353", "29744", "29555", "13417", "27740"]
  }
];

var seed = MinistryCore.normalizeAgentImportList(MinistryCore.officialAgentFleetSeed());

assert.strictEqual(CLIENTS.length, 7);
assert.strictEqual(seed.length, 7);

var totalCars = 0;
console.log("========================================");
console.log("تيست تيست — العملاء والسيارات");
console.log("الملف: tests/test-test.js");
console.log("========================================");

for (var i = 0; i < CLIENTS.length; i++) {
  var client = CLIENTS[i];
  var official = seed[i];
  assert.strictEqual(official.name, client.name);
  assert.strictEqual(official.kind, client.kind);
  assert.strictEqual(official.dbSheet, client.dbSheet);
  assert.deepStrictEqual(
    official.cars.map(function(car) { return car.carNumber; }),
    client.cars
  );

  totalCars += client.cars.length;
  console.log("");
  console.log((i + 1) + ") العميل: " + client.name);
  console.log("   النوع: " + client.kind);
  console.log("   قاعدة البيانات: " + client.dbSheet);
  console.log("   السيارات (" + client.cars.length + "): " + client.cars.join(" ، "));
}

assert.strictEqual(totalCars, 38);
console.log("");
console.log("========================================");
console.log("النتيجة: 7 عملاء — 38 سيارة — مطابق للقائمة الرسمية");
console.log("========================================");
