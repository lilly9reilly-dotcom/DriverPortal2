var SPREADSHEET_ID = "1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM";
var PRICE_PER_TON_HALAFAYA = 41800;
var PRICE_PER_TON_FACTORY = 10000;
var PRICE_PER_TON = PRICE_PER_TON_HALAFAYA;
var PRICE_PER_LITER = 430;
var DEDUCTION_RATE = 0.18;
var DRIVE_FOLDER_ID = "";
var OFFICIAL_TEMPLATE_SHEET_NAME = "60";

function doGet(e) {
  try {
    var page = e && e.parameter ? String(e.parameter.page || "").trim().toLowerCase() : "";
    if (page === "admin") {
      return renderAdminPage_();
    }
    if (page === "demand_template") {
      return renderDemandTemplateToolPage_();
    }
  } catch (err) {
  }

  return handleRequest(e);
}

function doPost(e) {
  return handleRequest(e);
}

function renderAdminPage_() {
  try {
    return HtmlService
      .createTemplateFromFile("Admin")
      .evaluate()
      .setTitle("لوحة المدير العام")
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
  } catch (err) {
    return HtmlService.createHtmlOutput(
      "<html dir='rtl'><body style='font-family:Tajawal,sans-serif;padding:20px'>" +
      "<h2>تعذر فتح صفحة الأدمن</h2>" +
      "<p>" + sanitizeHtml_(String(err)) + "</p>" +
      "</body></html>"
    );
  }
}

