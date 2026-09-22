# -*- coding: utf-8 -*-
from pathlib import Path

p = Path(r"F:\DriverPortal2\temp-prod-live-pull\Admin.html")
t = p.read_text(encoding="utf-8")

if "previewAccountingFreshStart" not in t:
    mark = '<button class="btn-orange" onclick="createBackup()">نسخة احتياطية</button>'
    if mark not in t:
        raise SystemExit("createBackup button not found")
    add = (
        mark
        + "\n              "
        + '<button class="btn-blue" onclick="previewAccountingFreshStart()">معاينة تصفير محاسبي</button>'
        + "\n              "
        + '<button class="btn-red" onclick="applyAccountingFreshStart()">تصفير محاسبي (سبتمبر فارغ)</button>'
    )
    t = t.replace(mark, add, 1)
    print("buttons added")
else:
    print("buttons exist")

js = r'''
    async function previewAccountingFreshStart() {
      showLoader(true);
      try {
        var res = await runServer("resetAccountingFreshStart", { dryRun: "true" });
        if (!(res && res.success)) {
          showMessage((res && res.message) || "فشلت معاينة التصفير المحاسبي", "error");
          showLoader(false);
          return;
        }
        var archived = (res.archivedMonthSheets || []).join("، ") || "—";
        var dbs = (res.clearedAgentDatabases || []).join("، ") || "—";
        var lines = [
          "معاينة تصفير محاسبي (بدون تعديل):",
          "- أشهر ستُؤرشف (" + formatNum((res.archivedMonthSheets || []).length, 0) + "): " + archived,
          "- قواعد معتمدين ستُفرَّغ (" + formatNum((res.clearedAgentDatabases || []).length, 0) + "): " + dbs,
          "- أوراق مولَّدة ستُحذف: " + formatNum((res.deletedSupportSheets || []).length, 0),
          "- سيُعاد إنشاء: " + ((res.recreated || []).join(" + ") || "—"),
          "- يبقى: معتمدون / سيارات / GPS / التفعيل"
        ];
        showMessage(lines.join("\n"), "ok");
      } catch (e) {
        showMessage("فشلت معاينة التصفير: " + (e && e.message ? e.message : e), "error");
      }
      showLoader(false);
    }

    async function applyAccountingFreshStart() {
      var warn =
        "تصفير محاسبي كامل:\n" +
        "1) نسخة احتياطية تلقائية\n" +
        "2) أرشفة كل أوراق الأشهر (بما فيها سبتمبر)\n" +
        "3) تفريغ كل قواعد المعتمدين\n" +
        "4) إنشاء أوراق الشهر الحالي فارغة\n\n" +
        "المعتمدون والسيارات وGPS لن تُمس.\nهل تريد المتابعة؟";
      if (!confirm(warn)) return;
      var typed = prompt("للتأكيد اكتب بالضبط: RESET_ALL_CONFIRMED");
      if (String(typed || "").trim() !== "RESET_ALL_CONFIRMED") {
        showMessage("أُلغي التنفيذ — رمز التأكيد غير مطابق", "error");
        return;
      }
      showLoader(true);
      try {
        var res = await runServer("resetAccountingFreshStart", {
          dryRun: "false",
          confirmToken: "RESET_ALL_CONFIRMED"
        });
        if (!(res && res.success)) {
          showMessage((res && res.message) || "فشل التصفير المحاسبي", "error");
          showLoader(false);
          return;
        }
        if (res.backup && res.backup.backupUrl) {
          try { window.open(res.backup.backupUrl, "_blank"); } catch (ignore) {}
        }
        showMessage(
          (res.message || "تم التصفير") +
          "\nأشهر مؤرشفة: " + formatNum((res.archivedMonthSheets || []).length, 0) +
          "\nقواعد مفرَّغة: " + formatNum((res.clearedAgentDatabases || []).length, 0) +
          "\nأُعيد: " + ((res.recreated || []).join(" + ") || "—"),
          "ok"
        );
        if (typeof loadData === "function") await loadData();
      } catch (e) {
        showMessage("فشل التصفير المحاسبي: " + (e && e.message ? e.message : e), "error");
      }
      showLoader(false);
    }

'''

if "function previewAccountingFreshStart" not in t:
    # insert before createBackup function if present, else before </script> last
    anchor = "async function createBackup()"
    if anchor in t:
        t = t.replace(anchor, js + "\n    " + anchor, 1)
        print("js inserted before createBackup")
    else:
        t = t.replace("</script>", js + "\n  </script>", 1)
        print("js inserted before script end")
else:
    print("js exists")

p.write_text(t, encoding="utf-8")
print("admin done")
