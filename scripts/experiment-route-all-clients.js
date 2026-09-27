#!/usr/bin/env node
"use strict";

/**
 * تجربة توزيع الوصولات من النسخة الاحتياطية على قواعد المعتمدين.
 * التشغيل: node scripts/experiment-route-all-clients.js
 */
var fs = require("fs");
var path = require("path");
var MinistryCore = require("../apps-script/MinistryCore.gs");

var ROOT = path.join(__dirname, "..", "backups", "google-sheet-full-20260913", "tabs");
var OUT = "/opt/cursor/artifacts/routing-experiment";
var MONTHS = [
  "2026_06", "F_2026_06",
  "2026_07", "F_2026_07",
  "2026_08", "F_2026_08",
  "2026_09", "F_2026_09"
];

function parseCsv(text) {
  var rows = [];
  var i = 0;
  var field = "";
  var row = [];
  var inQuotes = false;
  text = String(text || "").replace(/^\uFEFF/, "");
  while (i < text.length) {
    var ch = text[i];
    if (inQuotes) {
      if (ch === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i += 2;
          continue;
        }
        inQuotes = false;
        i += 1;
        continue;
      }
      field += ch;
      i += 1;
      continue;
    }
    if (ch === '"') {
      inQuotes = true;
      i += 1;
      continue;
    }
    if (ch === ",") {
      row.push(field);
      field = "";
      i += 1;
      continue;
    }
    if (ch === "\n") {
      row.push(field);
      rows.push(row);
      row = [];
      field = "";
      i += 1;
      continue;
    }
    if (ch === "\r") {
      i += 1;
      continue;
    }
    field += ch;
    i += 1;
  }
  if (field.length || row.length) {
    row.push(field);
    rows.push(row);
  }
  if (!rows.length) return [];
  var headers = rows[0].map(function (h) {
    return String(h || "").trim();
  });
  var out = [];
  for (var r = 1; r < rows.length; r++) {
    var obj = {};
    var empty = true;
    for (var c = 0; c < headers.length; c++) {
      var v = rows[r][c] == null ? "" : String(rows[r][c]);
      obj[headers[c]] = v;
      if (v.trim()) empty = false;
    }
    if (!empty) out.push(obj);
  }
  return out;
}

function pick(row, names) {
  for (var i = 0; i < names.length; i++) {
    if (row[names[i]] != null && String(row[names[i]]).trim() !== "") return row[names[i]];
  }
  var keys = Object.keys(row);
  for (var n = 0; n < names.length; n++) {
    var want = names[n].replace(/\s+/g, "");
    for (var k = 0; k < keys.length; k++) {
      if (keys[k].replace(/\s+/g, "") === want && String(row[keys[k]]).trim() !== "") {
        return row[keys[k]];
      }
    }
  }
  return "";
}

function toReceipt(row, sheetName) {
  var factory = /^F_/i.test(sheetName);
  return {
    docNumber: pick(row, ["رقم الوصل"]),
    driverName: pick(row, ["اسم السائق", "السائق"]),
    carNumber: pick(row, ["رقم السيارة"]),
    loadDate: pick(row, ["تاريخ التحميل"]),
    unloadDate: pick(row, ["تاريخ التفريغ"]),
    quantity: pick(row, ["الكمية"]),
    destination: factory
      ? pick(row, ["المحطة/المعمل", "المحطه/المعمل", "المحطة", "الوجهة"])
      : pick(row, ["المحطة", "المحطه", "الوجهة"]),
    station: pick(row, ["المحطة", "المحطه"]),
    factory: factory ? pick(row, ["المحطة/المعمل", "المحطه/المعمل", "المحطة"]) : "",
    liters: factory ? 0 : pick(row, ["لترات الكاز"]),
    owner: pick(row, ["المالك", "مالك السيارة أو المالك"]),
    source: factory ? "factory" : "station",
    sheetName: sheetName,
    month: sheetName.replace(/^F_/, "")
  };
}