function renderProfessionalAdminDashboard_() {
  var nowMonth = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy_MM");
  var today = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy/M/d");

  var html = "" +
    "<html dir='rtl'><head><meta charset='utf-8'>" +
    "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
    "<title>لوحة المدير العام</title>" +
    "<style>" +
    "@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;800&display=swap');" +
    "body{margin:0;font-family:Cairo,Tahoma,sans-serif;color:#102a43;background:radial-gradient(circle at 15% 20%,#dff3ff 0,#f6fbff 35%,#eef6ff 65%,#e5f0ff 100%)}" +
    ".shell{max-width:1280px;margin:0 auto;padding:20px}" +
    ".hero{background:linear-gradient(120deg,#0b3c5d,#1e6f8f);color:#fff;border-radius:16px;padding:18px 20px;box-shadow:0 10px 24px rgba(11,60,93,.25)}" +
    ".hero h1{margin:0;font-size:27px;font-weight:800}" +
    ".hero p{margin:6px 0 0;font-size:14px;opacity:.9}" +
    ".grid{display:grid;grid-template-columns:1.2fr .8fr;gap:14px;margin-top:14px}" +
    ".panel{background:#fff;border:1px solid #d9e6f2;border-radius:14px;padding:14px;box-shadow:0 6px 16px rgba(16,42,67,.08)}" +
    ".filters{display:grid;grid-template-columns:1fr 1fr 1fr;gap:10px}" +
    "label{display:block;font-size:12px;color:#486581;margin-bottom:4px;font-weight:700}" +
    "input,select{width:100%;padding:9px;border:1px solid #b7c9da;border-radius:8px;box-sizing:border-box;background:#fff}" +
    ".actions{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}" +
    "button{border:0;border-radius:9px;padding:10px 12px;cursor:pointer;font-weight:700}" +
    ".btn-primary{background:#0b7285;color:#fff}" +
    ".btn-dark{background:#1f2933;color:#fff}" +
    ".btn-light{background:#e6edf5;color:#123}" +
    ".cards{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-top:12px}" +
    ".card{background:#f8fbff;border:1px solid #d2e3f3;border-radius:12px;padding:10px}" +
    ".card .k{font-size:12px;color:#627d98;font-weight:700}" +
    ".card .v{font-size:24px;color:#102a43;font-weight:800;margin-top:2px}" +
    "table{width:100%;border-collapse:collapse;margin-top:10px;background:#fff}" +
    "th,td{border:1px solid #d9e2ec;padding:8px;text-align:center;font-size:13px}" +
    "th{background:#f0f4f8;font-weight:800}" +
    "#result{margin-top:8px;padding:10px;background:#f8fbff;border:1px solid #d9e6f2;border-radius:10px;white-space:pre-wrap;min-height:42px}" +
    "@media (max-width:1024px){.grid{grid-template-columns:1fr}.cards{grid-template-columns:repeat(2,minmax(0,1fr))}.filters{grid-template-columns:1fr 1fr}}" +
    "@media (max-width:700px){.cards{grid-template-columns:1fr}.filters{grid-template-columns:1fr}}" +
    "</style></head><body>" +
    "<div class='shell'>" +
    "<div class='hero'><h1>لوحة المدير العام</h1><p>إدارة القوالب والتقارير والحسابات من مكان واحد</p></div>" +
    "<div class='grid'>" +
    "<div class='panel'>" +
    "<div class='filters'>" +
    "<div><label>الشهر</label><select id='month'></select></div>" +
    "<div><label>النصف</label><select id='half'><option value='all'>كل الشهر</option><option value='first'>النصف الأول</option><option value='second'>النصف الثاني</option></select></div>" +
    "<div><label>رقم المطالبة</label><input id='demandNo' value='61'></div>" +
    "</div>" +
    "<div class='actions'>" +
    "<button class='btn-primary' onclick='refreshSummary()'>تحديث الداشبورد</button>" +
    "<button class='btn-dark' onclick='buildTemplate()'>إنشاء قالب المطالبة</button>" +
    "<button class='btn-light' onclick='openTemplatePage()'>فتح صفحة القالب</button>" +
    "</div>" +
    "<div class='cards'>" +
    "<div class='card'><div class='k'>عدد النقلات</div><div class='v' id='mTrips'>0</div></div>" +
    "<div class='card'><div class='k'>الكمية الكلية</div><div class='v' id='mGross'>0</div></div>" +
    "<div class='card'><div class='k'>الكمية المحتسبة</div><div class='v' id='mNet'>0</div></div>" +
    "<div class='card'><div class='k'>المبلغ المحسوب</div><div class='v' id='mAmount'>0</div></div>" +
    "<div class='card'><div class='k'>كلفة الغاز</div><div class='v' id='mGas'>0</div></div>" +
    "<div class='card'><div class='k'>الصافي بعد الغاز</div><div class='v' id='mAfterGas'>0</div></div>" +
    "</div>" +
    "<table><thead><tr><th>السائق</th><th>عدد النقلات</th><th>الكمية المحتسبة</th><th>المبلغ</th></tr></thead><tbody id='driversBody'><tr><td colspan='4'>لا توجد بيانات</td></tr></tbody></table>" +
    "</div>" +
    "<div class='panel'>" +
    "<label>تاريخ المطالبة</label><input id='reportDate' value='" + today + "'>" +
    "<label style='margin-top:8px'>الجهة المجهزة</label><input id='route' value='محور - حلفاية - بغداد - تاجي'>" +
    "<label style='margin-top:8px'>الجهة المستلمة</label><input id='receiver' value='شركة تعبئة الغاز'>" +
    "<label style='margin-top:8px'>المنتوج</label><input id='product' value='غاز سائل'>" +
    "<div id='result'>جاهز</div>" +
    "</div>" +
    "</div></div>" +
    "<script>" +
    "const money=v=>new Intl.NumberFormat('en-US').format(Number(v||0));" +
    "const qty=v=>new Intl.NumberFormat('en-US',{maximumFractionDigits:3}).format(Number(v||0));" +
    "const base=location.origin+location.pathname;" +
    "async function api(params){const q=new URLSearchParams(params);const r=await fetch(base+'?'+q.toString());return await r.json();}" +
    "function selectedMonth(){return document.getElementById('month').value||'';}" +
    "function selectedHalf(){return document.getElementById('half').value||'all';}" +
    "async function loadMonths(){" +
    "const sel=document.getElementById('month');" +
    "sel.innerHTML='<option>جاري التحميل...</option>';" +
    "try{" +
    "const res=await api({action:'getAvailableMonths'});" +
    "const list=(res&&res.data)||[];" +
    "sel.innerHTML='';" +
    "if(!list.length){const o=document.createElement('option');o.value='" + nowMonth + "';o.textContent='" + nowMonth + "';sel.appendChild(o);return;}" +
    "list.forEach((m,i)=>{const o=document.createElement('option');o.value=m;o.textContent=m; if(i===0)o.selected=true; sel.appendChild(o);});" +
    "}catch(e){sel.innerHTML='<option value=\"\">خطأ بالتحميل</option>';document.getElementById('result').textContent='خطأ تحميل الأشهر: '+e;}" +
    "}" +
    "function renderSummary(s){" +
    "document.getElementById('mTrips').textContent=money(s.trips);" +
    "document.getElementById('mGross').textContent=qty(s.grossQuantity);" +
    "document.getElementById('mNet').textContent=qty(s.netQuantity);" +
    "document.getElementById('mAmount').textContent=money(s.totalAmount);" +
    "document.getElementById('mGas').textContent=money(s.gasCost);" +
    "document.getElementById('mAfterGas').textContent=money(s.netAfterGas);" +
    "const body=document.getElementById('driversBody');" +
    "const top=(s.topDrivers||[]);" +
    "if(!top.length){body.innerHTML='<tr><td colspan=\"4\">لا توجد بيانات</td></tr>';return;}" +
    "body.innerHTML=top.map(d=>'<tr><td>'+d.driverName+'</td><td>'+money(d.trips)+'</td><td>'+qty(d.netQuantity)+'</td><td>'+money(d.amount)+'</td></tr>').join('');" +
    "}" +
    "async function refreshSummary(){" +
    "document.getElementById('result').textContent='جاري تحديث الداشبورد...';" +
    "try{" +
    "const m=selectedMonth();" +
    "if(!m){document.getElementById('result').textContent='اختر شهرًا أولاً';return;}" +
    "const res=await api({action:'getDashboardSummary',month:m,half:selectedHalf()});" +
    "if(!res||!res.success){document.getElementById('result').textContent=(res&&res.message)||'تعذر تحميل الملخص';return;}" +
    "renderSummary(res);" +
    "document.getElementById('result').textContent='تم التحديث بنجاح';" +
    "}catch(e){document.getElementById('result').textContent='خطأ: '+e;}" +
    "}" +
    "async function buildTemplate(){" +
    "document.getElementById('result').textContent='جاري إنشاء القالب...';" +
    "try{" +
    "const m=selectedMonth();" +
    "if(!m){document.getElementById('result').textContent='اختر شهرًا أولاً';return;}" +
    "const res=await api({action:'buildDemandTemplateSheet',month:m,half:selectedHalf(),demandNo:document.getElementById('demandNo').value,reportDate:document.getElementById('reportDate').value,route:document.getElementById('route').value,receiver:document.getElementById('receiver').value,product:document.getElementById('product').value});" +
    "const msg=(res&&res.message)||'تم التنفيذ';" +
    "document.getElementById('result').textContent=msg+(res&&res.sheetUrl?'\\n'+res.sheetUrl:'');" +
    "if(res&&res.sheetUrl){window.open(res.sheetUrl,'_blank');}" +
    "}catch(e){document.getElementById('result').textContent='خطأ: '+e;}" +
    "}" +
    "function openTemplatePage(){window.open(base+'?page=demand_template','_blank');}" +
    "(async function init(){await loadMonths(); await refreshSummary();})();" +
    "</script></body></html>";

  return HtmlService
    .createHtmlOutput(html)
    .setTitle("لوحة المدير العام")
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

function renderDemandTemplateToolPage_() {
  var html = "" +
    "<html dir='rtl'><head><meta charset='utf-8'>" +
    "<title>مولد قالب المطالبة</title>" +
    "<style>" +
    "body{font-family:Tajawal,Arial,sans-serif;background:#f3f6fb;padding:24px;color:#1f2937}" +
    ".box{max-width:760px;margin:auto;background:#fff;border:1px solid #dbe2ea;border-radius:14px;padding:20px}" +
    "h2{margin:0 0 16px 0}" +
    ".grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}" +
    "label{font-size:13px;color:#475569;display:block;margin-bottom:4px}" +
    "input,select{width:100%;padding:10px;border:1px solid #cbd5e1;border-radius:8px;box-sizing:border-box}" +
    "button{margin-top:14px;background:#0f766e;color:#fff;border:0;padding:10px 14px;border-radius:8px;cursor:pointer}" +
    "#result{margin-top:12px;white-space:pre-wrap;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:10px}" +
    "</style></head><body>" +
    "<div class='box'>" +
    "<h2>توليد قالب المطالبة</h2>" +
    "<div class='grid'>" +
    "<div><label>الشهر (yyyy_MM)</label><input id='month' value='" + Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy_MM") + "'></div>" +
    "<div><label>النصف</label><select id='half'><option value='all'>كل الشهر</option><option value='first'>1 - 15</option><option value='second'>16 - نهاية الشهر</option></select></div>" +
    "<div><label>رقم المطالبة</label><input id='demandNo' value='61'></div>" +
    "<div><label>تاريخ المطالبة</label><input id='reportDate' value='" + Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy/M/d") + "'></div>" +
    "<div><label>الجهة المجهزة</label><input id='route' value='محور - حلفاية - بغداد - تاجي'></div>" +
    "<div><label>الجهة المستلمة</label><input id='receiver' value='شركة تعبئة الغاز'></div>" +
    "</div>" +
    "<div><label>المنتوج</label><input id='product' value='غاز سائل'></div>" +
    "<button onclick='buildTemplate()'>إنشاء القالب الآن</button>" +
    "<div id='result'>جاهز للاختبار...</div>" +
    "</div>" +
    "<script>" +
    "async function buildTemplate(){" +
    "var base=location.origin+location.pathname;" +
    "var p=new URLSearchParams();" +
    "p.set('action','buildDemandTemplateSheet');" +
    "p.set('month',document.getElementById('month').value);" +
    "p.set('half',document.getElementById('half').value);" +
    "p.set('demandNo',document.getElementById('demandNo').value);" +
    "p.set('reportDate',document.getElementById('reportDate').value);" +
    "p.set('route',document.getElementById('route').value);" +
    "p.set('receiver',document.getElementById('receiver').value);" +
    "p.set('product',document.getElementById('product').value);" +
    "document.getElementById('result').textContent='جاري الإنشاء...';" +
    "try{" +
    "var r=await fetch(base+'?'+p.toString());" +
    "var j=await r.json();" +
    "var txt=JSON.stringify(j,null,2);" +
    "if(j&&j.sheetUrl){txt+='\\n\\nرابط الورقة: '+j.sheetUrl;}" +
    "document.getElementById('result').textContent=txt;" +
    "if(j&&j.sheetUrl){window.open(j.sheetUrl,'_blank');}" +
    "}catch(e){document.getElementById('result').textContent='خطأ: '+e;}" +
    "}" +
    "</script></body></html>";

  return HtmlService
    .createHtmlOutput(html)
    .setTitle("مولد قالب المطالبة")
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

function include(name) {
  return HtmlService.createHtmlOutputFromFile(name).getContent();
}

function handleRequest(e) {
  var action = "";
  var data = {};
  var page = "";

  try {
    if (e && e.parameter) {
      data = e.parameter || {};
      action = String(data.action || "").trim();
      page = String(data.page || "").trim().toLowerCase();
    }

    if (e && e.postData && e.postData.contents) {
      var content = String(e.postData.contents || "");

      try {
        if (content && content.trim().charAt(0) === "{") {
          var postData = JSON.parse(content);
          if (postData) {
            data = postData;
            action = String(postData.action || action || "").trim();
          }
        } else if (e.parameter) {
          data = e.parameter;
          action = String(data.action || action || "").trim();
        }
      } catch (err) {
      }
    }

    if (!action && page === "admin") {
      return renderAdminPage_();
    }

    if (!action) {
      return json({ success: false, message: "No action" });
    }

    if (action.indexOf("gas_mvp_") === 0) {
      if (typeof gasMvpDispatch === "function") {
        return gasMvpDispatch(e);
      }
      return json({
        success: false,
        message: "gasMvpDispatch is not defined"
      });
    }

    if (action === "getAvailableMonths") {
      return json({ success: true, data: getAvailableMonths() });
    }

    if (action === "getAllReceiptsData") {
      return json(getAllReceiptsData(data.month || ""));
    }

    if (action === "getDashboardSummary") {
      return json(getDashboardSummary(data));
    }

    if (action === "exportDemandTemplate") {
      return json(exportDemandTemplate(data));
    }

    if (action === "buildDemandTemplateSheet") {
      return json(buildDemandTemplateSheet(data));
    }

    if (action === "exportOfficialTemplateXlsx") {
      return json(exportOfficialTemplateXlsx(data));
    }

    if (action === "buildMonthlyStatementsSheets") {
      return json(buildMonthlyStatementsSheets(data));
    }

    if (action === "cleanupGeneratedSheets") {
      return json(cleanupGeneratedSheets(data));
    }

    if (action === "buildOfficialExactTemplate") {
      return json(buildOfficialExactTemplate(data));
    }

    if (action === "getMaintenanceData") {
      return json(getMaintenanceData(data.month || ""));
    }

    if (action === "schemaAudit") {
      return json(schemaAudit(data));
    }

    if (action === "schemaFix") {
      return json(schemaFix(data));
    }

    if (action === "deleteReceiptRow") {
      return json(deleteReceiptRow(data.row || "", data.month || "", data.sheetName || ""));
    }

    if (action === "login") {
      return json(callExisting_("loginDriver", [data]));
    }

    if (action === "gps") {
      return json(callExisting_("handleGPS", [data]));
    }

    if (action === "trip") {
      return json(callExisting_("saveTrip", [data]));
    }

    if (action === "factory") {
      return json(callExisting_("saveFactory", [data]));
    }

    if (action === "history") {
      return json(callExisting_("getDriverTrips", [data]));
    }

    if (action === "wallet" || action === "dashboard") {
      return json(callExisting_("getDriverWallet", [data]));
    }

    if (action === "saveMaintenance") {
      return json(callExisting_("saveMaintenance", [data]));
    }

    if (action === "getMaintenance") {
      return json(callExisting_("getMaintenanceRequests", [data]));
    }

    if (action === "checkDoc") {
      return json(callExisting_("checkDoc", [data]));
    }

    if (action === "reportIssue") {
      return json(callExisting_("reportIssue", [data]));
    }

    if (action === "drivers") {
      return json(callExisting_("getDriversLive", []));
    }

    if (action === "autoTrips") {
      return json(callExisting_("getAutoTrips", []));
    }

    if (action === "route") {
      return json(callExisting_("getVehicleRoute", [{
        vehicle: data.vehicle || "",
        carNumber: data.carNumber || "",
        driverName: data.driverName || ""
      }]));
    }

    if (action === "alerts") {
      return json(callExisting_("getRecentAlerts", []));
    }

    return json({ success: false, message: "Unknown action: " + action });
  } catch (err) {
    return json({
      success: false,
      message: String(err),
      action: action
    });
  }
}

function callExisting_(name, args) {
  if (typeof this[name] === "function") {
    return this[name].apply(this, args || []);
  }
  return {
    success: false,
    message: name + " غير موجودة في المشروع"
  };
}

function saveTrip(data) {
  if (!data) {
    return { success: false, message: "No data" };
  }

  var rawDocNumber = String(data.docNumber || "").trim();
  var normalizedDocNumber = normalizeDocNumber_(rawDocNumber);
  if (!normalizedDocNumber) {
    return { success: false, message: "رقم الوصل مطلوب" };
  }

  var monthPolicy = resolveSubmissionMonthPolicy_(data);
  if (!monthPolicy.success) {
    return monthPolicy;
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheetName = monthPolicy.monthKey;
  var sheet = ensureTripsSheet_(ss, sheetName);

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(15000);

    var duplicate = findExistingDocNumber_(ss, normalizedDocNumber);
    if (duplicate.found) {
      return {
        success: false,
        exists: true,
        isExists: true,
        message: "رقم الوصل مكرر وموجود مسبقًا",
        docNumber: rawDocNumber,
        existingSheet: duplicate.sheetName,
        existingRow: duplicate.rowNumber
      };
    }

    var imageUrl = "";
    try {
      if (data.fileData && data.fileData !== "") {
        imageUrl = processFile(data.fileData, (rawDocNumber || "trip") + ".jpg");
      }
    } catch (e) {
      imageUrl = "";
    }

    var sendTime = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

    sheet.appendRow([
      rawDocNumber,
      data.driverName || "",
      data.carNumber || "",
      data.loadDate || "",
      data.unloadDate || "",
      data.quantity || 0,
      data.owner || data.ownerType || "",
      data.station || data.destination || "",
      imageUrl,
      sendTime,
      data.liters || 0,
      data.bojer || data.bogerNumber || "",
      data.distance || 0,
      data.tripPrice || data.price || 0,
      data.notes || ""
    ]);

    return {
      success: true,
      exists: false,
      isExists: false,
      message: "تم حفظ النقلة بنجاح",
      sheetName: sheetName,
      tripMonth: monthPolicy.monthKey,
      sendTime: sendTime
    };
  } finally {
    try {
      lock.releaseLock();
    } catch (e) {
    }
  }
}

function saveFactory(data) {
  if (!data) {
    return { success: false, message: "No data" };
  }

  var rawDocNumber = String(data.docNumber || "").trim();
  var normalizedDocNumber = normalizeDocNumber_(rawDocNumber);
  if (!normalizedDocNumber) {
    return { success: false, message: "رقم الوصل مطلوب" };
  }

  var monthPolicy = resolveSubmissionMonthPolicy_(data);
  if (!monthPolicy.success) {
    return monthPolicy;
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheetName = "F_" + monthPolicy.monthKey;
  var sheet = ensureFactorySheet_(ss, sheetName);

  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(15000);

    var duplicate = findExistingDocNumber_(ss, normalizedDocNumber);
    if (duplicate.found) {
      return {
        success: false,
        exists: true,
        isExists: true,
        message: "رقم الوصل مكرر وموجود مسبقًا",
        docNumber: rawDocNumber,
        existingSheet: duplicate.sheetName,
        existingRow: duplicate.rowNumber
      };
    }

    var imageUrl = "";
    try {
      if (data.fileData && data.fileData !== "") {
        imageUrl = processFile(data.fileData, (rawDocNumber || "factory") + ".jpg");
      }
    } catch (e) {
      imageUrl = "";
    }

    var sendTime = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

    sheet.appendRow([
      rawDocNumber,
      data.driverName || "",
      data.carNumber || "",
      data.quantity || 0,
      data.factory || data.destination || "",
      imageUrl,
      data.unloadDate || data.loadDate || "",
      data.notes || "",
      "factory",
      sendTime
    ]);

    return {
      success: true,
      exists: false,
      isExists: false,
      message: "تم حفظ وصلة المعمل بنجاح",
      sheetName: sheetName,
      tripMonth: monthPolicy.monthKey,
      sendTime: sendTime
    };
  } finally {
    try {
      lock.releaseLock();
    } catch (e) {
    }
  }
}

function findExistingDocNumber_(ss, normalizedDocNumber) {
  var sheets = ss.getSheets();
  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = String(sheet.getName() || "");
    if (!extractMonthKeyFromSheetName_(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (!values || values.length === 0) continue;

    var header = values[0] || [];
    var hasHeader = isLikelyHeaderRow_(header);
    var colMap = hasHeader ? buildColumnMap_(header) : {};
    var startRow = hasHeader ? 1 : 0;

    for (var i = startRow; i < values.length; i++) {
      var row = values[i];
      if (!row || row.length === 0) continue;

      var candidateDoc = String(getCellByAliases_(row, colMap, ["docnumber", "doc", "document", "receipt", "رقمالوصل"], 0) || "");
      if (!candidateDoc) continue;

      if (normalizeDocNumber_(candidateDoc) === normalizedDocNumber) {
        return {
          found: true,
          sheetName: sheetName,
          rowNumber: i + 1
        };
      }
    }
  }

  return {
    found: false,
    sheetName: "",
    rowNumber: 0
  };
}

function normalizeDocNumber_(value) {
  return String(value || "")
    .replace(/[٠-٩]/g, function(d) {
      return "٠١٢٣٤٥٦٧٨٩".indexOf(d);
    })
    .replace(/\s+/g, "")
    .trim()
    .toLowerCase();
}

function getMaintenanceRequests(params) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = ss.getSheetByName("سجل الصيانة");

  if (!sheet) {
    return { success: true, requests: [] };
  }

  var carNumber = "";
  if (params && params.carNumber) {
    carNumber = String(params.carNumber).trim();
  }

  var dataSheet = sheet.getDataRange().getValues();
  var requests = [];

  for (var i = 1; i < dataSheet.length; i++) {
    var vehicle = String(dataSheet[i][2] || "").trim();

    if (carNumber && vehicle !== carNumber) {
      continue;
    }

    requests.push({
      requestId: dataSheet[i][0],
      driver: dataSheet[i][1],
      vehicle: vehicle,
      problem: dataSheet[i][3],
      status: dataSheet[i][4],
      requestDate: dataSheet[i][5],
      repairDate: dataSheet[i][6] || "",
      type: dataSheet[i][7] || "",
      cost: String(dataSheet[i][8] || ""),
      location: dataSheet[i][9] || "",
      notes: dataSheet[i][10] || ""
    });
  }

  requests.sort(function(a, b) {
    return String(b.requestDate).localeCompare(String(a.requestDate));
  });

  return {
    success: true,
    requests: requests
  };
}

function getDriverWallet(data) {
  var rawName = data.driverName || data.name || data.driver || "";
  var driverName = normalizeText_(rawName);

  if (!driverName) {
    return { success: false, message: "driverName is required" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();

  var trips = 0;
  var quantity = 0;
  var liters = 0;
  var profit = 0;
  var accountingRevenue = 0;
  var maintenanceCost = 0;

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = sheet.getName();

    if (!extractMonthKeyFromSheetName_(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (!values || values.length === 0) continue;

    var header = values[0] || [];
    var hasHeader = isLikelyHeaderRow_(header);
    var colMap = hasHeader ? buildColumnMap_(header) : {};
    var startRow = hasHeader ? 1 : 0;

    for (var i = startRow; i < values.length; i++) {
      var row = values[i];
      if (!row || row.length === 0) continue;

      var rowDriver = normalizeText_(getCellByAliases_(row, colMap, ["drivername", "driver", "name", "السائق", "اسمالسائق"], 1));

      if (!rowDriver || rowDriver !== driverName) continue;

      var fallbackQtyIndex = /^f_/i.test(sheetName) ? 3 : 5;
      var rowQty = toNumber_(getCellByAliases_(row, colMap, ["quantity", "qty", "الكمية"], fallbackQtyIndex));
      var rowLiters = toNumber_(getCellByAliases_(row, colMap, ["liters", "gas", "لتراتالكاز"], 10));
      var rowPrice = toNumber_(getCellByAliases_(row, colMap, ["price", "profit", "amount", "finalamount", "سعرالنقل"], 13));

      var source = String(getCellByAliases_(row, colMap, ["source", "type", "rowtype"], -1) || "");
      var station = String(getCellByAliases_(row, colMap, ["station", "destination", "المحطة", "الوجهة"], /^f_/i.test(sheetName) ? 4 : 7) || "");
      var factory = String(getCellByAliases_(row, colMap, ["factory", "اسم_المعمل", "اسم_الجهة"], /^f_/i.test(sheetName) ? 4 : -1) || "");
      var isFactory = isFactorySource_(sheetName, source, station, factory);

      var deductionQty = roundNumber_(rowQty * DEDUCTION_RATE, 3);
      var netQty = roundNumber_(rowQty - deductionQty, 3);
      var pricePerTon = getPricePerTonBySource_(isFactory);

      trips++;
      quantity += rowQty;
      liters += rowLiters;
      profit += rowPrice;
      accountingRevenue += roundNumber_(netQty * pricePerTon, 0);
    }
  }

  var maintenanceSheet = ss.getSheetByName("سجل الصيانة");
  if (maintenanceSheet) {
    var mValues = maintenanceSheet.getDataRange().getValues();
    for (var j = 1; j < mValues.length; j++) {
      var mDriver = normalizeText_(mValues[j][1]);
      var cost = toNumber_(mValues[j][8]);
      if (mDriver === driverName) {
        maintenanceCost += cost;
      }
    }
  }

  var deductionQty = roundNumber_(quantity * DEDUCTION_RATE, 3);
  var finalQuantity = roundNumber_(quantity - deductionQty, 3);
  accountingRevenue = roundNumber_(accountingRevenue, 0);

  return {
    success: true,
    trips: trips,
    quantity: quantity,
    liters: liters,
    profit: profit,
    maintenance: maintenanceCost,
    netProfit: profit - maintenanceCost,
    deductionQty: deductionQty,
    finalQuantity: finalQuantity,
    accountingRevenue: accountingRevenue
  };
}

function getDriverTrips(data) {
  var rawName = data.driverName || data.name || data.driver || "";
  var driverName = normalizeText_(rawName);

  if (!driverName) {
    return { success: false, message: "driverName is required" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var trips = [];

  var requestedMonth = "";
  if (data && data.year && data.month) {
    requestedMonth = resolveMonthKey_(String(data.year) + "_" + String(data.month));
  }
  if (!requestedMonth && data && data.month) {
    requestedMonth = resolveMonthKey_(data.month);
  }

  var sheets = [];
  if (requestedMonth) {
    sheets = getMonthSheets_(ss, requestedMonth);
  }

  if (!sheets || sheets.length === 0) {
    var explicitSheets = [
      String(data.sheet || "").trim(),
      String(data.tripSheet || "").trim(),
      String(data.factorySheet || "").trim()
    ];

    var byName = [];
    for (var es = 0; es < explicitSheets.length; es++) {
      var explicitName = explicitSheets[es];
      if (!explicitName) continue;
      var explicitSheet = ss.getSheetByName(explicitName);
      if (explicitSheet) byName.push(explicitSheet);
    }

    // If the client explicitly requested a month, do not fall back to all sheets.
    // Returning all sheets here causes repeated monthly totals.
    if (requestedMonth) {
      sheets = byName;
    } else {
      sheets = byName.length ? byName : ss.getSheets();
    }
  }

  var dayFrom = toNumber_(data.day_from || data.fromDay || 0);
  var dayTo = toNumber_(data.day_to || data.toDay || 0);
  var half = String(data.half || "all").trim().toLowerCase();

  if (half === "first") {
    dayFrom = 1;
    dayTo = 15;
  } else if (half === "second") {
    dayFrom = 16;
    dayTo = 31;
  }

  if (dayFrom <= 0) dayFrom = 1;
  if (dayTo <= 0) dayTo = 31;

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = sheet.getName();

    var values = sheet.getDataRange().getValues();
    if (!values || values.length === 0) continue;

    var header = values[0] || [];
    var hasHeader = isLikelyHeaderRow_(header);
    var colMap = hasHeader ? buildColumnMap_(header) : {};
    var startRow = hasHeader ? 1 : 0;

    for (var i = startRow; i < values.length; i++) {
      var row = values[i];
      if (!row || row.length === 0) continue;

      var rowDriverRaw = getCellByAliases_(row, colMap, ["drivername", "driver", "name", "السائق", "اسمالسائق"], 1);
      var rowDriver = normalizeText_(rowDriverRaw);

      if (!rowDriver || rowDriver !== driverName) continue;

      var docNumber = String(getCellByAliases_(row, colMap, ["docnumber", "doc", "document", "receipt", "رقمالوصل"], 0) || "").trim();
      if (!docNumber) continue;

      var loadDate = String(getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], -1) || "");
      var unloadDate = String(getCellByAliases_(row, colMap, ["unloaddate", "unload", "تاريخالتفريغ", "filldate", "fill_date"], -1) || "");
      var sendTime = String(getCellByAliases_(row, colMap, ["sendtime", "timestamp", "createdat", "created_at", "وقتالارسال"], 9) || "");

      var quantityRaw = getCellByAliases_(row, colMap, ["quantity", "qty", "الكمية"], 5);
      var litersRaw = getCellByAliases_(row, colMap, ["liters", "gas", "لتراتالكاز"], 10);
      var priceRaw = getCellByAliases_(row, colMap, ["price", "profit", "amount", "finalamount", "سعرالنقل"], 13);
      var notes = String(getCellByAliases_(row, colMap, ["notes", "note", "remarks", "ملاحظات"], 14) || "");
      var imageUrlRaw = String(getCellByAliases_(row, colMap, ["imageurl", "image", "photo", "رابطالصورة", "صوره"], 8) || "");

      var station = String(getCellByAliases_(row, colMap, ["station", "destination", "factory", "المحطة", "الوجهة"], 7) || "");
      var factory = String(getCellByAliases_(row, colMap, ["factory", "اسم_المعمل", "اسم_الجهة"], -1) || "");
      var source = String(getCellByAliases_(row, colMap, ["source", "type", "rowtype"], -1) || "");

      // Some monthly trip sheets are saved without a real header row (empty labels in GViz),
      // and some are mis-detected as header while date columns remain unresolved.
      // In both cases rely on legacy trip indexes to keep half-month filtering accurate.
      var isTripSheetByName = !/^f_/i.test(sheetName);
      var isNoHeaderTripRow = !hasHeader && isTripSheetByName;
      var needsLegacyTripDateFallback =
        isTripSheetByName &&
        row.length >= 6 &&
        (!String(loadDate || "").trim() && !String(unloadDate || "").trim());

      if (isNoHeaderTripRow || needsLegacyTripDateFallback) {
        if (!loadDate && row.length > 3) loadDate = String(row[3] || "");
        if (!unloadDate && row.length > 4) unloadDate = String(row[4] || "");
        if (toNumber_(quantityRaw) <= 0 && row.length > 5) quantityRaw = row[5];
        if (!station && row.length > 7) station = String(row[7] || "");
        if (!imageUrlRaw && row.length > 8) imageUrlRaw = String(row[8] || "");
        if (!sendTime && row.length > 9) sendTime = String(row[9] || "");
      }

      var isHeaderFactoryRow =
        hasHeader &&
        (!source && /^f_/i.test(sheetName) || factory !== "") &&
        unloadDate !== "";

      if (isHeaderFactoryRow) {
        if (!loadDate) loadDate = unloadDate;
        if (!station) station = factory;
        if (!source) source = "factory";
      }

      // Some older factory monthly sheets have shifted headers, e.g.:
      // D=quantity, E=factoryName, F=imageUrl, G=unloadDate.
      // In these sheets labels are misleading, so detect and remap values safely.
      var looksLikeUrl_ = function(v) {
        return String(v || "").trim().toLowerCase().indexOf("http") === 0;
      };
      var sourceFactoryBySheet = /^f_/i.test(sheetName);
      var sourceFactoryByValue = String(source || "").toLowerCase().indexOf("factory") > -1;
      var sourceFactoryByText = String(station || "").indexOf("معمل") > -1 || String(unloadDate || "").indexOf("معمل") > -1;
      var isFactoryContext = sourceFactoryBySheet || sourceFactoryByValue || sourceFactoryByText;

      if (hasHeader && isFactoryContext) {
        var quantityLooksUrl = looksLikeUrl_(quantityRaw);
        var mappedFactoryLooksDate = !!extractDayFromDate_(factory);
        var unloadLooksFactoryName = String(unloadDate || "").indexOf("معمل") > -1 || String(unloadDate || "") === "أخرى";

        if (quantityLooksUrl || mappedFactoryLooksDate || unloadLooksFactoryName) {
          var shiftedQty = getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], 3);
          var shiftedFactory = String(getCellByAliases_(row, colMap, ["unloaddate", "unload", "تاريخالتفريغ"], 4) || "");
          var shiftedImage = String(getCellByAliases_(row, colMap, ["quantity", "qty", "الكمية"], 5) || "");
          var shiftedUnloadDate = String(getCellByAliases_(row, colMap, ["factory", "اسم_المعمل", "اسم_الجهة"], 6) || "");

          if (!looksLikeUrl_(shiftedQty)) quantityRaw = shiftedQty;
          if (shiftedFactory) {
            factory = shiftedFactory;
            if (!station) station = shiftedFactory;
          }
          if (looksLikeUrl_(shiftedImage)) imageUrlRaw = shiftedImage;
          if (extractDayFromDate_(shiftedUnloadDate)) {
            unloadDate = shiftedUnloadDate;
            if (!loadDate) loadDate = shiftedUnloadDate;
          }
          if (!source) source = "factory";
        }
      }

      // Legacy factory rows may be stored without headers in a compact shape:
      // [doc, driver, car, quantity, factory, imageUrl, sendTime, notes]
      // Handle this shape explicitly so report cards don't become empty.
      var isLegacyFactoryRow =
        !hasHeader &&
        row.length >= 6 &&
        (String(row[4] || "").indexOf("معمل") > -1 || /^f_/i.test(sheetName)) &&
        String(row[5] || "").indexOf("http") === 0;

      if (isLegacyFactoryRow) {
        quantityRaw = row[3];
        station = String(row[4] || station || "");
        factory = String(row[4] || factory || "");
        sendTime = String(row[6] || sendTime || "");
        loadDate = String(row[6] || loadDate || "");
        unloadDate = String(row[6] || unloadDate || "");
        if (!notes) notes = String(row[7] || "");
        imageUrlRaw = String(row[5] || imageUrlRaw || "");
        source = "factory";
      }

      if (!source && /^f_/i.test(sheetName)) {
        source = "factory";
      }

      // Drop malformed factory rows that have no usable payload.
      // These legacy rows can inflate factory trip counts with zero-value records.
      var isFactorySource = String(source || "").toLowerCase().indexOf("factory") > -1 || /^f_/i.test(sheetName);
      var qtyNum = toNumber_(quantityRaw);
      var hasFactoryPayload =
        qtyNum > 0 ||
        String(factory || "").trim() !== "" ||
        String(station || "").trim() !== "" ||
        String(imageUrlRaw || "").trim() !== "" ||
        String(unloadDate || "").trim() !== "" ||
        String(loadDate || "").trim() !== "" ||
        String(sendTime || "").trim() !== "";
      if (isFactorySource && !hasFactoryPayload) {
        continue;
      }

      var rowDateValue = unloadDate || loadDate || sendTime;
      var rowDay = extractDayFromDate_(rowDateValue);

      if (half !== "all") {
        // Some legacy factory rows have quantity/factory but no usable date value.
        // Keep them visible in first-half reports instead of dropping them entirely.
        if (!rowDay) {
          if (isFactorySource) {
            rowDay = 1;
          } else {
            continue;
          }
        }
        if (rowDay < dayFrom || rowDay > dayTo) continue;
      }

      var grossQty = toNumber_(quantityRaw);
      var finalQty = roundNumber_(grossQty - (grossQty * DEDUCTION_RATE), 3);
      var pricePerTon = getPricePerTonBySource_(isFactorySource);
      var finalAmount = roundNumber_(finalQty * pricePerTon, 0);

      trips.push({
        docNumber: docNumber,
        driverName: String(rowDriverRaw || ""),
        carNumber: String(getCellByAliases_(row, colMap, ["carnumber", "car", "vehicle", "رقمالسيارة"], 2) || ""),
        loadDate: loadDate,
        unloadDate: unloadDate,
        quantity: String(quantityRaw || ""),
        owner: String(
          isLegacyFactoryRow
            ? ""
            : (getCellByAliases_(row, colMap, ["owner", "ownertype", "المالك"], 6) || "")
        ),
        destination: station,
        station: station,
        factory: factory,
        source: source,
        imageUrl: imageUrlRaw,
        sendTime: sendTime,
        liters: String(litersRaw || ""),
        bogerNumber: String(getCellByAliases_(row, colMap, ["bogernumber", "bojer", "رقمالبوجر"], 11) || ""),
        distance: String(getCellByAliases_(row, colMap, ["distance", "المسافة"], 12) || ""),
        price: String(priceRaw || ""),
        notes: notes,
        date: sendTime,
        status: notes.indexOf("REPORTED") > -1 ? "reported" : "ok",
        finalQuantity: finalQty,
        finalAmount: finalAmount
      });
    }
  }

  trips.reverse();

  return {
    success: true,
    trips: trips
  };
}

function checkDoc(data) {
  var rawDocNumber = String((data && data.docNumber) || "").trim();
  var normalizedDocNumber = normalizeDocNumber_(rawDocNumber);

  if (!normalizedDocNumber) {
    return {
      success: false,
      exists: false,
      isExists: false,
      message: "رقم الوصل مطلوب"
    };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var duplicate = findExistingDocNumber_(ss, normalizedDocNumber);

  if (duplicate.found) {
    return {
      success: false,
      exists: true,
      isExists: true,
      message: "رقم الوصل مكرر وموجود مسبقًا",
      docNumber: rawDocNumber,
      sheetName: duplicate.sheetName,
      row: duplicate.rowNumber
    };
  }

  return {
    success: true,
    exists: false,
    isExists: false,
    message: "رقم الوصل متاح"
  };
}

function reportIssue(data) {
  var docNumber = String(data.docNumber || "").trim();
  var driverName = normalizeText_(data.driverName || "");
  var issueType = String(data.issueType || "").trim();

  if (!docNumber || !driverName) {
    return { success: false, message: "docNumber و driverName مطلوبان" };
  }

  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);

  var issuesSheet = ss.getSheetByName("issues");
  if (!issuesSheet) {
    issuesSheet = ss.insertSheet("issues");
    issuesSheet.appendRow([
      "وقت التبليغ",
      "اسم السائق",
      "رقم الوصل",
      "نوع المشكلة",
      "ملاحظة",
      "الحالة"
    ]);
  }

  var time = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy-MM-dd HH:mm:ss");

  issuesSheet.appendRow([
    time,
    String(data.driverName || "").trim(),
    docNumber,
    issueType,
    String(data.note || "").trim(),
    "open"
  ]);

  var sheets = ss.getSheets();

  for (var s = 0; s < sheets.length; s++) {
    var sheet = sheets[s];
    var sheetName = sheet.getName();

    if (!extractMonthKeyFromSheetName_(sheetName)) continue;

    var values = sheet.getDataRange().getValues();
    if (values.length < 2) continue;

    for (var i = 1; i < values.length; i++) {
      var row = values[i];
      var rowDoc = String(row[0] || "").trim();
      var rowDriver = normalizeText_(row[1]);

      if (rowDoc !== docNumber || rowDriver !== driverName) continue;

      var currentNotes = String(row[14] || "");
      var newNote = "REPORTED: " + issueType;

      if (currentNotes.indexOf("REPORTED") === -1) {
        sheet.getRange(i + 1, 15).setValue(
          currentNotes ? currentNotes + " | " + newNote : newNote
        );
      }

      return {
        success: true,
        message: "تم الإبلاغ عن المشكلة بنجاح",
        docNumber: docNumber,
        issueType: issueType
      };
    }
  }

  return {
    success: true,
    message: "تم حفظ البلاغ في issues لكن لم يتم العثور على الوصل في النقلات",
    docNumber: docNumber,
    issueType: issueType
  };
}

function getAvailableMonths() {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheets = ss.getSheets();
  var monthsMap = {};

  for (var i = 0; i < sheets.length; i++) {
    var sheetName = String(sheets[i].getName() || "").trim();
    if (!sheetName) continue;

    // Exclude generated template/report tabs to avoid selecting a non-source month.
    if (/^TPL_/i.test(sheetName)) continue;

    // Keep only the monthly source tabs: yyyy_MM or F_yyyy_MM.
    if (!/^(F_)?\d{4}_\d{2}$/i.test(sheetName)) continue;

    var key = extractMonthKeyFromSheetName_(sheetName);
    if (key) {
      monthsMap[key] = true;
    }
  }

  var months = Object.keys(monthsMap);
  months.sort(function(a, b) {
    return a < b ? 1 : -1;
  });

  return months;
}

function getAllReceiptsData(month) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var monthKey = resolveMonthKey_(month);

  if (!monthKey) {
    return {
      success: true,
      data: [],
      month: ""
    };
  }

  var targetSheets = getMonthSheets_(ss, monthKey);
  var result = [];

  for (var s = 0; s < targetSheets.length; s++) {
    var sheet = targetSheets[s];
    var values = sheet.getDataRange().getValues();
    if (!values || values.length === 0) continue;

    var sheetName = String(sheet.getName() || "");
    var header = values[0] || [];
    var hasHeader = isLikelyHeaderRow_(header);
    var colMap = hasHeader ? buildColumnMap_(header) : {};
    var startRow = hasHeader ? 1 : 0;

    for (var i = startRow; i < values.length; i++) {
      var row = values[i];
      if (!row || row.length === 0) continue;

      if (
        String(getCellByAliases_(row, colMap, ["docnumber", "doc", "document", "receipt", "رقمالوصل"], 0) || "").trim() === "" &&
        String(getCellByAliases_(row, colMap, ["drivername", "driver", "name", "السائق", "اسمالسائق"], 1) || "").trim() === "" &&
        String(getCellByAliases_(row, colMap, ["carnumber", "car", "vehicle", "رقمالسيارة"], 2) || "").trim() === ""
      ) {
        continue;
      }

      var fallbackQtyIndex = /^f_/i.test(sheetName) ? 3 : 5;
      var grossQty = toNumber_(getCellByAliases_(row, colMap, ["quantity", "qty", "الكمية"], fallbackQtyIndex));
      var deductionQty = roundNumber_(grossQty * DEDUCTION_RATE, 3);
      var netQty = roundNumber_(grossQty - deductionQty, 3);
      var liters = toNumber_(getCellByAliases_(row, colMap, ["liters", "gas", "لتراتالكاز"], 10));
      var gasCost = roundNumber_(liters * PRICE_PER_LITER, 0);

      var source = String(getCellByAliases_(row, colMap, ["source", "type", "rowtype"], -1) || "");
      var destination = String(getCellByAliases_(row, colMap, ["station", "destination", "المحطة", "الوجهة"], /^f_/i.test(sheetName) ? 4 : 7) || "");
      var factory = String(getCellByAliases_(row, colMap, ["factory", "اسم_المعمل", "اسم_الجهة"], /^f_/i.test(sheetName) ? 4 : -1) || "");
      var isFactory = isFactorySource_(sheetName, source, destination, factory);

      // Older factory sheets may have shifted headers where the quantity header points to a URL/date column.
      // Re-read from legacy index if the extracted quantity is zero in factory context.
      if (isFactory && grossQty <= 0) {
        var shiftedQty = getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], 3);
        var shiftedQtyNum = toNumber_(shiftedQty);
        if (shiftedQtyNum > 0) {
          grossQty = shiftedQtyNum;
          deductionQty = roundNumber_(grossQty * DEDUCTION_RATE, 3);
          netQty = roundNumber_(grossQty - deductionQty, 3);
        }
      }

      var pricePerTon = getPricePerTonBySource_(isFactory);
      var finalAmount = roundNumber_(netQty * pricePerTon, 0);

      var loadDate = String(getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], 3) || "");
      var unloadDate = String(getCellByAliases_(row, colMap, ["unloaddate", "unload", "تاريخالتفريغ", "filldate", "fill_date"], /^f_/i.test(sheetName) ? 6 : 4) || "");
      var timestamp = String(getCellByAliases_(row, colMap, ["sendtime", "timestamp", "createdat", "created_at", "وقتالارسال"], 9) || "");

      result.push({
        row: i + 1,
        sheetName: sheet.getName(),
        docNumber: String(getCellByAliases_(row, colMap, ["docnumber", "doc", "document", "receipt", "رقمالوصل"], 0) || ""),
        driverName: String(getCellByAliases_(row, colMap, ["drivername", "driver", "name", "السائق", "اسمالسائق"], 1) || ""),
        carNumber: String(getCellByAliases_(row, colMap, ["carnumber", "car", "vehicle", "رقمالسيارة"], 2) || ""),
        loadDate: loadDate,
        unloadDate: unloadDate,
        quantity: grossQty,
        deductionQty: deductionQty,
        netQuantity: netQty,
        owner: String(getCellByAliases_(row, colMap, ["owner", "ownertype", "المالك"], 6) || ""),
        destination: destination,
        imageUrl: String(getCellByAliases_(row, colMap, ["imageurl", "image", "photo", "رابطالصورة", "صوره"], /^f_/i.test(sheetName) ? 5 : 8) || ""),
        timestamp: timestamp,
        liters: liters,
        bojer: String(getCellByAliases_(row, colMap, ["bogernumber", "bojer", "رقمالبوجر"], 11) || ""),
        distance: toNumber_(getCellByAliases_(row, colMap, ["distance", "المسافة"], 12)),
        storedPrice: toNumber_(getCellByAliases_(row, colMap, ["price", "profit", "amount", "finalamount", "سعرالنقل"], 13)),
        price: finalAmount,
        gasCost: gasCost,
        notes: String(getCellByAliases_(row, colMap, ["notes", "note", "remarks", "ملاحظات"], 14) || ""),
        period15: getPeriodFromDate_(unloadDate || loadDate || timestamp),
        month: monthKey
      });
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

  if (!sheet) {
    return { success: true, data: [] };
  }

  var monthKey = resolveMonthKey_(month);
  var values = sheet.getDataRange().getValues();
  var list = [];

  for (var i = 1; i < values.length; i++) {
    var rowDate = values[i][5];

    if (monthKey && !sameMonth_(rowDate, monthKey)) {
      continue;
    }

    list.push({
      row: i + 1,
      requestId: String(values[i][0] || ""),
      driverName: String(values[i][1] || ""),
      carNumber: String(values[i][2] || ""),
      type: String(values[i][7] || values[i][3] || ""),
      cost: toNumber_(values[i][8]),
      date: String(values[i][5] || ""),
      notes: String(values[i][10] || ""),
      imageUrl: String(values[i][11] || "")
    });
  }

  list.sort(function(a, b) {
    return String(b.date || "").localeCompare(String(a.date || ""));
  });

  return {
    success: true,
    data: list
  };
}

