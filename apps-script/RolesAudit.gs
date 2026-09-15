/**
 * Roles + Audit Log + trip lifecycle helpers.
 * Additive only — does not wipe existing sheets or change driver write paths.
 */

var SYSTEM_ROLES_ = [
  { id: "driver", name: "سائق", canWriteTrips: true, canSeeAllClients: false },
  { id: "client", name: "معتمد/عميل", canWriteTrips: false, canSeeAllClients: false },
  { id: "owner", name: "مالك سيارة", canWriteTrips: false, canSeeAllClients: false },
  { id: "accountant", name: "محاسب", canWriteTrips: false, canSeeAllClients: true },
  { id: "admin", name: "إدارة", canWriteTrips: true, canSeeAllClients: true }
];

var TRIP_STATUSES_ = [
  "جديد",
  "تحميل",
  "نقل",
  "وصول",
  "تفريغ",
  "مكتمل",
  "ملغى",
  "يحتاج مراجعة",
  "موقوف"
];

function listSystemRoles_() {
  return { success: true, data: SYSTEM_ROLES_ };
}

function getTripStatusCatalog_() {
  return { success: true, data: TRIP_STATUSES_ };
}

function ensureRoleReferenceSheets_(data) {
  var ss = getCompanySpreadsheet_();
  ensureNamedSheetWithHeaders_(ss, "الأدوار", ["المعرف", "الاسم", "يكتب_نقلات", "يرى_كل_العملاء"]);
  ensureNamedSheetWithHeaders_(ss, "سجل_التعديلات", [
    "الوقت", "المستخدم", "الدور", "الإجراء", "الهدف", "التفاصيل", "القيمة_القديمة", "القيمة_الجديدة", "السبب"
  ]);
  ensureNamedSheetWithHeaders_(ss, "حالات_النقلات", ["الحالة", "الترتيب", "نشط"]);

  var rolesSheet = ss.getSheetByName("الأدوار");
  if (rolesSheet.getLastRow() < 2) {
    for (var i = 0; i < SYSTEM_ROLES_.length; i++) {
      var r = SYSTEM_ROLES_[i];
      rolesSheet.appendRow([r.id, r.name, r.canWriteTrips ? 1 : 0, r.canSeeAllClients ? 1 : 0]);
    }
  }

  var statusSheet = ss.getSheetByName("حالات_النقلات");
  if (statusSheet.getLastRow() < 2) {
    for (var s = 0; s < TRIP_STATUSES_.length; s++) {
      statusSheet.appendRow([TRIP_STATUSES_[s], s + 1, 1]);
    }
  }

  appendAuditLog_({
    actor: (data && data.actor) || "system",
    role: "إدارة",
    action: "ensureRoleSheets",
    target: ss.getName(),
    detail: "تهيئة أوراق الأدوار وسجل التعديلات وحالات النقلات"
  });

  return {
    success: true,
    message: "تم التأكد من أوراق الأدوار وسجل التعديلات",
    sheets: ["الأدوار", "سجل_التعديلات", "حالات_النقلات"]
  };
}

function ensureNamedSheetWithHeaders_(ss, name, headers) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
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

function appendAuditLog_(data) {
  data = data || {};
  var ss = getCompanySpreadsheet_();
  var sheet = ensureNamedSheetWithHeaders_(ss, "سجل_التعديلات", [
    "الوقت", "المستخدم", "الدور", "الإجراء", "الهدف", "التفاصيل", "القيمة_القديمة", "القيمة_الجديدة", "السبب"
  ]);
  sheet.appendRow([
    new Date(),
    String(data.actor || ""),
    String(data.role || ""),
    String(data.action || ""),
    String(data.target || ""),
    String(data.detail || ""),
    String(data.oldValue || ""),
    String(data.newValue || ""),
    String(data.reason || "")
  ]);
  return { success: true };
}

function listAuditLog_(data) {
  data = data || {};
  var ss = getCompanySpreadsheet_();
  var sheet = ss.getSheetByName("سجل_التعديلات");
  if (!sheet || sheet.getLastRow() < 2) return { success: true, data: [] };
  var values = sheet.getDataRange().getValues();
  var out = [];
  var limit = Math.min(values.length - 1, Number(data.limit || 200));
  for (var i = values.length - 1; i >= 1 && out.length < limit; i--) {
    var row = values[i];
    out.push({
      time: row[0],
      actor: row[1],
      role: row[2],
      action: row[3],
      target: row[4],
      detail: row[5],
      oldValue: row[6],
      newValue: row[7],
      reason: row[8]
    });
  }
  return { success: true, data: out };
}

function setTripLifecycleStatus_(data) {
  data = data || {};
  var status = String(data.status || "").trim();
  if (TRIP_STATUSES_.indexOf(status) < 0) {
    return { success: false, message: "حالة غير معروفة: " + status };
  }
  // Non-destructive stub: records intent in audit log.
  // Full row mutation is deferred until live mapping of trip rows is approved.
  appendAuditLog_({
    actor: data.actor || "admin",
    role: data.role || "إدارة",
    action: "setTripStatus",
    target: String(data.docNumber || data.target || ""),
    oldValue: String(data.oldStatus || ""),
    newValue: status,
    reason: String(data.reason || "تحديث حالة النقلة"),
    detail: "مسجّل في سجل التعديلات — تطبيق الصف يتطلب موافقة نشر لاحقة"
  });
  return {
    success: true,
    message: "تم تسجيل طلب الحالة في سجل التعديلات (بدون تعديل صفوف النقل الحية بعد)",
    status: status
  };
}

function getAdminDashboardSummary_(data) {
  data = data || {};
  var ss = getCompanySpreadsheet_();
  var agents = [];
  try { agents = readAgents_(ss) || []; } catch (err) { agents = []; }
  var fleet = [];
  try { fleet = readFleet_(ss) || []; } catch (err2) { fleet = []; }
  var dbCount = 0;
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    if (/^DB_/.test(sheets[i].getName())) dbCount++;
  }
  return {
    success: true,
    data: {
      agents: agents.length,
      fleet: fleet.length,
      clientDatabases: dbCount,
      roles: SYSTEM_ROLES_.length,
      tripStatuses: TRIP_STATUSES_.length,
      generatedAt: nowBaghdad_()
    }
  };
}