function main() {
  fs.mkdirSync(OUT, { recursive: true });
  var seed = MinistryCore.normalizeAgentImportList(MinistryCore.officialAgentFleetSeed());
  var fleet = [];
  seed.forEach(function (a) {
    (a.cars || []).forEach(function (c) {
      fleet.push({
        carNumber: c.carNumber,
        ownerKind: a.kind,
        agentName: a.name,
        active: 1
      });
    });
  });

  var byDb = {};
  var seen = {};
  var total = 0;
  var routed = 0;
  var skippedDup = 0;
  var unclassified = 0;
  var perAgent = {};

  MONTHS.forEach(function (sheet) {
    var file = path.join(ROOT, sheet + ".csv");
    if (!fs.existsSync(file)) return;
    parseCsv(fs.readFileSync(file, "utf8")).forEach(function (raw) {
      var receipt = toReceipt(raw, sheet);
      if (!String(receipt.docNumber || "").trim() && !String(receipt.carNumber || "").trim()) return;
      total += 1;
      var enriched = MinistryCore.enrichReceipt(receipt, fleet);
      var target = MinistryCore.resolveRoutingTarget(enriched, fleet);
      if (seen[target.routingKey]) {
        skippedDup += 1;
        return;
      }
      seen[target.routingKey] = true;
      routed += 1;
      if (target.unclassified) unclassified += 1;
      if (!byDb[target.sheetName]) byDb[target.sheetName] = [];
      byDb[target.sheetName].push(MinistryCore.buildAgentLedgerRow(enriched, "2026-09-14Texperiment"));
      var agent = target.agentName || "غير مصنف";
      if (!perAgent[agent]) perAgent[agent] = { trips: 0, qty: 0, amount: 0, gas: 0 };
      perAgent[agent].trips += 1;
      perAgent[agent].qty += Number(enriched.ministryQty || 0);
      perAgent[agent].amount += Number(enriched.ministryAmount || 0);
      perAgent[agent].gas += Number(enriched.ministryGas || 0);
    });
  });

  var headers = MinistryCore.agentLedgerHeaders();
  Object.keys(byDb).forEach(function (db) {
    var lines = [headers.join("\t")];
    byDb[db].forEach(function (r) {
      lines.push(
        r
          .map(function (c) {
            return String(c == null ? "" : c).replace(/[\t\n\r]+/g, " ");
          })
          .join("\t")
      );
    });
    fs.writeFileSync(path.join(OUT, db + ".tsv"), lines.join("\n"), "utf8");
  });

  var md = [];
  md.push("# نتيجة تجربة التوزيع على كل المعتمدين");
  md.push("");
  md.push("المصدر: النسخة الاحتياطية للشيت الحي (يونيو → سبتمبر 2026)");
  md.push("");
  md.push("| المقياس | القيمة |");
  md.push("|---|---|");
  md.push("| وصولات ممسوحة | " + total + " |");
  md.push("| فريدة موزّعة | " + routed + " |");
  md.push("| مكررة متجاهلة | " + skippedDup + " |");
  md.push("| غير مصنّفة | " + unclassified + " |");
  md.push("");
  md.push("## لكل معتمد");
  md.push("");
  md.push("| المعتمد | القاعدة | الرحلات | الكمية طن | المبلغ | الكاز |");
  md.push("|---|---|---:|---:|---:|---:|");
  seed.forEach(function (a) {
    var v = perAgent[a.name] || { trips: 0, qty: 0, amount: 0, gas: 0 };
    md.push(
      "| " +
        a.name +
        " | " +
        a.dbSheet +
        " | " +
        (v.trips || 0) +
        " | " +
        Math.round((v.qty || 0) * 1000) / 1000 +
        " | " +
        (v.amount || 0) +
        " | " +
        (v.gas || 0) +
        " |"
    );
  });
  var u = perAgent["غير مصنف"] || { trips: 0, qty: 0, amount: 0, gas: 0 };
  md.push(
    "| غير مصنف | DB_غير_مصنف | " +
      u.trips +
      " | " +
      Math.round((u.qty || 0) * 1000) / 1000 +
      " | " +
      u.amount +
      " | " +
      u.gas +
      " |"
  );
  fs.writeFileSync(path.join(OUT, "report.md"), md.join("\n"), "utf8");
  console.log(md.join("\n"));
}

main();