function getDashboardSummary(data) {
  var monthKey = resolveMonthKey_(data && data.month ? data.month : buildMonthKey_(new Date()));
  if (!monthKey) {
    return {
      success: false,
      message: "month مطلوب"
    };
  }

  var half = String((data && data.half) || "all").trim().toLowerCase();
  if (half !== "all" && half !== "first" && half !== "second") half = "all";

  var rows = getTemplateRecordsFast_(monthKey);
  var trips = 0;
  var grossQty = 0;
  var netQty = 0;
  var amount = 0;
  var preview = [];

  for (var i = 0; i < rows.length; i++) {
    var r = rows[i] || {};
    var period = Number(r.period15 || 0);
    if (half === "first" && period !== 1) continue;
    if (half === "second" && period !== 2) continue;

    var gross = toNumber_(r.quantity);
    var net = toNumber_(r.netQuantity);
    if (net <= 0) net = roundNumber_(gross - (gross * DEDUCTION_RATE), 3);

    var isFactory = isFactorySource_(
      String(r.sheetName || ""),
      String(r.source || ""),
      String(r.destination || ""),
      String(r.factory || "")
    );
    var pricePerTon = getPricePerTonBySource_(isFactory);
    var rowAmount = roundNumber_(net * pricePerTon, 0);

    trips += 1;
    grossQty += gross;
    netQty += net;
    amount += rowAmount;

    if (preview.length < 25) {
      preview.push({
        docNumber: String(r.docNumber || ""),
        driverName: String(r.driverName || ""),
        carNumber: String(r.carNumber || ""),
        unloadDate: String(r.unloadDate || ""),
        loadDate: String(r.loadDate || ""),
        grossQty: gross,
        netQty: net,
        pricePerTon: pricePerTon,
        amount: rowAmount
      });
    }
  }

  return {
    success: true,
    month: monthKey,
    half: half,
    trips: trips,
    grossQty: roundNumber_(grossQty, 3),
    netQty: roundNumber_(netQty, 3),
    amount: roundNumber_(amount, 0),
    preview: preview
  };
}

function deleteReceiptRow(rowId, month, sheetName) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var sheet = null;

  if (sheetName) {
    sheet = ss.getSheetByName(String(sheetName).trim());
  }

  if (!sheet) {
    sheet = getMonthSheet_(ss, month);
  }

  if (!sheet) {
    return { success: false, message: "Month sheet not found" };
  }

  var rowNumber = Number(rowId || 0);

  if (rowNumber > 1 && rowNumber <= sheet.getLastRow()) {
    sheet.deleteRow(rowNumber);
    return { success: true, message: "تم حذف السجل بنجاح" };
  }

  return { success: false, message: "Invalid row number" };
}

function ensureTripsSheet_(ss, sheetName) {
  var sheet = ss.getSheetByName(sheetName);

  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow([
      "رقم الوصل",
      "اسم السائق",
      "رقم السيارة",
      "تاريخ التحميل",
      "تاريخ التفريغ",
      "الكمية",
      "المالك",
      "المحطة",
      "رابط الصورة",
      "وقت الإرسال",
      "لترات الكاز",
      "رقم البوجر",
      "المسافة",
      "سعر النقل",
      "ملاحظات"
    ]);
  }

  return sheet;
}

function ensureFactorySheet_(ss, sheetName) {
  var sheet = ss.getSheetByName(sheetName);

  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
    sheet.appendRow([
      "رقم الوصل",
      "اسم السائق",
      "رقم السيارة",
      "الكمية",
      "اسم المعمل",
      "رابط الصورة",
      "تاريخ التفريغ",
      "ملاحظات",
      "source",
      "وقت الإرسال"
    ]);
  }

  return sheet;
}

function tripHeaders_() {
  return [
    "رقم الوصل",
    "اسم السائق",
    "رقم السيارة",
    "تاريخ التحميل",
    "تاريخ التفريغ",
    "الكمية",
    "المالك",
    "المحطة",
    "رابط الصورة",
    "وقت الإرسال",
    "لترات الكاز",
    "رقم البوجر",
    "المسافة",
    "سعر النقل",
    "ملاحظات"
  ];
}

function factoryHeaders_() {
  return [
    "رقم الوصل",
    "اسم السائق",
    "رقم السيارة",
    "الكمية",
    "اسم المعمل",
    "رابط الصورة",
    "تاريخ التفريغ",
    "ملاحظات",
    "source",
    "وقت الإرسال"
  ];
}

function isHeaderRowEffectivelyEmpty_(row) {
  if (!row || !row.length) return true;
  for (var i = 0; i < row.length; i++) {
    if (String(row[i] || "").trim() !== "") return false;
  }
  return true;
}

function schemaAudit(data) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var monthKey = resolveMonthKey_(data && data.month ? data.month : "");
  var months = monthKey ? [monthKey] : getAvailableMonths();
  var rows = [];

  for (var i = 0; i < months.length; i++) {
    var m = months[i];
    var targets = [
      { sheetName: m, type: "trip", headers: tripHeaders_() },
      { sheetName: "F_" + m, type: "factory", headers: factoryHeaders_() }
    ];

    for (var t = 0; t < targets.length; t++) {
      var target = targets[t];
      var sheet = ss.getSheetByName(target.sheetName);

      if (!sheet) {
        rows.push({
          month: m,
          sheetName: target.sheetName,
          type: target.type,
          exists: false,
          lastRow: 0,
          hasHeader: false,
          headerEmpty: true,
          needsFix: true,
          reason: "missing_sheet"
        });
        continue;
      }

      var lastRow = sheet.getLastRow();
      var headerRow = [];
      if (lastRow > 0) {
        headerRow = sheet.getRange(1, 1, 1, target.headers.length).getValues()[0] || [];
      }

      var hasHeader = lastRow > 0 ? isLikelyHeaderRow_(headerRow) : false;
      var headerEmpty = isHeaderRowEffectivelyEmpty_(headerRow);
      var needsFix = (lastRow === 0) || !hasHeader || headerEmpty;

      rows.push({
        month: m,
        sheetName: target.sheetName,
        type: target.type,
        exists: true,
        lastRow: lastRow,
        hasHeader: hasHeader,
        headerEmpty: headerEmpty,
        needsFix: needsFix,
        reason: needsFix ? "missing_or_invalid_header" : "ok"
      });
    }
  }

  return {
    success: true,
    auditedMonths: months,
    rows: rows
  };
}

function schemaFix(data) {
  var ss = SpreadsheetApp.openById(SPREADSHEET_ID);
  var monthKey = resolveMonthKey_(data && data.month ? data.month : "");
  var months = monthKey ? [monthKey] : getAvailableMonths();
  var applyFix = String((data && data.apply) || "").toLowerCase() === "true" || String((data && data.apply) || "") === "1";
  var changes = [];

  for (var i = 0; i < months.length; i++) {
    var m = months[i];
    var targets = [
      { sheetName: m, type: "trip", headers: tripHeaders_() },
      { sheetName: "F_" + m, type: "factory", headers: factoryHeaders_() }
    ];

    for (var t = 0; t < targets.length; t++) {
      var target = targets[t];
      var sheet = ss.getSheetByName(target.sheetName);
      var action = "none";

      if (!sheet) {
        if (applyFix) {
          if (target.type === "trip") {
            sheet = ensureTripsSheet_(ss, target.sheetName);
          } else {
            sheet = ensureFactorySheet_(ss, target.sheetName);
          }
          action = "created_sheet_with_header";
        } else {
          action = "would_create_sheet_with_header";
        }

        changes.push({
          month: m,
          sheetName: target.sheetName,
          type: target.type,
          action: action
        });
        continue;
      }

      var lastRow = sheet.getLastRow();
      var headerRow = [];
      if (lastRow > 0) {
        headerRow = sheet.getRange(1, 1, 1, target.headers.length).getValues()[0] || [];
      }

      var hasHeader = lastRow > 0 ? isLikelyHeaderRow_(headerRow) : false;
      var headerEmpty = isHeaderRowEffectivelyEmpty_(headerRow);
      var needsFix = (lastRow === 0) || !hasHeader || headerEmpty;

      if (!needsFix) {
        changes.push({
          month: m,
          sheetName: target.sheetName,
          type: target.type,
          action: "ok_no_change"
        });
        continue;
      }

      if (!applyFix) {
        changes.push({
          month: m,
          sheetName: target.sheetName,
          type: target.type,
          action: "would_insert_or_set_header"
        });
        continue;
      }

      if (lastRow <= 0) {
        sheet.getRange(1, 1, 1, target.headers.length).setValues([target.headers]);
        action = "set_header_on_empty_sheet";
      } else {
        sheet.insertRowBefore(1);
        sheet.getRange(1, 1, 1, target.headers.length).setValues([target.headers]);
        action = "inserted_header_row_at_top";
      }

      changes.push({
        month: m,
        sheetName: target.sheetName,
        type: target.type,
        action: action
      });
    }
  }

  return {
    success: true,
    dryRun: !applyFix,
    months: months,
    changes: changes
  };
}

function getCurrentTripsSheet_(ss) {
  var currentName = buildMonthKey_(new Date());
  var currentSheet = ss.getSheetByName(currentName);
  if (currentSheet) return currentSheet;

  var allSheets = ss.getSheets();
  var matched = [];

  for (var i = 0; i < allSheets.length; i++) {
    var n = allSheets[i].getName();
    if (extractMonthKeyFromSheetName_(n)) {
      matched.push(allSheets[i]);
    }
  }

  if (matched.length > 0) {
    matched.sort(function(a, b) {
      return a.getName() < b.getName() ? 1 : -1;
    });
    return matched[0];
  }

  return null;
}

function getMonthSheet_(ss, month) {
  var list = getMonthSheets_(ss, month);
  return list.length ? list[0] : null;
}

function getMonthSheets_(ss, month) {
  var key = resolveMonthKey_(month);
  if (!key) return [];

  var sheets = ss.getSheets();
  var matched = [];

  for (var i = 0; i < sheets.length; i++) {
    var foundKey = extractMonthKeyFromSheetName_(sheets[i].getName());
    if (foundKey === key) {
      matched.push(sheets[i]);
    }
  }

  matched.sort(function(a, b) {
    return String(a.getName()).localeCompare(String(b.getName()));
  });

  return matched;
}

function buildMonthKey_(dateObj) {
  return Utilities.formatDate(dateObj || new Date(), "Asia/Baghdad", "yyyy_MM");
}

function resolveSubmissionMonthPolicy_(data) {
  var unloadDate = String((data && data.unloadDate) || "").trim();
  var loadDate = String((data && data.loadDate) || "").trim();
  var effectiveDate = unloadDate || loadDate;

  var nowMonth = buildMonthKey_(new Date());

  // Keep backward compatibility for legacy clients that do not send dates.
  if (!effectiveDate) {
    return {
      success: true,
      monthKey: nowMonth,
      usedFallbackNowMonth: true
    };
  }

  var parsed = parseDateParts_(effectiveDate);
  if (!parsed || parsed.year < 2000 || parsed.month < 1 || parsed.month > 12) {
    return {
      success: false,
      message: "تاريخ النقلة غير صالح. يرجى إرسال تاريخ تحميل أو تفريغ صحيح"
    };
  }

  var tripMonth = parsed.year + "_" + ("0" + parsed.month).slice(-2);
  var tripValue = monthKeyToComparableNumber_(tripMonth);
  var nowValue = monthKeyToComparableNumber_(nowMonth);

  var allowHistoricalEdit = isHistoricalEditAllowed_(data);

  if (tripValue < nowValue && !allowHistoricalEdit) {
    return {
      success: false,
      message: "لا يمكن إرسال تاريخ شهر قديم في الشهر الحالي. فعّل وضع التعديل القديم للمشرف إذا كان هذا تعديلًا",
      tripMonth: tripMonth,
      currentMonth: nowMonth
    };
  }

  if (tripValue > nowValue && !allowHistoricalEdit) {
    return {
      success: false,
      message: "لا يمكن إرسال تاريخ شهر مستقبلي قبل أوانه",
      tripMonth: tripMonth,
      currentMonth: nowMonth
    };
  }

  return {
    success: true,
    monthKey: tripMonth,
    currentMonth: nowMonth,
    usedFallbackNowMonth: false
  };
}

function monthKeyToComparableNumber_(monthKey) {
  var k = resolveMonthKey_(monthKey);
  if (!k) return 0;
  var parts = k.split("_");
  return (toNumber_(parts[0]) * 100) + toNumber_(parts[1]);
}

function isHistoricalEditAllowed_(data) {
  var flags = [
    data && data.allowHistoricalEdit,
    data && data.adminOverride,
    data && data.editMode,
    data && data.allowPastDate
  ];

  for (var i = 0; i < flags.length; i++) {
    var v = String(flags[i] || "").trim().toLowerCase();
    if (v === "1" || v === "true" || v === "yes" || v === "on") {
      return true;
    }
  }

  return false;
}

function resolveMonthKey_(value) {
  var s = String(value || "").trim();
  if (!s) return "";

  s = s.replace(/[٠-٩]/g, function(d) {
    return "٠١٢٣٤٥٦٧٨٩".indexOf(d);
  });

  s = s.replace(/\//g, "_").replace(/-/g, "_").replace(/\s+/g, "");

  var m = s.match(/^(\d{4})_(\d{1,2})$/);
  if (m) return m[1] + "_" + ("0" + m[2]).slice(-2);

  m = s.match(/^(\d{1,2})_(\d{4})$/);
  if (m) return m[2] + "_" + ("0" + m[1]).slice(-2);

  m = s.match(/(\d{4}_\d{2})/);
  if (m) return m[1];

  return "";
}

function extractMonthKeyFromSheetName_(name) {
  var s = String(name || "").trim();
  var m = s.match(/(\d{4}_\d{2})/);
  if (!m) {
    var m2 = s.match(/(\d{1,2}_\d{4})/);
    if (m2) {
      return resolveMonthKey_(m2[1]);
    }
  }
  return m ? m[1] : "";
}

function sameMonth_(dateValue, monthKey) {
  var key = resolveMonthKey_(monthKey);
  if (!key) return true;

  if (Object.prototype.toString.call(dateValue) === "[object Date]" && !isNaN(dateValue)) {
    return Utilities.formatDate(dateValue, "Asia/Baghdad", "yyyy_MM") === key;
  }

  var str = String(dateValue || "").trim();
  if (!str) return false;

  str = str.replace(/[٠-٩]/g, function(d) {
    return "٠١٢٣٤٥٦٧٨٩".indexOf(d);
  });

  var parts = key.split("_");
  var year = Number(parts[0]);
  var month = Number(parts[1]);

  var parsed = parseDateParts_(str);
  if (!parsed) return false;

  return parsed.year === year && parsed.month === month;
}

function getPeriodFromDate_(dateValue) {
  var day = extractDayFromDate_(dateValue);
  if (!day) return 1;
  return day <= 15 ? 1 : 2;
}

function parseDateParts_(raw) {
  if (!raw) return null;

  var text = String(raw).trim();
  if (!text) return null;

  text = text.replace(/[٠-٩]/g, function(d) {
    return "٠١٢٣٤٥٦٧٨٩".indexOf(d);
  });

  text = text.split("T")[0].split(" ")[0].replace(/\./g, "/").replace(/-/g, "/");

  var m = text.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})$/);
  if (m) {
    return { year: Number(m[1]), month: Number(m[2]), day: Number(m[3]) };
  }

  m = text.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (m) {
    return { year: Number(m[3]), month: Number(m[2]), day: Number(m[1]) };
  }

  return null;
}

function extractDayFromDate_(dateValue) {
  if (Object.prototype.toString.call(dateValue) === "[object Date]" && !isNaN(dateValue)) {
    return dateValue.getDate();
  }

  var text = String(dateValue || "").trim();
  if (!text) return null;

  var numeric = text.replace(/[^0-9]/g, "");
  if (numeric.length === 10 || numeric.length === 13) {
    var epoch = Number(numeric);
    if (!isNaN(epoch) && epoch > 0) {
      var millis = numeric.length === 10 ? epoch * 1000 : epoch;
      var dateFromEpoch = new Date(millis);
      if (!isNaN(dateFromEpoch.getTime())) {
        return dateFromEpoch.getDate();
      }
    }
  }

  var parsed = parseDateParts_(text);
  if (parsed && parsed.day >= 1 && parsed.day <= 31) return parsed.day;

  var autoParsed = new Date(text);
  if (!isNaN(autoParsed.getTime())) {
    return autoParsed.getDate();
  }

  return null;
}

function isFactorySource_(sheetName, sourceValue, destinationValue, factoryValue) {
  var bySheet = /^f_/i.test(String(sheetName || ""));
  var bySource = String(sourceValue || "").toLowerCase().indexOf("factory") > -1;
  var byDestination = String(destinationValue || "").indexOf("معمل") > -1;
  var byFactory = String(factoryValue || "").indexOf("معمل") > -1;
  return bySheet || bySource || byDestination || byFactory;
}

function getPricePerTonBySource_(isFactory) {
  return isFactory ? PRICE_PER_TON_FACTORY : PRICE_PER_TON_HALAFAYA;
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
    var token = normalizeHeaderToken_(row[i]);
    if (token && known[token]) hits++;
  }

  return hits >= 2;
}

function buildColumnMap_(headerRow) {
  var map = {};
  if (!headerRow || !headerRow.length) return map;

  for (var i = 0; i < headerRow.length; i++) {
    var key = normalizeHeaderToken_(headerRow[i]);
    if (!key) continue;
    if (typeof map[key] === "undefined") {
      map[key] = i;
    }
  }

  return map;
}

function getCellByAliases_(row, colMap, aliases, fallbackIndex) {
  for (var i = 0; i < aliases.length; i++) {
    var key = normalizeHeaderToken_(aliases[i]);
    if (typeof colMap[key] !== "undefined") {
      return row[colMap[key]];
    }
  }

  if (typeof fallbackIndex === "number" && fallbackIndex >= 0) {
    return row[fallbackIndex];
  }

  return "";
}

function normalizeHeaderToken_(value) {
  return normalizeText_(value)
    .replace(/[^a-z0-9\u0600-\u06FF]/g, "")
    .trim();
}

function normalizeText_(value) {
  return String(value || "")
    .replace(/\u00A0/g, " ")
    .replace(/\s+/g, " ")
    .replace(/[أإآ]/g, "ا")
    .replace(/ة/g, "ه")
    .replace(/ى/g, "ي")
    .trim()
    .toLowerCase();
}

function toNumber_(value) {
  if (value === null || value === "" || typeof value === "undefined") return 0;

  var s = String(value)
    .replace(/[٠-٩]/g, function(d) {
      return "٠١٢٣٤٥٦٧٨٩".indexOf(d);
    })
    .replace(/[^\d.\-]/g, "")
    .trim();

  var n = parseFloat(s);
  return isNaN(n) ? 0 : n;
}

function roundNumber_(value, digits) {
  var n = Number(value || 0);
  var p = Math.pow(10, digits || 0);
  return Math.round((n + Number.EPSILON) * p) / p;
}

function sanitizeHtml_(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function openSpreadsheetWithRetry_() {
  var attempts = 3;
  var lastError = null;

  for (var i = 0; i < attempts; i++) {
    try {
      return SpreadsheetApp.openById(SPREADSHEET_ID);
    } catch (err) {
      lastError = err;
      if (i >= attempts - 1) break;
      Utilities.sleep(250 * (i + 1));
    }
  }

  throw lastError || new Error("تعذر فتح ملف Google Sheets");
}

function prepareGeneratedSheet_(ss, sheetName, minRows, minColumns) {
  var sheet = ss.getSheetByName(sheetName);
  if (!sheet) {
    sheet = ss.insertSheet(sheetName);
  } else {
    var lastRow = Math.max(sheet.getLastRow(), minRows || 1);
    var lastColumn = Math.max(sheet.getLastColumn(), minColumns || 1);
    var resetRange = sheet.getRange(1, 1, lastRow, lastColumn);
    resetRange.breakApart();
    resetRange.clearContent();
    resetRange.clearFormat();
    resetRange.setBorder(false, false, false, false, false, false);
  }

  sheet.setHiddenGridlines(true);
  return sheet;
}

function buildDemandTemplateSheet(data) {
  var reportDate = String((data && data.reportDate) || "").trim();
  if (!reportDate) {
    reportDate = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy/M/d");
  }

  var reportDateMonth = "";
  var reportDateParts = parseDateParts_(reportDate);
  if (reportDateParts && reportDateParts.year >= 2000 && reportDateParts.month >= 1 && reportDateParts.month <= 12) {
    reportDateMonth = reportDateParts.year + "_" + ("0" + reportDateParts.month).slice(-2);
  }

  var monthKey = resolveMonthKey_(
    (data && data.month) ||
    ((data && data.year && data.monthNumber) ? (String(data.year) + "_" + String(data.monthNumber)) : "") ||
    reportDateMonth
  );

  if (!monthKey) {
    return {
      success: false,
      message: "month مطلوب بصيغة yyyy_MM"
    };
  }

  var half = String((data && data.half) || "all").trim().toLowerCase();
  if (half !== "all" && half !== "first" && half !== "second") {
    half = "all";
  }

  var requestedTemplateSheetName = String((data && data.templateSheetName) || "").trim();

  var demandNo = String((data && data.demandNo) || "61").trim();
  var routeText = String((data && data.route) || "محور - حلفاية - بغداد - تاجي").trim();
  var receiverText = String((data && data.receiver) || "شركة تعبئة الغاز").trim();
  var productText = String((data && data.product) || "غاز سائل").trim();

  var ss = openSpreadsheetWithRetry_();

  // Use fast month reader to avoid timeout on large spreadsheets during template generation.
  var records = getTemplateRecordsFast_(monthKey, ss);

  var filtered = [];
  for (var i = 0; i < records.length; i++) {
    var rec = records[i] || {};
    var period = Number(rec.period15 || getPeriodFromDate_(rec.unloadDate || rec.loadDate || rec.timestamp || "") || 0);

    if (half === "first" && period !== 1) continue;
    if (half === "second" && period !== 2) continue;

    filtered.push(rec);
  }

  var usedAutoMonthFromReportDate = false;
  if (filtered.length === 0 && reportDateMonth && reportDateMonth !== monthKey) {
    var fallbackRecords = getTemplateRecordsFast_(reportDateMonth, ss);
    for (var f = 0; f < fallbackRecords.length; f++) {
      var fallbackRec = fallbackRecords[f] || {};
      var fallbackPeriod = Number(fallbackRec.period15 || getPeriodFromDate_(fallbackRec.unloadDate || fallbackRec.loadDate || fallbackRec.timestamp || "") || 0);
      if (half === "first" && fallbackPeriod !== 1) continue;
      if (half === "second" && fallbackPeriod !== 2) continue;
      filtered.push(fallbackRec);
    }
    if (filtered.length > 0) {
      monthKey = reportDateMonth;
      usedAutoMonthFromReportDate = true;
    }
  }

  filtered.sort(function(a, b) {
    var aDoc = Number(a.docNumber || 0);
    var bDoc = Number(b.docNumber || 0);
    if (!isNaN(aDoc) && !isNaN(bDoc) && aDoc !== bDoc) return aDoc - bDoc;
    return String(a.docNumber || "").localeCompare(String(b.docNumber || ""));
  });

  if (!filtered.length) {
    return {
      success: false,
      message: "لا توجد بيانات مطابقة لإنشاء القالب في الشهر/الفترة المحددة",
      month: monthKey,
      half: half
    };
  }

  var halfTag = (half === "all" ? "ALL" : (half === "first" ? "FIRST" : "SECOND"));
  var sheetName = "TPL_" + monthKey + "_" + halfTag;
  var sheet = prepareGeneratedSheet_(ss, sheetName, filtered.length + 6, 13);
  sheet.setRightToLeft(true);

  // 13 columns template: A..M
  sheet.getRange("A1:M1").merge();
  sheet.getRange("A1").setValue("كشف بالكميات المنقولة بواسطة السيارات العاملة بمعية شركة الناقلات النموذجية");

  sheet.getRange("A2:B2").merge().setValue("مطالبة رقم (" + demandNo + ")");
  sheet.getRange("C2:F2").merge().setValue("الجهة المجهزة / " + routeText);
  sheet.getRange("G2:H2").merge().setValue("تاريخها " + reportDate);
  sheet.getRange("I2:K2").merge().setValue("الجهة المستلمة: " + receiverText);
  sheet.getRange("L2:M2").merge().setValue("المنتوج / " + productText);

  var headers = [[
    "ت",
    "رقم مستند النقلة الاصلية",
    "اسم السائق",
    "رقم السيارة",
    "تاريخ التحميل",
    "تاريخ التفريغ",
    "الكمية المحملة",
    "الكمية المفرغة",
    "الكمية المحتسبة",
    "سعر النقلة الاصلية / طن",
    "مبلغ النقلة الاصلية",
    "كمية النقص",
    ""
  ]];
  sheet.getRange(3, 1, 1, 13).setValues(headers);

  var rows = [];
  var totalAccountedQty = 0;
  var totalAmount = 0;

  for (var r = 0; r < filtered.length; r++) {
    var item = filtered[r] || {};
    var grossQty = toNumber_(item.quantity);
    var netQty = toNumber_(item.netQuantity);
    if (netQty <= 0) {
      netQty = roundNumber_(grossQty - (grossQty * DEDUCTION_RATE), 3);
    }

    var source = String(item.source || "");
    var dest = String(item.destination || "");
    var pricePerTon = getPricePerTonBySource_(isFactorySource_(item.sheetName || "", source, dest, ""));

    var amount = roundNumber_(netQty * pricePerTon, 0);
    var unloadDate = formatTemplateDate_(item.unloadDate || item.loadDate || item.timestamp || "");
    var loadDate = formatTemplateDate_(item.loadDate || item.unloadDate || item.timestamp || "");

    rows.push([
      r + 1,
      String(item.docNumber || ""),
      String(item.driverName || ""),
      String(item.carNumber || ""),
      loadDate,
      unloadDate,
      grossQty,
      grossQty,
      netQty,
      pricePerTon,
      amount,
      0,
      ""
    ]);

    totalAccountedQty += netQty;
    totalAmount += amount;
  }

  var startRow = 4;
  if (rows.length > 0) {
    sheet.getRange(startRow, 1, rows.length, 13).setValues(rows);
  }

  var totalRow = startRow + rows.length;
  sheet.getRange(totalRow, 1, 1, 3).merge().setValue("مجموع مبلغ القائمة");
  sheet.getRange(totalRow, 4).setValue(totalAmount);
  sheet.getRange(totalRow, 5, 1, 4).merge().setValue("مجموع الكمية المحتسبة =");
  sheet.getRange(totalRow, 9).setValue(totalAccountedQty);
  sheet.getRange(totalRow, 10, 1, 4).merge().setValue("ق - (21) ---");

  var lastDataRow = totalRow;
  var tableRange = sheet.getRange(1, 1, lastDataRow, 13);
  tableRange
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle")
    .setBorder(true, true, true, true, true, true, "#202124", SpreadsheetApp.BorderStyle.SOLID);

  sheet.getRange("A1:M1")
    .setFontSize(18)
    .setFontWeight("bold")
    .setBackground("#f2f2f2");

  sheet.getRange("A2:M2")
    .setFontWeight("bold")
    .setBackground("#f7f7f7");

  sheet.getRange("A3:M3")
    .setFontWeight("bold")
    .setBackground("#efefef")
    .setWrap(true);

  if (rows.length > 0) {
    sheet.getRange(startRow, 7, rows.length, 3).setNumberFormat("#,##0.###");
    sheet.getRange(startRow, 10, rows.length, 1).setNumberFormat("#,##0");
    sheet.getRange(startRow, 11, rows.length, 1).setNumberFormat("#,##0");
  }

  sheet.getRange(totalRow, 4).setNumberFormat("#,##0").setBackground("#9ccc65").setFontWeight("bold");
  sheet.getRange(totalRow, 9).setNumberFormat("#,##0.###").setBackground("#9ccc65").setFontWeight("bold");

  // Set approximate column widths similar to the provided form.
  var widths = [42, 110, 150, 75, 95, 95, 85, 85, 85, 95, 130, 70, 60];
  for (var c = 0; c < widths.length; c++) {
    sheet.setColumnWidth(c + 1, widths[c]);
  }

  sheet.setRowHeight(1, 42);
  sheet.setRowHeight(2, 34);
  sheet.setRowHeight(3, 42);
  if (lastDataRow >= startRow) {
    sheet.setRowHeights(startRow, lastDataRow - startRow + 1, 34);
  }

  sheet.setFrozenRows(3);

  return {
    success: true,
    message: usedAutoMonthFromReportDate
      ? "تم إنشاء ورقة القالب بنجاح (تم اعتماد شهر تاريخ المطالبة تلقائيًا لوجود بيانات)"
      : "تم إنشاء ورقة القالب بنجاح",
    sheetName: sheetName,
    month: monthKey,
    half: half,
    rows: rows.length,
    spreadsheetUrl: ss.getUrl(),
    sheetUrl: ss.getUrl() + "#gid=" + sheet.getSheetId(),
    pdfUrl: buildSheetPdfUrl_(ss.getId(), sheet.getSheetId(), sheetName)
  };
}

function exportOfficialTemplateXlsx(data) {
  var monthKey = resolveMonthKey_((data && data.month) || "");
  if (!monthKey) {
    return {
      success: false,
      message: "month مطلوب بصيغة yyyy_MM"
    };
  }

  var half = String((data && data.half) || "all").trim().toLowerCase();
  if (half !== "all" && half !== "first" && half !== "second") {
    half = "all";
  }

  var requestedTemplateSheetName = String((data && data.templateSheetName) || "").trim();

  var demandNo = String((data && data.demandNo) || "61").trim();
  var reportDate = String((data && data.reportDate) || "").trim();
  if (!reportDate) {
    reportDate = Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy/M/d");
  }

  var routeText = String((data && data.route) || "محور - حلفاية - بغداد - تاجي").trim();
  var receiverText = String((data && data.receiver) || "شركة تعبئة الغاز").trim();
  var productText = String((data && data.product) || "غاز سائل").trim();

  var ss = openSpreadsheetWithRetry_();
  var records = getTemplateRecordsFast_(monthKey, ss);
  var filtered = [];

  for (var i = 0; i < records.length; i++) {
    var rec = records[i] || {};
    var period = Number(rec.period15 || getPeriodFromDate_(rec.unloadDate || rec.loadDate || rec.timestamp || "") || 0);
    if (half === "first" && period !== 1) continue;
    if (half === "second" && period !== 2) continue;
    filtered.push(rec);
  }

  filtered.sort(function(a, b) {
    var aDoc = Number(a.docNumber || 0);
    var bDoc = Number(b.docNumber || 0);
    if (!isNaN(aDoc) && !isNaN(bDoc) && aDoc !== bDoc) return aDoc - bDoc;
    return String(a.docNumber || "").localeCompare(String(b.docNumber || ""));
  });

  if (!filtered.length) {
    return {
      success: false,
      message: "لا توجد بيانات مطابقة لإنشاء النموذج الرسمي",
      month: monthKey,
      half: half
    };
  }

  var masterTemplateSheet = findOfficialTemplateSheet_(ss);
  if (!masterTemplateSheet) {
    return {
      success: false,
      message: "تعذر العثور على شيت القالب الرسمي داخل ملف Google Sheets"
    };
  }

  var halfTag = (half === "all" ? "ALL" : (half === "first" ? "FIRST" : "SECOND"));
  var tempName = "OFFICIAL_" + monthKey + "_" + halfTag + "_" + Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyyMMdd_HHmmss");
  var tempSpreadsheet = SpreadsheetApp.create(tempName);
  var tempId = tempSpreadsheet.getId();

  var copiedSheet = masterTemplateSheet.copyTo(tempSpreadsheet);
  copiedSheet.setName("النموذج الرسمي");
  copiedSheet.setRightToLeft(true);

  var defaultSheets = tempSpreadsheet.getSheets();
  for (var ds = defaultSheets.length - 1; ds >= 0; ds--) {
    var sh = defaultSheets[ds];
    if (sh.getSheetId() !== copiedSheet.getSheetId()) {
      tempSpreadsheet.deleteSheet(sh);
    }
  }

  applyOfficialTemplateHeaderFields_(copiedSheet, {
    demandNo: demandNo,
    reportDate: reportDate,
    routeText: routeText,
    receiverText: receiverText,
    productText: productText
  });

  var layout = detectOfficialTemplateLayout_(copiedSheet);
  if (!layout || !layout.headerRow || !layout.startCol || !layout.totalRow) {
    return {
      success: false,
      message: "تعذر تحديد أماكن الجدول داخل القالب الرسمي. يرجى التأكد من قالب 60"
    };
  }

  var rows = [];
  var totalAccountedQty = 0;
  var totalAmount = 0;

  for (var r = 0; r < filtered.length; r++) {
    var item = filtered[r] || {};
    var grossQty = toNumber_(item.quantity);
    var netQty = toNumber_(item.netQuantity);
    if (netQty <= 0) {
      netQty = roundNumber_(grossQty - (grossQty * DEDUCTION_RATE), 3);
    }

    var source = String(item.source || "");
    var dest = String(item.destination || "");
    var pricePerTon = getPricePerTonBySource_(isFactorySource_(item.sheetName || "", source, dest, ""));
    var amount = roundNumber_(netQty * pricePerTon, 0);

    rows.push([
      r + 1,
      String(item.docNumber || ""),
      String(item.driverName || ""),
      String(item.carNumber || ""),
      formatTemplateDate_(item.loadDate || item.unloadDate || item.timestamp || ""),
      formatTemplateDate_(item.unloadDate || item.loadDate || item.timestamp || ""),
      grossQty,
      grossQty,
      netQty,
      pricePerTon,
      amount,
      0,
      ""
    ]);

    totalAccountedQty += netQty;
    totalAmount += amount;
  }

  fillOfficialTemplateRows_(copiedSheet, layout, rows, totalAmount, totalAccountedQty);

  var xlsxUrl = "https://docs.google.com/spreadsheets/d/" + encodeURIComponent(tempId) + "/export?format=xlsx";
  return {
    success: true,
    message: "تم تجهيز ملف Excel الرسمي بنجاح",
    month: monthKey,
    half: half,
    rows: rows.length,
    templateSheet: masterTemplateSheet.getName(),
    spreadsheetId: tempId,
    spreadsheetUrl: tempSpreadsheet.getUrl(),
    sheetUrl: tempSpreadsheet.getUrl() + "#gid=" + copiedSheet.getSheetId(),
    xlsxUrl: xlsxUrl
  };
}

function findOfficialTemplateSheet_(ss) {
  var preferred = String(OFFICIAL_TEMPLATE_SHEET_NAME || "").trim();
  if (preferred) {
    var exact = ss.getSheetByName(preferred);
    if (exact) return exact;
  }

  var sheets = ss.getSheets();
  var fallback = null;
  for (var i = 0; i < sheets.length; i++) {
    var name = String(sheets[i].getName() || "").trim();
    if (!name) continue;
    if (/^tpl_|^stmt_|^f_\d{4}_\d{2}$/i.test(name)) continue;
    if (/^\d{4}_\d{2}$/i.test(name)) continue;
    if (/60|نموذج|مطالبه|مطالبة|template/i.test(name)) {
      return sheets[i];
    }
    if (!fallback) fallback = sheets[i];
  }

  return fallback;
}

function applyOfficialTemplateHeaderFields_(sheet, data) {
  var maxRows = Math.min(8, Math.max(sheet.getLastRow(), 8));
  var maxCols = Math.min(20, Math.max(sheet.getLastColumn(), 13));
  var rg = sheet.getRange(1, 1, maxRows, maxCols);
  var vals = rg.getValues();

  for (var r = 0; r < vals.length; r++) {
    for (var c = 0; c < vals[r].length; c++) {
      var text = String(vals[r][c] || "").trim();
      if (!text) continue;

      if (text.indexOf("مطالبة رقم") > -1) {
        vals[r][c] = "مطالبة رقم (" + data.demandNo + ")";
      } else if (text.indexOf("الجهة المجهزة") > -1) {
        vals[r][c] = "الجهة المجهزة / " + data.routeText;
      } else if (text.indexOf("الجهة المستلمة") > -1) {
        vals[r][c] = "الجهة المستلمة: " + data.receiverText;
      } else if (text.indexOf("المنتوج") > -1) {
        vals[r][c] = "المنتوج / " + data.productText;
      } else if (text.indexOf("تاريخها") > -1) {
        vals[r][c] = "تاريخها " + data.reportDate;
      }
    }
  }

  rg.setValues(vals);
}

function detectOfficialTemplateLayout_(sheet) {
  var maxRows = Math.min(30, Math.max(sheet.getLastRow(), 10));
  var maxCols = Math.min(30, Math.max(sheet.getLastColumn(), 13));
  var vals = sheet.getRange(1, 1, maxRows, maxCols).getValues();

  var headerRow = 0;
  var startCol = 0;
  for (var r = 0; r < vals.length; r++) {
    for (var c = 0; c < vals[r].length; c++) {
      var t = normalizeText_(vals[r][c]);
      if (t.indexOf("رقممستندالنقلهالاصليه") > -1 || t.indexOf("رقممستندالنقلهالاصلية") > -1) {
        headerRow = r + 1;
        startCol = Math.max(1, c);
        break;
      }
    }
    if (headerRow) break;
  }

  if (!headerRow) return null;

  var totalRow = 0;
  var scanRows = Math.min(Math.max(sheet.getLastRow(), headerRow + 50), 1000);
  var scanVals = sheet.getRange(headerRow + 1, 1, Math.max(1, scanRows - headerRow), maxCols).getValues();
  for (var i = 0; i < scanVals.length; i++) {
    var found = false;
    for (var j = 0; j < scanVals[i].length; j++) {
      var cell = normalizeText_(scanVals[i][j]);
      if (cell.indexOf("مجموع") > -1 && (cell.indexOf("القائمه") > -1 || cell.indexOf("الكمية") > -1 || cell.indexOf("الكميه") > -1)) {
        totalRow = headerRow + 1 + i;
        found = true;
        break;
      }
    }
    if (found) break;
  }

  if (!totalRow) {
    totalRow = headerRow + 40;
  }

  return {
    headerRow: headerRow,
    startCol: startCol,
    totalRow: totalRow
  };
}

function fillOfficialTemplateRows_(sheet, layout, rows, totalAmount, totalQty) {
  var dataStart = layout.headerRow + 1;
  var col = layout.startCol;
  var tableCols = 13;
  var totalRow = layout.totalRow;

  if (totalRow <= dataStart) {
    totalRow = dataStart + 1;
  }

  var availableRows = Math.max(0, totalRow - dataStart);
  var neededRows = rows.length;
  var extraRows = neededRows - availableRows;

  if (extraRows > 0) {
    sheet.insertRowsBefore(totalRow, extraRows);
    totalRow += extraRows;
    if (availableRows > 0) {
      var sourceRange = sheet.getRange(dataStart, col, 1, tableCols);
      for (var i = 0; i < extraRows; i++) {
        sourceRange.copyTo(sheet.getRange(dataStart + availableRows + i, col, 1, tableCols), SpreadsheetApp.CopyPasteType.PASTE_FORMAT, false);
      }
    }
  }

  // Remove unused template rows so the exported sheet does not keep large empty blocks.
  var removableRows = Math.max(0, (totalRow - dataStart) - neededRows);
  if (removableRows > 0) {
    sheet.deleteRows(dataStart + neededRows, removableRows);
    totalRow -= removableRows;
  }

  var clearRows = Math.max(1, totalRow - dataStart);
  sheet.getRange(dataStart, col, clearRows, tableCols).clearContent();

  if (rows.length) {
    sheet.getRange(dataStart, col, rows.length, tableCols).setValues(rows);
    sheet.getRange(dataStart, col + 6, rows.length, 3).setNumberFormat("#,##0.###");
    sheet.getRange(dataStart, col + 9, rows.length, 2).setNumberFormat("#,##0");
  }

  sheet.getRange(totalRow, col + 3).setValue(roundNumber_(totalAmount, 0)).setNumberFormat("#,##0");
  sheet.getRange(totalRow, col + 8).setValue(roundNumber_(totalQty, 3)).setNumberFormat("#,##0.###");
}

function buildMonthlyStatementsSheets(data) {
  var monthKey = resolveMonthKey_(
    (data && data.month) ||
    ((data && data.year && data.monthNumber) ? (String(data.year) + "_" + String(data.monthNumber)) : "")
  );

  if (!monthKey) {
    return {
      success: false,
      message: "month مطلوب بصيغة yyyy_MM"
    };
  }

  var ss = openSpreadsheetWithRetry_();
  var records = getTemplateRecordsFast_(monthKey, ss);
  if (!records || !records.length) {
    return {
      success: false,
      message: "لا توجد بيانات في الشهر المحدد",
      month: monthKey,
      createdSheets: []
    };
  }

  var shouldSplit = true;
  if (data) {
    var splitRaw = String(data.split || data.createSplit || "true").trim().toLowerCase();
    shouldSplit = !(splitRaw === "0" || splitRaw === "false" || splitRaw === "no" || splitRaw === "off");
  }

  var periods = shouldSplit ? ["all", "first", "second"] : [String((data && data.half) || "all").toLowerCase()];
  var createdSheets = [];

  for (var p = 0; p < periods.length; p++) {
    var half = periods[p];
    if (half !== "all" && half !== "first" && half !== "second") {
      half = "all";
    }

    var rows = [];
    var sumQty = 0;
    var sumGross = 0;
    var sumDiscount = 0;
    var sumNet = 0;

    for (var i = 0; i < records.length; i++) {
      var rec = records[i] || {};
      var period = Number(rec.period15 || getPeriodFromDate_(rec.unloadDate || rec.loadDate || rec.timestamp || "") || 0);
      if (half === "first" && period !== 1) continue;
      if (half === "second" && period !== 2) continue;

      var qty = toNumber_(rec.quantity);
      var source = String(rec.source || "");
      var dest = String(rec.destination || "");
      var isFactory = isFactorySource_(rec.sheetName || monthKey, source, dest, "");
      var pricePerTon = getPricePerTonBySource_(isFactory);
      var gross = roundNumber_(qty * pricePerTon, 0);
      var discount = roundNumber_(gross * DEDUCTION_RATE, 0);
      var net = roundNumber_(gross - discount, 0);

      rows.push([
        rows.length + 1,
        String(rec.docNumber || ""),
        String(rec.driverName || ""),
        String(rec.carNumber || ""),
        formatTemplateDate_(rec.unloadDate || rec.loadDate || rec.timestamp || ""),
        qty,
        pricePerTon,
        gross,
        discount,
        net
      ]);

      sumQty += qty;
      sumGross += gross;
      sumDiscount += discount;
      sumNet += net;
    }

    var tag = half === "all" ? "ALL" : (half === "first" ? "FIRST" : "SECOND");
    var sheetName = "STMT_" + monthKey + "_" + tag;
    var sheet = prepareGeneratedSheet_(ss, sheetName, rows.length + 5, 10);
    sheet.setRightToLeft(true);

    sheet.getRange("A1:J1").merge();
    sheet.getRange("A1").setValue("كشف ترحيل شهري - " + monthKey + " - " + (half === "all" ? "كل الشهر" : (half === "first" ? "1 - 15" : "16 - نهاية الشهر")));

    sheet.getRange(2, 1, 1, 10).setValues([[
      "ت",
      "رقم الوصل",
      "اسم السائق",
      "رقم السيارة",
      "تاريخ التفريغ",
      "الكمية (طن)",
      "سعر الطن",
      "القيمة قبل الخصم",
      "قيمة الخصم 18%",
      "القيمة بعد الخصم"
    ]]);

    if (rows.length > 0) {
      sheet.getRange(3, 1, rows.length, 10).setValues(rows);
    }

    var totalRow = rows.length + 3;
    sheet.getRange(totalRow, 1, 1, 5).merge().setValue("الإجمالي");
    sheet.getRange(totalRow, 6).setValue(sumQty);
    sheet.getRange(totalRow, 7).setValue("");
    sheet.getRange(totalRow, 8).setValue(sumGross);
    sheet.getRange(totalRow, 9).setValue(sumDiscount);
    sheet.getRange(totalRow, 10).setValue(sumNet);

    var lastRow = totalRow;
    sheet.getRange(1, 1, lastRow, 10)
      .setHorizontalAlignment("center")
      .setVerticalAlignment("middle")
      .setBorder(true, true, true, true, true, true, "#202124", SpreadsheetApp.BorderStyle.SOLID);

    sheet.getRange("A1:J1").setFontSize(16).setFontWeight("bold").setBackground("#f2f2f2");
    sheet.getRange(2, 1, 1, 10).setFontWeight("bold").setBackground("#efefef");
    sheet.getRange(totalRow, 1, 1, 10).setFontWeight("bold").setBackground("#d9ead3");

    if (rows.length > 0) {
      sheet.getRange(3, 6, rows.length, 1).setNumberFormat("#,##0.###");
      sheet.getRange(3, 7, rows.length, 4).setNumberFormat("#,##0");
    }
    sheet.getRange(totalRow, 6).setNumberFormat("#,##0.###");
    sheet.getRange(totalRow, 8, 1, 3).setNumberFormat("#,##0");

    var widths = [42, 130, 160, 95, 115, 100, 95, 120, 120, 130];
    for (var c = 0; c < widths.length; c++) {
      sheet.setColumnWidth(c + 1, widths[c]);
    }

    sheet.setRowHeight(1, 38);
    sheet.setRowHeight(2, 34);
    if (totalRow >= 3) {
      sheet.setRowHeights(3, totalRow - 2, 30);
    }
    sheet.setFrozenRows(2);

    createdSheets.push({
      sheetName: sheetName,
      half: half,
      rows: rows.length,
      sheetUrl: ss.getUrl() + "#gid=" + sheet.getSheetId(),
      pdfUrl: buildSheetPdfUrl_(ss.getId(), sheet.getSheetId(), sheetName)
    });
  }

  return {
    success: true,
    message: "تم ترحيل بيانات الشهر إلى الكشوفات بنجاح",
    month: monthKey,
    createdSheets: createdSheets
  };
}

function cleanupGeneratedSheets(data) {
  var monthKey = resolveMonthKey_((data && data.month) || "");
  if (!monthKey) {
    return {
      success: false,
      message: "month مطلوب بصيغة yyyy_MM"
    };
  }

  var ss = openSpreadsheetWithRetry_();
  var sheets = ss.getSheets();
  var deleted = [];
  var prefixes = ["TPL_" + monthKey + "_", "STMT_" + monthKey + "_"];

  for (var i = sheets.length - 1; i >= 0; i--) {
    var sh = sheets[i];
    var name = String(sh.getName() || "");
    for (var p = 0; p < prefixes.length; p++) {
      if (name.indexOf(prefixes[p]) === 0) {
        ss.deleteSheet(sh);
        deleted.push(name);
        break;
      }
    }
  }

  deleted.sort();

  return {
    success: true,
    message: deleted.length
      ? "تم حذف الشيتات المولدة للشهر بنجاح"
      : "لا توجد شيتات مولدة للحذف في هذا الشهر",
    month: monthKey,
    deletedSheets: deleted,
    deletedCount: deleted.length
  };
}

function buildOfficialExactTemplate(data) {
  var monthKey = resolveMonthKey_(
    (data && data.month) ||
    ((data && data.year && data.monthNumber) ? (String(data.year) + "_" + String(data.monthNumber)) : "")
  );

  if (!monthKey) {
    return {
      success: false,
      message: "month مطلوب بصيغة yyyy_MM"
    };
  }

  var half = String((data && data.half) || "all").trim().toLowerCase();
  if (half !== "all" && half !== "first" && half !== "second") {
    half = "all";
  }

  var requestedTemplateSheetName = String((data && data.templateSheetName) || "").trim();

  var demandNo = String((data && data.demandNo) || "61").trim();
  var reportDate = String((data && data.reportDate) || Utilities.formatDate(new Date(), "Asia/Baghdad", "yyyy/M/d")).trim();
  var routeText = String((data && data.route) || "محور - حلفاية - بغداد - تاجي").trim();
  var receiverText = String((data && data.receiver) || "شركة تعبئة الغاز").trim();
  var productText = String((data && data.product) || "غاز سائل").trim();

  var ss = openSpreadsheetWithRetry_();
  var templateSheet = null;
  var templateSheetName = "";

  if (requestedTemplateSheetName) {
    templateSheet = ss.getSheetByName(requestedTemplateSheetName);
    if (templateSheet) {
      templateSheetName = requestedTemplateSheetName;
    } else {
      return {
        success: false,
        message: "شيت القالب المطلوب غير موجود: " + requestedTemplateSheetName + "\nقم باستيراد ملف القالب وتسميته بنفس الاسم."
      };
    }
  }

  if (!templateSheet) {
    var configuredTemplateName = String(OFFICIAL_TEMPLATE_SHEET_NAME || "").trim();
    if (configuredTemplateName) {
      templateSheet = ss.getSheetByName(configuredTemplateName);
      if (templateSheet) {
        templateSheetName = configuredTemplateName;
      }
    }
  }

  if (!templateSheet) {
    templateSheet = findOfficialTemplateSheet_(ss);
    if (templateSheet) {
      templateSheetName = String(templateSheet.getName() || "");
    }
  }

  if (!templateSheet) {
    var names = [];
    var sheets = ss.getSheets();
    for (var si = 0; si < sheets.length; si++) {
      names.push(String(sheets[si].getName() || ""));
    }

    return {
      success: false,
      message: "لم يتم العثور على شيت القالب الرسمي.\nيمكنك تمرير templateSheetName صراحة أو ضبط OFFICIAL_TEMPLATE_SHEET_NAME.\nالشيتات الحالية: " + names.join(", ")
    };
  }

  var records = getTemplateRecordsFast_(monthKey, ss);
  var filtered = [];
  for (var i = 0; i < records.length; i++) {
    var rec = records[i] || {};
    var period = Number(rec.period15 || getPeriodFromDate_(rec.unloadDate || rec.loadDate || rec.timestamp || "") || 0);

    if (half === "first" && period !== 1) continue;
    if (half === "second" && period !== 2) continue;

    filtered.push(rec);
  }

  if (!filtered.length) {
    return {
      success: false,
      message: "لا توجد بيانات مطابقة في الشهر/الفترة المحددة",
      month: monthKey,
      half: half
    };
  }

  filtered.sort(function(a, b) {
    var aDoc = Number(a.docNumber || 0);
    var bDoc = Number(b.docNumber || 0);
    if (!isNaN(aDoc) && !isNaN(bDoc) && aDoc !== bDoc) return aDoc - bDoc;
    return String(a.docNumber || "").localeCompare(String(b.docNumber || ""));
  });

  var halfTag = (half === "all" ? "ALL" : (half === "first" ? "FIRST" : "SECOND"));
  var targetSheetName = "OFF_" + monthKey + "_" + halfTag;

  var old = ss.getSheetByName(targetSheetName);
  if (old) ss.deleteSheet(old);

  var target = templateSheet.copyTo(ss).setName(targetSheetName);
  target.setRightToLeft(true);

  // Keep the exact template style; update only content zones.
  target.getRange("A2").setValue("مطالبة رقم (" + demandNo + ")");
  target.getRange("C2").setValue("الجهة المجهزة / " + routeText);
  target.getRange("G2").setValue("تاريخها " + reportDate);
  target.getRange("I2").setValue("الجهة المستلمة: " + receiverText);
  target.getRange("L2").setValue("المنتوج / " + productText);

  var startRow = 4;
  var maxRows = Math.max(500, filtered.length + 20);
  target.getRange(startRow, 1, maxRows, 13).clearContent();

  var rows = [];
  var totalAccountedQty = 0;
  var totalAmount = 0;

  for (var r = 0; r < filtered.length; r++) {
    var item = filtered[r] || {};
    var grossQty = toNumber_(item.quantity);
    var netQty = toNumber_(item.netQuantity);
    if (netQty <= 0) {
      netQty = roundNumber_(grossQty - (grossQty * DEDUCTION_RATE), 3);
    }

    var source = String(item.source || "");
    var dest = String(item.destination || "");
    var isFactory = isFactorySource_(item.sheetName || "", source, dest, "");
    var pricePerTon = getPricePerTonBySource_(isFactory);
    var amount = roundNumber_(netQty * pricePerTon, 0);

    rows.push([
      r + 1,
      String(item.docNumber || ""),
      String(item.driverName || ""),
      String(item.carNumber || ""),
      formatTemplateDate_(item.loadDate || item.unloadDate || item.timestamp || ""),
      formatTemplateDate_(item.unloadDate || item.loadDate || item.timestamp || ""),
      grossQty,
      grossQty,
      netQty,
      pricePerTon,
      amount,
      0,
      ""
    ]);

    totalAccountedQty += netQty;
    totalAmount += amount;
  }

  target.getRange(startRow, 1, rows.length, 13).setValues(rows);

  var totalRow = startRow + rows.length;
  target.getRange(totalRow, 1).setValue("مجموع مبلغ القائمة");
  target.getRange(totalRow, 4).setValue(totalAmount);
  target.getRange(totalRow, 5).setValue("مجموع الكمية المحتسبة =");
  target.getRange(totalRow, 9).setValue(totalAccountedQty);
  target.getRange(totalRow, 10).setValue("ق - (21) ---");

  target.getRange(startRow, 7, rows.length, 3).setNumberFormat("#,##0.###");
  target.getRange(startRow, 10, rows.length, 2).setNumberFormat("#,##0");
  target.getRange(totalRow, 4).setNumberFormat("#,##0");
  target.getRange(totalRow, 9).setNumberFormat("#,##0.###");

  return {
    success: true,
    message: "تم إنشاء النسخة الرسمية المطابقة بنجاح",
    month: monthKey,
    half: half,
    templateSheetName: templateSheetName,
    sheetName: targetSheetName,
    rows: rows.length,
    sheetUrl: ss.getUrl() + "#gid=" + target.getSheetId(),
    pdfUrl: buildSheetPdfUrl_(ss.getId(), target.getSheetId(), targetSheetName),
    xlsxUrl: buildSheetXlsxUrl_(ss.getId(), target.getSheetId(), targetSheetName)
  };
}

function buildSheetPdfUrl_(spreadsheetId, sheetId, fileName) {
  var base = "https://docs.google.com/spreadsheets/d/" + encodeURIComponent(spreadsheetId) + "/export";
  var params = [
    "format=pdf",
    "size=A4",
    "portrait=true",
    "fitw=true",
    "sheetnames=false",
    "printtitle=false",
    "pagenumbers=true",
    "gridlines=false",
    "fzr=true",
    "gid=" + encodeURIComponent(String(sheetId || "")),
    "attachment=true",
    "filename=" + encodeURIComponent(String(fileName || "report"))
  ];
  return base + "?" + params.join("&");
}

function buildSheetXlsxUrl_(spreadsheetId, sheetId, fileName) {
  var base = "https://docs.google.com/spreadsheets/d/" + encodeURIComponent(spreadsheetId) + "/export";
  var params = [
    "format=xlsx",
    "gid=" + encodeURIComponent(String(sheetId || "")),
    "attachment=true",
    "filename=" + encodeURIComponent(String(fileName || "report"))
  ];
  return base + "?" + params.join("&");
}

function formatTemplateDate_(value) {
  if (Object.prototype.toString.call(value) === "[object Date]" && !isNaN(value)) {
    return Utilities.formatDate(value, "Asia/Baghdad", "M/d/yyyy");
  }

  var text = String(value || "").trim();
  if (!text) return "";

  var day = extractDayFromDate_(text);
  if (!day) return text;

  var parsed = parseDateParts_(text);
  if (parsed) {
    return parsed.month + "/" + parsed.day + "/" + parsed.year;
  }

  var dt = new Date(text);
  if (!isNaN(dt.getTime())) {
    return Utilities.formatDate(dt, "Asia/Baghdad", "M/d/yyyy");
  }

  return text;
}

function json(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function exportDemandTemplate(data) {
  var monthKey = resolveMonthKey_(data && data.month ? data.month : buildMonthKey_(new Date()));
  var half = String((data && data.half) || "all").trim().toLowerCase();
  var issueDate = String((data && data.issueDate) || "").trim();
  var demandNo = String((data && data.demandNo) || "61").trim();
  var routeTitle = String((data && data.routeTitle) || "محور / حلفاية - بغداد - تاجي").trim();
  var receiverTitle = String((data && data.receiverTitle) || "شركة تعبئة الغاز").trim();
  var productTitle = String((data && data.productTitle) || "غاز سائل").trim();

  if (!monthKey) {
    return {
      success: false,
      message: "month is required"
    };
  }

  var ss = openSpreadsheetWithRetry_();
  var rows = getTemplateRecordsFast_(monthKey, ss);
  var filtered = [];

  for (var i = 0; i < rows.length; i++) {
    var row = rows[i] || {};
    var isFactory = isFactorySource_(
      String(row.sheetName || ""),
      String(row.source || ""),
      String(row.destination || ""),
      String(row.factory || "")
    );

    // هذا القالب مخصص لقائمة الحلفاية كما في النموذج المرسل.
    if (isFactory) continue;

    var rowDay = extractDayFromDate_(row.unloadDate || row.loadDate || row.timestamp || "");
    if (half === "first" && rowDay && rowDay > 15) continue;
    if (half === "second" && rowDay && rowDay < 16) continue;

    filtered.push(row);
  }

  filtered.sort(function(a, b) {
    return String(a.timestamp || "").localeCompare(String(b.timestamp || ""));
  });

  if (!filtered.length) {
    return {
      success: false,
      message: "لا توجد بيانات مطابقة لإنشاء القالب الرسمي",
      month: monthKey,
      half: half || "all"
    };
  }

  var sheetName = "TPL_" + monthKey + "_" + (half || "all");
  var sheet = prepareGeneratedSheet_(ss, sheetName, filtered.length + 6, 13);
  sheet.setRightToLeft(true);

  var cols = 13;
  var titleRow = 1;
  var metaRow = 2;
  var headerRow = 3;
  var startDataRow = 4;

  sheet.getRange(titleRow, 1, 1, cols).merge();
  sheet.getRange(titleRow, 1).setValue("كشف بالكميات المنقولة بواسطة السيارات العاملة بمعية شركة الناقلات النموذجية");

  // صف العناوين العلوية (مطابق بصريًا للنموذج).
  sheet.getRange(metaRow, 1).setValue("ت");
  sheet.getRange(metaRow, 2).setValue("مطالبة رقم (" + demandNo + ")");
  sheet.getRange(metaRow, 3, 1, 2).merge().setValue("الجهة المجهزة / " + routeTitle);
  sheet.getRange(metaRow, 5).setValue("تاريخها " + (issueDate || "-"));
  sheet.getRange(metaRow, 6, 1, 2).merge().setValue("الجهة المستلمة: " + receiverTitle);
  sheet.getRange(metaRow, 8, 1, 2).merge().setValue("المنتوج / " + productTitle);
  sheet.getRange(metaRow, 10, 1, 4).merge().setValue("");

  var headers = [
    "ت",
    "رقم مستند النقلة الاصلية",
    "اسم السائق",
    "رقم السيارة",
    "تاريخ التحميل",
    "تاريخ التفريغ",
    "الكمية المحملة",
    "الكمية المفرغة",
    "الكمية المحتسبة",
    "سعر النقلة الاصلية / طن",
    "مبلغ النقلة الاصلية",
    "كمية النقص",
    "ملاحظات"
  ];

  sheet.getRange(headerRow, 1, 1, cols).setValues([headers]);

  var values = [];
  var totalNetQty = 0;
  var totalAmount = 0;

  for (var r = 0; r < filtered.length; r++) {
    var it = filtered[r] || {};
    var grossQty = toNumber_(it.quantity);
    var netQty = roundNumber_(grossQty - (grossQty * DEDUCTION_RATE), 3);
    var pricePerTon = PRICE_PER_TON_HALAFAYA;
    var amount = roundNumber_(grossQty * pricePerTon, 0);

    totalNetQty += netQty;
    totalAmount += amount;

    values.push([
      r + 1,
      String(it.docNumber || ""),
      String(it.driverName || ""),
      String(it.carNumber || ""),
      toDisplayDate_(it.loadDate || it.timestamp || ""),
      toDisplayDate_(it.unloadDate || it.timestamp || ""),
      grossQty,
      grossQty,
      netQty,
      pricePerTon,
      amount,
      0,
      String(it.notes || "")
    ]);
  }

  if (values.length > 0) {
    sheet.getRange(startDataRow, 1, values.length, cols).setValues(values);
  }

  var totalRow = startDataRow + values.length;
  sheet.getRange(totalRow, 1, 1, 8).merge().setValue("مجموع الكمية المحتسبة =");
  sheet.getRange(totalRow, 9).setValue(roundNumber_(totalNetQty, 3));
  sheet.getRange(totalRow, 10).setValue("مجموع مبلغ القائمة");
  sheet.getRange(totalRow, 11).setValue(roundNumber_(totalAmount, 0));
  sheet.getRange(totalRow, 12, 1, 2).merge().setValue("");

  // تنسيقات.
  sheet.getRange(titleRow, 1, 1, cols)
    .setFontSize(18)
    .setFontWeight("bold")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle")
    .setBackground("#f2f2f2");

  sheet.getRange(metaRow, 1, 1, cols)
    .setFontWeight("bold")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle")
    .setBackground("#fafafa");

  sheet.getRange(headerRow, 1, 1, cols)
    .setFontWeight("bold")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle")
    .setBackground("#e6e6e6");

  if (values.length > 0) {
    sheet.getRange(startDataRow, 1, values.length, cols)
      .setHorizontalAlignment("center")
      .setVerticalAlignment("middle");
  }

  sheet.getRange(totalRow, 1, 1, cols)
    .setFontWeight("bold")
    .setBackground("#d9ead3")
    .setHorizontalAlignment("center")
    .setVerticalAlignment("middle");

  var lastRow = totalRow;
  sheet.getRange(1, 1, lastRow, cols).setBorder(true, true, true, true, true, true);

  sheet.getRange(startDataRow, 7, values.length || 1, 3).setNumberFormat("#,##0.000");
  sheet.getRange(startDataRow, 10, values.length || 1, 2).setNumberFormat("#,##0");
  sheet.getRange(totalRow, 9).setNumberFormat("#,##0.000");
  sheet.getRange(totalRow, 11).setNumberFormat("#,##0");

  var widths = [40, 120, 150, 80, 95, 95, 90, 90, 90, 105, 130, 80, 130];
  for (var c = 0; c < widths.length; c++) {
    sheet.setColumnWidth(c + 1, widths[c]);
  }

  sheet.setRowHeight(titleRow, 42);
  sheet.setRowHeight(metaRow, 30);
  sheet.setRowHeight(headerRow, 34);
  sheet.setFrozenRows(3);

  return {
    success: true,
    message: "تم إنشاء ورقة القالب بنجاح",
    sheetName: sheetName,
    month: monthKey,
    half: half || "all",
    rows: values.length,
    totalNetQuantity: roundNumber_(totalNetQty, 3),
    totalOriginalAmount: roundNumber_(totalAmount, 0)
  };
}

function toDisplayDate_(value) {
  if (Object.prototype.toString.call(value) === "[object Date]" && !isNaN(value)) {
    return Utilities.formatDate(value, "Asia/Baghdad", "M/d/yyyy");
  }

  var parsed = parseDateParts_(value);
  if (parsed) {
    return parsed.month + "/" + parsed.day + "/" + parsed.year;
  }

  var asDate = new Date(String(value || ""));
  if (!isNaN(asDate.getTime())) {
    return Utilities.formatDate(asDate, "Asia/Baghdad", "M/d/yyyy");
  }

  return String(value || "");
}

function getTemplateRecordsFast_(monthKey, ss) {
  ss = ss || openSpreadsheetWithRetry_();
  var tripSheet = ss.getSheetByName(monthKey);
  if (!tripSheet) return [];

  var values = tripSheet.getDataRange().getValues();
  if (!values || values.length <= 1) return [];

  var header = values[0] || [];
  var hasHeader = isLikelyHeaderRow_(header);
  var colMap = hasHeader ? buildColumnMap_(header) : {};
  var startRow = hasHeader ? 1 : 0;

  var result = [];

  for (var i = startRow; i < values.length; i++) {
    var row = values[i] || [];
    if (!row.length) continue;

    var doc = String(getCellByAliases_(row, colMap, ["docnumber", "doc", "document", "receipt", "رقمالوصل"], 0) || "").trim();
    var drv = String(getCellByAliases_(row, colMap, ["drivername", "driver", "name", "السائق", "اسمالسائق"], 1) || "").trim();
    if (!doc && !drv) continue;

    var qty = toNumber_(getCellByAliases_(row, colMap, ["quantity", "qty", "الكمية"], 5));
    var loadDate = String(getCellByAliases_(row, colMap, ["loaddate", "load", "date", "تاريخالتحميل"], 3) || "");
    var unloadDate = String(getCellByAliases_(row, colMap, ["unloaddate", "unload", "تاريخالتفريغ", "filldate", "fill_date"], 4) || "");
    var ts = String(getCellByAliases_(row, colMap, ["sendtime", "timestamp", "createdat", "created_at", "وقتالارسال"], 9) || "");

    result.push({
      docNumber: doc,
      driverName: drv,
      carNumber: String(getCellByAliases_(row, colMap, ["carnumber", "car", "vehicle", "رقمالسيارة"], 2) || ""),
      loadDate: loadDate,
      unloadDate: unloadDate,
      timestamp: ts,
      quantity: qty,
      netQuantity: roundNumber_(qty - (qty * DEDUCTION_RATE), 3),
      destination: String(getCellByAliases_(row, colMap, ["station", "destination", "المحطة", "الوجهة"], 7) || ""),
      source: String(getCellByAliases_(row, colMap, ["source", "type", "rowtype"], -1) || ""),
      sheetName: monthKey,
      notes: String(getCellByAliases_(row, colMap, ["notes", "note", "remarks", "ملاحظات"], 14) || ""),
      period15: getPeriodFromDate_(unloadDate || loadDate || ts)
    });
  }

  return result;
}
