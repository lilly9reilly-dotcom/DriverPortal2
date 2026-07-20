const DRIVER_RESET_FLAG = 'companyDriversResetToAliV1';
const STATION_TRIP_RATE = 35500;
const FACTORY_TRIP_RATE = 8500;
const APP_VERSION_CODE = 105;
const APP_VERSION_NAME = '1.0.5';
const SINGLE_DRIVER = {
  id: 'driver-ali-ghanem-alnas',
  name: 'علي غانم الناس',
  phone: '',
  carNumber: '20201',
  fare: 0
};

function normalizeCompanyIdToken(value) {
  return String(value || '').trim().toUpperCase().replace(/\s+/g, '');
}

function getActiveCompanyScopeId() {
  return normalizeCompanyIdToken(state && state.activationScope && state.activationScope.companyId) || 'COMP-001';
}

function scopedStorageKey(base, companyId) {
  return `${String(base || '').trim()}::${normalizeCompanyIdToken(companyId) || getActiveCompanyScopeId()}`;
}

function readJsonStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function stripCompanyMarker(noteText) {
  return String(noteText || '')
    .replace(/\[CID\s*:\s*[A-Z0-9_-]{2,40}\]/ig, '')
    .replace(/\[ACT\s*:\s*[^\]]{3,80}\]/ig, '')
    .trim();
}

function buildCompanyScopedNotes(noteText) {
  const cid = getActiveCompanyScopeId();
  const act = String(state && state.activationCode || '').trim();
  const cleaned = stripCompanyMarker(noteText);
  const marker = `[CID:${cid}]${act ? ` [ACT:${act}]` : ''}`;
  return `${marker}${cleaned ? ` ${cleaned}` : ''}`;
}

function extractCompanyIdFromNotes(noteText) {
  const m = String(noteText || '').match(/\[CID\s*:\s*([A-Z0-9_-]{2,40})\]/i);
  return m ? normalizeCompanyIdToken(m[1]) : '';
}

function extractActivationCodeFromNotes(noteText) {
  const m = String(noteText || '').match(/\[ACT\s*:\s*([^\]]{3,80})\]/i);
  return m ? String(m[1] || '').trim() : '';
}

function normalizeDriverRecord(driver) {
  return {
    id: String(driver && driver.id ? driver.id : Date.now()),
    name: String(driver && driver.name ? driver.name : '').trim(),
    phone: String(driver && driver.phone ? driver.phone : '').trim(),
    carNumber: String(driver && driver.carNumber ? driver.carNumber : '').trim(),
    fare: 0
  };
}

function loadDriversFromStorage() {
  const driversKey = scopedStorageKey('companyDrivers');
  const resetFlagKey = scopedStorageKey(DRIVER_RESET_FLAG);
  const rows = readJsonStorage(driversKey, []);
  const list = Array.isArray(rows) ? rows.map(normalizeDriverRecord) : [];
  if (!localStorage.getItem(resetFlagKey)) {
    localStorage.setItem(resetFlagKey, '1');
    localStorage.setItem(driversKey, JSON.stringify([]));
    return [];
  }
  return list;
}

const state = {
  bootstrap: null,
  activationScope: {
    appKey: 'company',
    companyId: 'COMP-001',
    packageName: 'com.driver.portal.company.desktop'
  },
  activationCode: '',
  activationAllowed: false,
  manager: null,
  authenticated: false,
  drivers: [SINGLE_DRIVER],
  maintenance: [],
  sentDocs: [],
  tripEdits: {},
  auditLog: [],
  pendingSyncEdits: {},
  months: [],
  month: '',
  trips: [],
  ui: {
    tripSubmitting: false,
    factorySubmitting: false,
    editTripIdentity: '',
    editDriverId: '',
    auditVisible: false,
    serverEditDisabled: false
  }
};

function $(id) { return document.getElementById(id); }

function loadScopedStateFromStorage() {
  state.activationCode = localStorage.getItem(scopedStorageKey('companyActivationCode')) || '';
  state.manager = readJsonStorage(scopedStorageKey('companyManager'), null);
  state.drivers = loadDriversFromStorage();
  state.maintenance = readJsonStorage(scopedStorageKey('companyMaintenance'), []);
  state.sentDocs = readJsonStorage(scopedStorageKey('companySentDocs'), []);
  state.tripEdits = readJsonStorage(scopedStorageKey('companyTripEdits'), {});
  state.auditLog = readJsonStorage(scopedStorageKey('companyAuditLog'), []);
  state.pendingSyncEdits = readJsonStorage(scopedStorageKey('companyPendingSyncEdits'), {});
}

function saveState() {
  localStorage.setItem(scopedStorageKey('companyDrivers'), JSON.stringify(state.drivers || []));
  localStorage.setItem(scopedStorageKey('companyMaintenance'), JSON.stringify(state.maintenance || []));
  localStorage.setItem(scopedStorageKey('companySentDocs'), JSON.stringify(state.sentDocs || []));
  localStorage.setItem(scopedStorageKey('companyTripEdits'), JSON.stringify(state.tripEdits || {}));
  localStorage.setItem(scopedStorageKey('companyAuditLog'), JSON.stringify(state.auditLog || []));
  localStorage.setItem(scopedStorageKey('companyPendingSyncEdits'), JSON.stringify(state.pendingSyncEdits || {}));
  localStorage.setItem(scopedStorageKey('companyActivationCode'), state.activationCode || '');
  if (state.manager) localStorage.setItem(scopedStorageKey('companyManager'), JSON.stringify(state.manager));
}
function esc(v) { return String(v || '').replace(/[&<>\"]/g, (m) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[m])); }
function num(v) { const n = Number(String(v || '').replace(/,/g, '')); return Number.isFinite(n) ? n : 0; }
function normalizeCarNumber(value) { return String(value || '').trim().replace(/\s+/g, '').toUpperCase(); }
function normalizePersonName(value) { return String(value || '').trim().replace(/\s+/g, ' '); }
function isValidDocNumber(value) {
  const doc = normalizeDocNumber(value);
  if (!doc) return false;
  if (doc.length < 2 || doc.length > 24) return false;
  return /^[\w\-\/]+$/i.test(doc);
}
function monthKeyFromDate(value) {
  if (!value) return '';
  const s = String(value).replace(/\//g, '-');
  const m = s.match(/(\d{4})-(\d{1,2})/);
  if (!m) return '';
  return `${m[1]}_${String(m[2]).padStart(2, '0')}`;
}
async function api(action, data) {
  const payload = { ...(data || {}) };
  const companyId = getActiveCompanyScopeId();
  const deviceId = getDeviceId();
  if (companyId && !payload.companyId) payload.companyId = companyId;
  if (state.activationCode && action !== 'companyActivationVerify' && !payload.activationCode) {
    payload.activationCode = state.activationCode;
  }
  if (deviceId && !payload.deviceId) payload.deviceId = deviceId;
  if (state.activationScope && state.activationScope.packageName && !payload.packageName) {
    payload.packageName = state.activationScope.packageName;
  }
  if (state.activationScope && state.activationScope.appKey && !payload.appKey) {
    payload.appKey = state.activationScope.appKey;
  }
  const res = await window.managerDesktop.apiRequest(action, payload);
  if (!res || res.success === false) throw new Error((res && res.message) || `API ${action} failed`);
  return res;
}

function setInlineMessage(targetId, message, type = 'info') {
  const el = $(targetId);
  if (!el) return;
  el.textContent = String(message || '');
  el.classList.remove('msg-success', 'msg-error', 'msg-info');
  if (type === 'success') el.classList.add('msg-success');
  else if (type === 'error') el.classList.add('msg-error');
  else el.classList.add('msg-info');
}

function setButtonBusy(buttonId, isBusy) {
  const el = $(buttonId);
  if (!el) return;
  el.disabled = !!isBusy;
  el.style.opacity = isBusy ? '0.65' : '1';
  el.style.cursor = isBusy ? 'wait' : 'pointer';
}

function getTripIdentity(record) {
  const doc = normalizeDocNumber(record && record.docNumber);
  const car = normalizeCarNumber(record && record.carNumber);
  const when = String(record && (record.unloadDate || record.loadDate || record.sendTime || record.timestamp || '')).trim();
  const source = isFactoryTripRecord(record) ? 'factory' : 'station';
  return `${doc}__${car}__${when}__${source}`;
}

function cloneAuditPayload(record) {
  return {
    docNumber: String(record && record.docNumber || ''),
    carNumber: String(record && record.carNumber || ''),
    driverName: String(record && record.driverName || ''),
    destination: getRecordDestination(record),
    quantity: num(record && (record.netQuantity || record.quantity)),
    fare: num(record && (record.kroa || record.driverFare)),
    loadDate: String(record && record.loadDate || ''),
    unloadDate: String(record && record.unloadDate || ''),
    notes: stripCompanyMarker(record && record.notes)
  };
}

function appendAudit(action, identity, beforeRecord, afterRecord) {
  const entry = {
    id: String(Date.now()),
    at: new Date().toISOString(),
    actor: state.manager && state.manager.name ? state.manager.name : 'غير محدد',
    action: String(action || 'update'),
    identity: String(identity || ''),
    before: cloneAuditPayload(beforeRecord),
    after: cloneAuditPayload(afterRecord)
  };
  state.auditLog = Array.isArray(state.auditLog) ? state.auditLog : [];
  state.auditLog.unshift(entry);
  if (state.auditLog.length > 500) state.auditLog = state.auditLog.slice(0, 500);
  saveState();
}

function isAuditInRange(isoText, rangeKey) {
  if (!isoText || !rangeKey || rangeKey === 'all') return true;
  const at = new Date(String(isoText));
  if (Number.isNaN(at.getTime())) return true;
  const now = new Date();
  const ms = now.getTime() - at.getTime();
  if (rangeKey === 'today') return ms <= 24 * 60 * 60 * 1000;
  if (rangeKey === 'week') return ms <= 7 * 24 * 60 * 60 * 1000;
  if (rangeKey === 'month') return ms <= 30 * 24 * 60 * 60 * 1000;
  return true;
}

async function syncTripEditToServer(identity, baseRecord, patch) {
  if (state.ui.serverEditDisabled) {
    return { success: false, message: 'تعطلت مزامنة التعديل بسبب عدم دعم الخادم.', hardFail: true };
  }

  const payload = {
    identity,
    month: state.month || monthKeyFromDate(new Date().toISOString()),
    docNumber: String(baseRecord && baseRecord.docNumber || ''),
    carNumber: String(baseRecord && baseRecord.carNumber || ''),
    driverName: String(baseRecord && baseRecord.driverName || ''),
    source: isFactoryTripRecord(baseRecord) ? 'factory' : 'trip',
    patch: JSON.stringify(patch || {})
  };

  try {
    const res = await api('updateReceipt', payload);
    return { success: !!(res && res.success !== false), message: (res && res.message) || 'تمت مزامنة التعديل مع الخادم.' };
  } catch (e) {
    const message = String(e && (e.message || e) || 'فشل مزامنة التعديل');
    const lower = message.toLowerCase();
    const hardFail = lower.includes('unknown action') || lower.includes('not allowed');
    if (hardFail) state.ui.serverEditDisabled = true;
    return { success: false, message, hardFail };
  }
}

async function flushPendingSyncEdits() {
  if (state.ui.serverEditDisabled) return;
  const pending = state.pendingSyncEdits || {};
  const keys = Object.keys(pending);
  if (!keys.length) return;

  for (const identity of keys) {
    const item = pending[identity];
    if (!item || !item.baseRecord || !item.patch) continue;
    const result = await syncTripEditToServer(identity, item.baseRecord, item.patch);
    if (result.success) {
      delete state.pendingSyncEdits[identity];
    } else if (result.hardFail) {
      break;
    }
  }
  saveState();
}

function applyTripEdits(rows) {
  const list = Array.isArray(rows) ? rows : [];
  return list.map((row) => {
    const identity = getTripIdentity(row);
    const patch = state.tripEdits && state.tripEdits[identity];
    if (!patch) return row;
    return { ...row, ...patch };
  });
}

async function sha256Hex(value) {
  const data = new TextEncoder().encode(String(value || ''));
  const hash = await crypto.subtle.digest('SHA-256', data);
  return Array.from(new Uint8Array(hash)).map((b) => b.toString(16).padStart(2, '0')).join('');
}

function getDeviceId() {
  const key = 'companyDeviceId';
  let id = localStorage.getItem(key);
  if (!id) {
    const rand = Math.random().toString(36).slice(2, 10).toUpperCase();
    id = `desktop-${Date.now()}-${rand}`;
    localStorage.setItem(key, id);
  }
  return id;
}

function showGate(id) {
  ['activationGate', 'managerSetup', 'managerLogin', 'dashboard'].forEach((x) => $(x).classList.add('hidden'));
  const authExperience = $('authExperience');
  const mainWelcome = $('mainWelcome');
  if (id === 'dashboard') {
    authExperience && authExperience.classList.add('hidden');
    mainWelcome && mainWelcome.classList.remove('hidden');
  } else {
    authExperience && authExperience.classList.remove('hidden');
    mainWelcome && mainWelcome.classList.add('hidden');
  }
  $(id).classList.remove('hidden');
}

function updateAuthDateTime() {
  const clockEl = $('authClock');
  const dateEl = $('authDate');
  if (!clockEl || !dateEl) return;
  const now = new Date();
  clockEl.textContent = now.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true
  });
  dateEl.textContent = now.toLocaleDateString('en-GB');
}

async function verifyActivation() {
  const code = $('activationCode').value.trim();
  if (!code) { $('activationMsg').textContent = 'أدخل كود التفعيل'; return; }
  if (!state.activationScope.companyId) {
    $('activationMsg').textContent = 'النسخة غير مهيأة: يجب ضبط companyId في manager-config.json';
    return;
  }
  $('activationMsg').textContent = 'جاري التحقق...';
  try {
    const res = await api('companyActivationVerify', {
      activationCode: code,
      appKey: state.activationScope.appKey,
      companyId: state.activationScope.companyId,
      packageName: state.activationScope.packageName,
      versionName: APP_VERSION_NAME,
      deviceId: getDeviceId()
    });
    if (!res.allowed) throw new Error(res.message || 'كود التفعيل غير صالح أو غير مفعل');
    state.activationCode = code;
    state.activationAllowed = true;
    saveState();
    $('activationMsg').textContent = 'تم التفعيل بنجاح';
    routeAfterActivation();
  } catch (e) {
    const msg = String(e.message || e);
    $('activationMsg').textContent = 'فشل التفعيل: ' + msg;
  }
}

function routeAfterActivation() {
  if (!state.manager) {
    showGate('managerSetup');
  } else {
    applyManagerIdentityToUi();
    showGate('managerLogin');
  }
}

function applyManagerIdentityToUi() {
  const managerName = state.manager && state.manager.name ? state.manager.name : '-';
  if ($('managerNameLabel')) {
    $('managerNameLabel').textContent = 'المدير: ' + managerName;
  }
  if ($('activeManager')) {
    $('activeManager').textContent = `مدير الحسابات: ${managerName}`;
  }
}

function closeManagerEditModal() {
  const modal = $('managerEditModal');
  if (modal) modal.classList.add('hidden');
  setInlineMessage('managerEditMsg', '', 'info');
}

function openManagerEditModal() {
  if (!state.manager || !state.authenticated) {
    window.alert('يجب تسجيل الدخول أولاً لتعديل اسم المدير.');
    return;
  }

  $('editManagerName').value = String(state.manager.name || '').trim();
  setInlineMessage('managerEditMsg', 'قم بتعديل الاسم ثم احفظ التغيير.', 'info');
  const modal = $('managerEditModal');
  if (modal) modal.classList.remove('hidden');
}

async function revalidateActivationAccess() {
  if (!state.activationCode) {
    state.activationAllowed = false;
    return false;
  }

  try {
    const res = await api('companyActivationVerify', {
      activationCode: state.activationCode,
      appKey: state.activationScope.appKey,
      companyId: state.activationScope.companyId,
      packageName: state.activationScope.packageName,
      versionName: APP_VERSION_NAME,
      deviceId: getDeviceId()
    });

    state.activationAllowed = !!res.allowed;
    if (!state.activationAllowed) {
      $('activationMsg').textContent = res.message || 'تم إيقاف كود التفعيل. لا يمكن الدخول حتى إعادة التفعيل من الإدارة.';
      state.authenticated = false;
      showGate('activationGate');
      saveState();
      return false;
    }

    return true;
  } catch (e) {
    const msg = String(e && (e.message || e) || 'فشل التحقق من التفعيل');
    $('loginMsg').textContent = 'تعذر التحقق من صلاحية الكود: ' + msg;
    return false;
  }
}

async function setupManager() {
  const name = $('setupName').value.trim();
  const phone = $('setupPhone').value.trim();
  const pin = $('setupPin').value.trim();
  const pin2 = $('setupPin2').value.trim();
  if (!state.activationAllowed) { $('setupMsg').textContent = 'يجب التفعيل أولاً.'; return; }
  if (!name || !pin || pin.length < 4) { $('setupMsg').textContent = 'البيانات غير مكتملة'; return; }
  if (pin !== pin2) { $('setupMsg').textContent = 'تأكيد PIN غير مطابق'; return; }
  const pinHash = await sha256Hex(pin);
  state.manager = { name, phone, pinHash };
  state.authenticated = true;
  saveState();
  applyManagerIdentityToUi();
  initDashboard();
}

function updateManagerName() {
  if (!state.manager || !state.authenticated) {
    window.alert('يجب تسجيل الدخول أولاً لتعديل اسم المدير.');
    return;
  }

  const nextName = normalizePersonName($('editManagerName').value);
  if (!nextName) {
    setInlineMessage('managerEditMsg', 'اسم المدير مطلوب.', 'error');
    return;
  }

  state.manager.name = nextName;
  saveState();
  applyManagerIdentityToUi();
  setInlineMessage('managerEditMsg', 'تم تحديث اسم المدير بنجاح.', 'success');
  closeManagerEditModal();
}

async function managerLogin() {
  const activationOk = await revalidateActivationAccess();
  if (!activationOk) {
    return;
  }

  const pin = $('loginPin').value.trim();
  if (!state.manager) {
    $('loginMsg').textContent = 'لا يوجد مدير محفوظ';
    return;
  }
  const pinHash = await sha256Hex(pin);
  const storedHash = state.manager.pinHash || '';
  const legacyPin = state.manager.pin || '';
  const ok = (storedHash && pinHash === storedHash) || (!storedHash && legacyPin && pin === legacyPin);
  if (!ok) {
    $('loginMsg').textContent = 'PIN غير صحيح';
    return;
  }
  if (!storedHash) {
    state.manager.pinHash = pinHash;
    delete state.manager.pin;
    saveState();
  }
  state.authenticated = true;
  initDashboard();
}

async function loadMonths() {
  try {
    const res = await api('getAvailableMonths', {});
    const months = Array.isArray(res.data) ? res.data : [];
    state.months = months;
    state.month = months[0] || '';
    $('monthSelect').innerHTML = months.map((m) => `<option value="${m}">${m}</option>`).join('');
    if (state.month) $('monthSelect').value = state.month;
  } catch {
    $('monthSelect').innerHTML = '<option value="">تعذر تحميل الأشهر</option>';
  }
}

function refreshDriverSelects() {
  const opts = state.drivers.map((d) => `<option value="${esc(d.id)}">${esc(d.name)} - ${esc(d.carNumber)}</option>`).join('');
  ['tripDriver', 'factoryDriver', 'maintDriver'].forEach((id) => {
    $(id).innerHTML = `<option value="">اختر سائق</option>${opts}`;
  });
}

function renderDrivers() {
  const list = $('driversList');
  if (!state.drivers.length) { list.innerHTML = '<div class="list-item muted">لا توجد بيانات سواق</div>'; return; }
  list.innerHTML = state.drivers.map((d) => `
    <div class="list-item">
      <div>${esc(d.name)} | ${esc(d.phone || '-')} | ${esc(d.carNumber)}</div>
      <div class="driver-actions">
        <button class="btn driver-edit-btn" data-id="${esc(d.id)}">تعديل</button>
        <button class="btn danger driver-delete-btn" data-id="${esc(d.id)}">حذف</button>
      </div>
    </div>
  `).join('');
}

function addDriver() {
  const name = normalizePersonName($('driverName').value);
  const phone = String($('driverPhone').value || '').trim();
  const carNumber = normalizeCarNumber($('driverCar').value);

  if (!name || !carNumber) {
    window.alert('اسم السائق ورقم السيارة مطلوبان.');
    return;
  }

  const duplicate = (state.drivers || []).some((d) => {
    const sameName = normalizePersonName(d && d.name).toLowerCase() === name.toLowerCase();
    const sameCar = normalizeCarNumber(d && d.carNumber) === carNumber;
    return sameName && sameCar;
  });
  if (duplicate) {
    window.alert('السائق موجود مسبقًا بنفس الاسم ورقم السيارة.');
    return;
  }

  const driver = {
    id: `local-${Date.now()}`,
    name,
    phone,
    carNumber,
    fare: 0
  };

  state.drivers = [...(state.drivers || []), driver].sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'ar'));
  saveState();
  renderDrivers();
  refreshDriverSelects();

  $('driverName').value = '';
  $('driverPhone').value = '';
  $('driverCar').value = '';
}

function deleteDriver(id) {
  const targetId = String(id || '').trim();
  if (!targetId) return;

  const target = (state.drivers || []).find((d) => String(d.id || '').trim() === targetId);
  if (!target) {
    window.alert('تعذر العثور على السائق المطلوب حذفه.');
    return;
  }

  const ok = window.confirm(`تأكيد حذف السائق: ${target.name} - ${target.carNumber} ؟`);
  if (!ok) return;

  state.drivers = (state.drivers || []).filter((d) => String(d.id || '').trim() !== targetId);
  if (state.ui && state.ui.editDriverId === targetId) {
    closeDriverEditModal();
  }
  saveState();
  renderDrivers();
  refreshDriverSelects();
}

function closeDriverEditModal() {
  state.ui.editDriverId = '';
  const modal = $('driverEditModal');
  if (modal) modal.classList.add('hidden');
  setInlineMessage('driverEditMsg', '', 'info');
}

function openDriverEditModal(driverId) {
  const id = String(driverId || '').trim();
  if (!id) return;
  const target = state.drivers.find((d) => d.id === id);
  if (!target) {
    window.alert('تعذر العثور على السائق المطلوب.');
    return;
  }

  state.ui.editDriverId = id;
  $('editDriverName').value = String(target.name || '');
  $('editDriverPhone').value = String(target.phone || '');
  $('editDriverCar').value = String(target.carNumber || '');
  setInlineMessage('driverEditMsg', 'قم بتعديل البيانات ثم الحفظ.', 'info');

  const modal = $('driverEditModal');
  if (modal) modal.classList.remove('hidden');
}

function saveDriverEdit() {
  const id = String(state.ui && state.ui.editDriverId || '').trim();
  if (!id) {
    setInlineMessage('driverEditMsg', 'تعذر تحديد السائق للتعديل.', 'error');
    return;
  }

  const name = normalizePersonName($('editDriverName').value);
  const phone = String($('editDriverPhone').value || '').trim();
  const carNumber = normalizeCarNumber($('editDriverCar').value);

  if (!name || !carNumber) {
    setInlineMessage('driverEditMsg', 'اسم السائق ورقم السيارة مطلوبان.', 'error');
    return;
  }

  const duplicate = (state.drivers || []).some((d) => {
    const otherId = String(d && d.id || '').trim();
    if (!otherId || otherId === id) return false;
    const sameName = normalizePersonName(d && d.name).toLowerCase() === name.toLowerCase();
    const sameCar = normalizeCarNumber(d && d.carNumber) === carNumber;
    return sameName && sameCar;
  });
  if (duplicate) {
    setInlineMessage('driverEditMsg', 'يوجد سائق آخر بنفس الاسم ورقم السيارة.', 'error');
    return;
  }

  let updated = false;
  state.drivers = (state.drivers || []).map((d) => {
    if (String(d && d.id || '').trim() !== id) return d;
    updated = true;
    return {
      ...d,
      name,
      phone,
      carNumber
    };
  });

  if (!updated) {
    setInlineMessage('driverEditMsg', 'تعذر العثور على السائق.', 'error');
    return;
  }

  state.drivers.sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'ar'));
  saveState();
  renderDrivers();
  refreshDriverSelects();
  setInlineMessage('driverEditMsg', 'تم حفظ التعديل بنجاح.', 'success');
  closeDriverEditModal();
}

function normalizeDocNumber(value) {
  return String(value || '').trim().toUpperCase();
}

function getRecordCompanyId(record) {
  const direct = normalizeCompanyIdToken(
    record && (record.companyId || record.company || record.scopeCompanyId || record.cid || '')
  );
  if (direct) return direct;
  return extractCompanyIdFromNotes(record && record.notes);
}

function getRecordActivationCode(record) {
  const direct = String(
    record && (record.activationCode || record.activation || record.scopeActivationCode || record.act || '')
  ).trim();
  if (direct) return direct;
  return extractActivationCodeFromNotes(record && record.notes);
}

const STATION_CANONICALS = [
  { name: 'محطة الحلفاية', aliases: ['حلفاية', 'الحلفاية'] },
  { name: 'محطة التاجي', aliases: ['تاجي', 'التاجي'] },
  { name: 'محطة الرصافة', aliases: ['رصافة', 'الرصافة'] },
  { name: 'محطة الدورة', aliases: ['دورة', 'الدورة'] }
];

const FACTORY_OPTIONS = [
  'معمل الحبيبية',
  'مستودع التاجي',
  'معمل أبو غريب',
  'معمل الغزالية',
  'معمل العامرية',
  'معمل الصمود',
  'معمل الزعفرانية',
  'معمل المشتل',
  'معمل النهضة',
  'معمل الرصافة',
  'معمل النهروان',
  'معمل الحسينية',
  'معمل طارق',
  'معمل باب الشام',
  'معمل كسرة وعطش'
];

function normalizeLooseText(value) {
  return String(value || '').trim().toLowerCase().replace(/[\s\-_/]+/g, ' ');
}

function normalizeStationName(value) {
  const raw = String(value || '').trim();
  if (!raw) return '-';
  const norm = normalizeLooseText(raw);
  const found = STATION_CANONICALS.find((item) => {
    if (normalizeLooseText(item.name) === norm) return true;
    return item.aliases.some((a) => norm.includes(normalizeLooseText(a)));
  });
  return found ? found.name : raw;
}

function normalizeFactoryName(value) {
  const raw = String(value || '').trim();
  if (!raw) return '-';
  const norm = normalizeLooseText(raw);
  if (norm.startsWith('معمل')) return raw;
  if (norm.startsWith('مستودع')) return raw;
  if (norm.includes('factory')) return raw;
  return `معمل ${raw}`;
}

function populateFactoryOptions() {
  const select = $('factoryName');
  if (!select) return;
  const options = FACTORY_OPTIONS.map((name) => `<option value="${esc(name)}">${esc(name)}</option>`).join('');
  select.innerHTML = `<option value="">اختر المعمل</option>${options}<option value="أخرى">أخرى</option>`;
}

function toggleFactoryOtherField() {
  const select = $('factoryName');
  const other = $('factoryNameOther');
  if (!select || !other) return;
  const isOther = String(select.value || '').trim() === 'أخرى';
  other.classList.toggle('hidden', !isOther);
  if (!isOther) other.value = '';
}

function getRecordDestination(record) {
  const station = String(record && (record.station || record.destination || '')).trim();
  const factory = String(record && (record.factory || '')).trim();
  if (isFactoryTripRecord(record)) {
    return normalizeFactoryName(factory || station);
  }
  return normalizeStationName(station || factory);
}

function fmtNumAr(value, digits = 0) {
  const n = num(value);
  return n.toLocaleString('ar-IQ', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
}

function fmtDateAr(value) {
  const raw = String(value || '').trim();
  if (!raw) return '-';

  const normalized = raw.replace(/\//g, '-');
  const d = new Date(normalized);
  if (!Number.isNaN(d.getTime())) {
    return d.toLocaleString('ar-IQ', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  return raw;
}

function fixedRateForTrip(record) {
  return isFactoryTripRecord(record) ? FACTORY_TRIP_RATE : STATION_TRIP_RATE;
}

function amountFromTrip(record) {
  const qty = num(record && (record.netQuantity || record.quantity));
  if (qty > 0) return qty * fixedRateForTrip(record);
  return num(record && (record.price || record.finalAmount || record.storedPrice));
}

function getFilteredReportRecords() {
  const half = $('reportHalf').value;
  return state.trips.filter((t) => {
    if (half === 'all') return true;
    const d = new Date(String(t.unloadDate || t.loadDate || t.timestamp || '').replace(/\//g, '-'));
    const day = Number.isNaN(d.getTime()) ? 1 : d.getDate();
    if (half === 'first') return day <= 15;
    return day >= 16;
  });
}

function filenameSafeMonth() {
  const base = String(state.month || monthKeyFromDate(new Date().toISOString()) || 'month').trim();
  return base.replace(/[^\w\-]+/g, '_');
}

function addSentDoc(docNumber) {
  const doc = normalizeDocNumber(docNumber);
  if (!doc) return;
  if (state.sentDocs.includes(doc)) return;
  state.sentDocs.unshift(doc);
  if (state.sentDocs.length > 3000) state.sentDocs = state.sentDocs.slice(0, 3000);
  saveState();
}

function hasLocalDuplicateReceipt(docNumber, carNumber) {
  const normalizedDoc = normalizeDocNumber(docNumber);
  const normalizedCar = normalizeCarNumber(carNumber);
  if (!normalizedDoc || !normalizedCar) return false;
  return (state.trips || []).some((r) => {
    const sameDoc = normalizeDocNumber(r && r.docNumber) === normalizedDoc;
    const sameCar = normalizeCarNumber(r && r.carNumber) === normalizedCar;
    return sameDoc && sameCar;
  });
}

function isValidDateRange(loadDate, unloadDate) {
  if (!loadDate || !unloadDate) return false;
  const load = new Date(String(loadDate).replace(/\//g, '-'));
  const unload = new Date(String(unloadDate).replace(/\//g, '-'));
  if (Number.isNaN(load.getTime()) || Number.isNaN(unload.getTime())) return false;
  return load.getTime() <= unload.getTime();
}

function isFactoryTripRecord(record) {
  const station = String(record && (record.station || record.factory || record.destination || '')).toLowerCase();
  const source = String(record && record.source || '').toLowerCase();
  const sheetName = String(record && record.sheetName || '').toLowerCase();
  return station.includes('معمل') || station.includes('factory') || source.includes('factory') || sheetName.startsWith('f_') || sheetName.startsWith('مع_');
}

function filterTripsForCompanyScope(rows) {
  const cid = getActiveCompanyScopeId();
  const act = String(state.activationCode || '').trim();
  return (rows || []).filter((r) => {
    const rowCid = getRecordCompanyId(r);
    if (!rowCid || rowCid !== cid) return false;
    if (!act) return false;
    const rowAct = getRecordActivationCode(r);
    return !!rowAct && rowAct === act;
  });
}

function syncDriversFromTrips() {
  const map = new Map();

  // Keep manually managed drivers visible even if they have no trips in selected month.
  (state.drivers || []).forEach((driver) => {
    const name = normalizePersonName(driver && driver.name);
    const carNumber = normalizeCarNumber(driver && driver.carNumber);
    if (!name || !carNumber) return;
    const key = `${name.toLowerCase()}__${carNumber}`;
    map.set(key, {
      id: String(driver && driver.id || `local-${Date.now()}-${map.size + 1}`),
      name,
      phone: String(driver && driver.phone || ''),
      carNumber,
      fare: num(driver && driver.fare)
    });
  });

  (state.trips || []).forEach((trip) => {
    const name = normalizePersonName(trip && trip.driverName);
    const carNumber = normalizeCarNumber(trip && trip.carNumber);
    if (!name || !carNumber) return;
    const key = `${name.toLowerCase()}__${carNumber}`;
    const fare = num(trip && (trip.kroa || trip.driverFare));
    const current = map.get(key);
    if (!current) {
      map.set(key, {
        id: `srv-${Date.now()}-${map.size + 1}`,
        name,
        phone: '',
        carNumber,
        fare
      });
      return;
    }
    if (fare > current.fare) current.fare = fare;
  });

  state.drivers = Array.from(map.values()).sort((a, b) => a.name.localeCompare(b.name, 'ar'));
  saveState();
  renderDrivers();
  refreshDriverSelects();
}

async function wasReceiptSavedForPayload(payload) {
  const doc = normalizeDocNumber(payload && payload.docNumber);
  if (!doc) return false;
  const month = state.month || monthKeyFromDate(new Date().toISOString());

  try {
    const res = await api('getAllReceiptsData', { month });
    const rows = Array.isArray(res && res.data) ? res.data : [];
    return rows.some((r) => {
      const sameDoc = normalizeDocNumber(r && r.docNumber) === doc;
      const sameCar = String((r && r.carNumber) || '').trim() === String(payload.carNumber || '').trim();
      return sameDoc && sameCar;
    });
  } catch {
    return false;
  }
}

function clearTripForm() {
  $('tripDoc').value = '';
  $('tripStation').value = 'محطة التاجي';
  $('tripLoad').value = '';
  $('tripUnload').value = '';
  $('tripQty').value = '';
  $('tripLiters').value = '';
  $('tripOwner').value = '';
  $('tripNotes').value = '';
}

function clearFactoryForm() {
  $('factoryDoc').value = '';
  $('factoryName').value = '';
  $('factoryNameOther').value = '';
  $('factoryNameOther').classList.add('hidden');
  $('factoryLoad').value = '';
  $('factoryUnload').value = '';
  $('factoryQty').value = '';
  $('factoryVehicleOwner').value = '';
  $('factoryNotes').value = '';
}

async function sendTrip() {
  if (state.ui.tripSubmitting) return;
  const driver = state.drivers.find((d) => d.id === $('tripDriver').value);
  if (!driver) {
    setInlineMessage('tripMsg', 'اختر السائق أولاً.', 'error');
    return;
  }

  const docNumber = normalizeDocNumber($('tripDoc').value);
  if (!isValidDocNumber(docNumber)) {
    setInlineMessage('tripMsg', 'رقم الوصل غير صالح. استخدم أرقام/حروف بدون رموز خاصة.', 'error');
    return;
  }

  const rawStation = String($('tripStation').value || '').trim();
  if (!rawStation) {
    setInlineMessage('tripMsg', 'يرجى تحديد المحطة.', 'error');
    return;
  }
  const station = normalizeStationName(rawStation);

  if (!isValidDateRange($('tripLoad').value, $('tripUnload').value)) {
    setInlineMessage('tripMsg', 'تاريخ التحميل/التفريغ غير صالح أو غير مكتمل.', 'error');
    return;
  }

  const qty = num($('tripQty').value || '0');
  if (qty <= 0) {
    setInlineMessage('tripMsg', 'الكمية يجب أن تكون أكبر من صفر.', 'error');
    return;
  }

  if (hasLocalDuplicateReceipt(docNumber, driver.carNumber)) {
    setInlineMessage('tripMsg', 'هذا الوصل مسجل مسبقًا لنفس السيارة.', 'error');
    return;
  }

  const tripAmount = qty * STATION_TRIP_RATE;
  const payload = {
    docNumber,
    driverName: driver.name,
    carNumber: driver.carNumber,
    station,
    loadDate: $('tripLoad').value,
    unloadDate: $('tripUnload').value,
    quantity: String(qty),
    liters: $('tripLiters').value || '0',
    owner: $('tripOwner').value.trim(),
    price: String(tripAmount),
    storedPrice: String(tripAmount),
    finalAmount: String(tripAmount),
    distance: '0',
    notes: buildCompanyScopedNotes($('tripNotes').value.trim()),
    month: state.month,
    tripMonth: state.month
  };
  state.ui.tripSubmitting = true;
  setButtonBusy('btnSendTrip', true);
  setInlineMessage('tripMsg', 'جاري الإرسال...', 'info');
  try {
    const res = await api('trip', payload);
    setInlineMessage('tripMsg', res.message || 'تم إرسال الوصل بنجاح', 'success');
    addSentDoc(payload.docNumber);
    clearTripForm();
    await loadTrips();
  } catch (e) {
    const saved = await wasReceiptSavedForPayload(payload);
    if (saved) {
      setInlineMessage('tripMsg', 'تم إرسال الوصل بنجاح (تم تأكيد الحفظ من السجل).', 'success');
      addSentDoc(payload.docNumber);
      clearTripForm();
      await loadTrips();
      return;
    }
    setInlineMessage('tripMsg', 'فشل: ' + String(e.message || e), 'error');
  } finally {
    state.ui.tripSubmitting = false;
    setButtonBusy('btnSendTrip', false);
  }
}

async function sendFactory() {
  if (state.ui.factorySubmitting) return;
  const driver = state.drivers.find((d) => d.id === $('factoryDriver').value);
  if (!driver) {
    setInlineMessage('factoryMsg', 'اختر السائق أولاً.', 'error');
    return;
  }

  const docNumber = normalizeDocNumber($('factoryDoc').value);
  if (!isValidDocNumber(docNumber)) {
    setInlineMessage('factoryMsg', 'رقم الوصل غير صالح. استخدم أرقام/حروف بدون رموز خاصة.', 'error');
    return;
  }

  if (!isValidDateRange($('factoryLoad').value, $('factoryUnload').value)) {
    setInlineMessage('factoryMsg', 'تاريخ التحميل/التفريغ غير صالح أو غير مكتمل.', 'error');
    return;
  }

  const selectedFactory = String($('factoryName').value || '').trim();
  const rawFactoryName = selectedFactory === 'أخرى'
    ? String($('factoryNameOther').value || '').trim()
    : selectedFactory;
  if (!rawFactoryName) {
    setInlineMessage('factoryMsg', 'اسم المعمل مطلوب.', 'error');
    return;
  }
  const factoryName = normalizeFactoryName(rawFactoryName);

  const vehicleOwner = $('factoryVehicleOwner').value.trim();
  const qty = num($('factoryQty').value || '0');
  if (qty <= 0) {
    setInlineMessage('factoryMsg', 'الكمية يجب أن تكون أكبر من صفر.', 'error');
    return;
  }

  if (hasLocalDuplicateReceipt(docNumber, driver.carNumber)) {
    setInlineMessage('factoryMsg', 'هذا الوصل مسجل مسبقًا لنفس السيارة.', 'error');
    return;
  }

  const tripAmount = qty * FACTORY_TRIP_RATE;
  const payload = {
    docNumber,
    driverName: driver.name,
    carNumber: driver.carNumber,
    factory: factoryName,
    loadDate: $('factoryLoad').value,
    unloadDate: $('factoryUnload').value,
    quantity: String(qty),
    owner: vehicleOwner,
    vehicleOwner,
    price: String(tripAmount),
    storedPrice: String(tripAmount),
    finalAmount: String(tripAmount),
    notes: buildCompanyScopedNotes($('factoryNotes').value.trim()),
    month: state.month,
    tripMonth: state.month
  };
  state.ui.factorySubmitting = true;
  setButtonBusy('btnSendFactory', true);
  setInlineMessage('factoryMsg', 'جاري الإرسال...', 'info');
  try {
    const res = await api('factory', payload);
    setInlineMessage('factoryMsg', res.message || 'تم إرسال الوصل بنجاح', 'success');
    addSentDoc(payload.docNumber);
    clearFactoryForm();
    await loadTrips();
  } catch (e) {
    const saved = await wasReceiptSavedForPayload(payload);
    if (saved) {
      setInlineMessage('factoryMsg', 'تم إرسال الوصل بنجاح (تم تأكيد الحفظ من السجل).', 'success');
      addSentDoc(payload.docNumber);
      clearFactoryForm();
      await loadTrips();
      return;
    }
    setInlineMessage('factoryMsg', 'فشل: ' + String(e.message || e), 'error');
  } finally {
    state.ui.factorySubmitting = false;
    setButtonBusy('btnSendFactory', false);
  }
}

function renderOverview() {
  const driverFilter = String($('overviewDriverFilter')?.value || '').trim().toLowerCase();
  const carFilter = String($('overviewCarFilter')?.value || '').trim().toLowerCase();
  const docFilter = String($('overviewDocFilter')?.value || '').trim().toLowerCase();
  const sortBy = String($('overviewSort')?.value || 'trips').trim();

  let rows = state.trips.map((t) => ({
    docNumber: String(t.docNumber || '-').trim(),
    driverName: String(t.driverName || 'غير محدد').trim(),
    carNumber: String(t.carNumber || '-').trim(),
    station: getRecordDestination(t),
    qty: num(t.netQuantity || t.quantity),
    amount: amountFromTrip(t),
    liters: num(t.liters || t.gasLiters),
    owner: String(t.owner || t.vehicleOwner || '-').trim(),
    when: String(t.unloadDate || t.loadDate || t.sendTime || t.timestamp || '').trim(),
    isFactory: isFactoryTripRecord(t),
    sortDate: String(t.sendTime || t.timestamp || t.unloadDate || t.loadDate || '').trim(),
    tripIdentity: getTripIdentity(t)
  }));

  rows = rows.filter((r) => {
    if (driverFilter && !r.driverName.toLowerCase().includes(driverFilter)) return false;
    if (carFilter && !r.carNumber.toLowerCase().includes(carFilter)) return false;
    if (docFilter && !String(r.docNumber).toLowerCase().includes(docFilter)) return false;
    return true;
  });

  if (sortBy === 'name') {
    rows.sort((a, b) => a.driverName.localeCompare(b.driverName, 'ar'));
  } else if (sortBy === 'qty') {
    rows.sort((a, b) => b.qty - a.qty);
  } else if (sortBy === 'amount') {
    rows.sort((a, b) => b.amount - a.amount);
  } else {
    rows.sort((a, b) => String(b.sortDate).localeCompare(String(a.sortDate)));
  }

  if (!rows.length) {
    $('overviewStats').innerHTML = '<div class="list-item muted">لا توجد بيانات</div>';
    $('driversSplitBoard').innerHTML = '';
    return;
  }

  $('overviewStats').innerHTML = `
    <div class="trip-cards-wrap">
      ${rows.map((r) => `
        <div class="trip-card ${r.isFactory ? 'trip-factory' : 'trip-station'} trip-card-mobile-like">
          <div class="trip-status-pill">${r.isFactory ? 'معمل' : 'مكتمل'}</div>
          <div class="trip-number-title">النقلة <span class="trip-doc-ltr">#${esc(r.docNumber)}</span></div>
          <div class="trip-line"><span class="label">السائق:</span> <span class="value">${esc(r.driverName)}</span></div>
          <div class="trip-line"><span class="label">السيارة:</span> <span class="value">${esc(r.carNumber)}</span></div>
          <div class="trip-line"><span class="label">الوجهة:</span> <span class="value">${esc(r.station)}</span></div>
          <div class="trip-line"><span class="label">الكمية:</span> <span class="value">${fmtNumAr(r.qty, 3)} طن</span></div>
          <div class="trip-line"><span class="label">الإيراد:</span> <span class="value">${fmtNumAr(r.amount, 0)} د.ع</span></div>
          <div class="trip-line"><span class="label">التاريخ:</span> <span class="value">${esc(fmtDateAr(r.when))}</span></div>
          <button class="trip-edit-btn" type="button" data-trip="${encodeURIComponent(r.tripIdentity)}">تعديل</button>
        </div>
      `).join('')}
    </div>
  `;

  renderDriversSplitBoard(rows);
}

function renderDriversSplitBoard(rows) {
  const stationMap = new Map();
  const factoryMap = new Map();

  (rows || []).forEach((r) => {
    const key = `${r.driverName}__${r.carNumber}`;
    const target = r.isFactory ? factoryMap : stationMap;
    const current = target.get(key) || {
      driverName: r.driverName,
      carNumber: r.carNumber,
      trips: 0,
      qty: 0,
      amount: 0,
    };
    current.trips += 1;
    current.qty += num(r.qty);
    current.amount += num(r.amount);
    target.set(key, current);
  });

  const renderList = (map, emptyText) => {
    const items = Array.from(map.values()).sort((a, b) => b.trips - a.trips || b.qty - a.qty);
    if (!items.length) return `<div class="list-item muted">${emptyText}</div>`;
    return items.map((x) => `
      <div class="list-item split-driver-item">
        <div>
          <strong>${esc(x.driverName)}</strong>
          <div class="muted">سيارة: ${esc(x.carNumber)}</div>
        </div>
        <div class="split-driver-metrics">
          <span>${x.trips} نقلة</span>
          <span>${fmtNumAr(x.qty, 3)} طن</span>
          <span>${fmtNumAr(x.amount, 0)} د.ع</span>
        </div>
      </div>
    `).join('');
  };

  $('driversSplitBoard').innerHTML = `
    <div class="drivers-split-wrap">
      <div class="drivers-split-card">
        <h4>لائحة السائقين - التاجي/المحطات</h4>
        <div class="list">${renderList(stationMap, 'لا توجد نقلات محطات')}</div>
      </div>
      <div class="drivers-split-card">
        <h4>لائحة السائقين - المعامل</h4>
        <div class="list">${renderList(factoryMap, 'لا توجد نقلات معامل')}</div>
      </div>
    </div>
  `;
}

function renderSummaryCards(items, emptyText) {
  const rows = Array.isArray(items) ? items : [];
  if (!rows.length) {
    return `<div class="report-empty-card">${esc(emptyText || 'لا توجد بيانات')}</div>`;
  }

  return rows.map((item) => `
    <div class="report-kpi-card report-kpi-card-wide">
      <span>${esc(item.label || '')}</span>
      <strong>${esc(item.value || '')}</strong>
      ${item.sub ? `<small>${esc(item.sub)}</small>` : ''}
    </div>
  `).join('');
}

function renderCompanyTab() {
  const byCar = new Map();
  const byDriver = new Map();
  let totalRevenue = 0;
  let totalSalaries = 0;
  let stationQty = 0;
  let factoryQty = 0;
  let stationRevenue = 0;
  let factoryRevenue = 0;

  (state.trips || []).forEach((trip) => {
    const car = String(trip.carNumber || '-').trim() || '-';
    const driverName = String(trip.driverName || 'غير محدد').trim() || 'غير محدد';
    const qty = num(trip.netQuantity || trip.quantity);
    const amount = amountFromTrip(trip);
    const fare = num(trip.kroa || trip.driverFare);
    const isFactory = isFactoryTripRecord(trip);

    totalRevenue += amount;
    totalSalaries += fare;
    if (isFactory) {
      factoryQty += qty;
      factoryRevenue += amount;
    } else {
      stationQty += qty;
      stationRevenue += amount;
    }

    const carCurrent = byCar.get(car) || { carNumber: car, trips: 0, qty: 0, amount: 0 };
    carCurrent.trips += 1;
    carCurrent.qty += qty;
    carCurrent.amount += amount;
    byCar.set(car, carCurrent);

    const driverCurrent = byDriver.get(driverName) || { driverName, trips: 0, salary: 0 };
    driverCurrent.trips += 1;
    driverCurrent.salary += fare;
    byDriver.set(driverName, driverCurrent);
  });

  const carsCount = byCar.size;
  const netRevenue = totalRevenue - totalSalaries;
  const totalQty = stationQty + factoryQty;

  $('companyStats').innerHTML = `
    ${renderSummaryCards([
      { label: 'محطات - الكمية', value: `${fmtNumAr(stationQty, 3)} طن` },
      { label: 'محطات - الإيراد', value: `${fmtNumAr(stationRevenue, 0)} د.ع` },
      { label: 'معامل - الكمية', value: `${fmtNumAr(factoryQty, 3)} طن` },
      { label: 'معامل - الإيراد', value: `${fmtNumAr(factoryRevenue, 0)} د.ع` },
      { label: 'الإجمالي - الكمية', value: `${fmtNumAr(totalQty, 3)} طن` },
      { label: 'الإجمالي - الإيراد', value: `${fmtNumAr(totalRevenue, 0)} د.ع` },
      { label: 'رواتب السواق', value: `${fmtNumAr(totalSalaries, 0)} د.ع` },
      { label: 'صافي الإيراد', value: `${fmtNumAr(netRevenue, 0)} د.ع` }
    ])}
  `;

  const carRows = Array.from(byCar.values()).sort((a, b) => b.amount - a.amount);
  $('companyCarRevenue').innerHTML = carRows.length
    ? carRows.map((x) => `
      <div class="list-item split-driver-item">
        <div>
          <strong>سيارة ${esc(x.carNumber)}</strong>
          <div class="muted">${x.trips} نقلة | ${fmtNumAr(x.qty, 3)} طن</div>
        </div>
        <div class="split-driver-metrics">
          <span>${fmtNumAr(x.amount, 0)} د.ع</span>
        </div>
      </div>
    `).join('')
    : '<div class="list-item muted">لا توجد بيانات إيرادات سيارات</div>';

  const salaryRows = Array.from(byDriver.values()).sort((a, b) => b.salary - a.salary);
  $('companyDriverSalaries').innerHTML = salaryRows.length
    ? salaryRows.map((x) => `
      <div class="list-item split-driver-item">
        <div>
          <strong>${esc(x.driverName)}</strong>
          <div class="muted">${x.trips} نقلة</div>
        </div>
        <div class="split-driver-metrics">
          <span>${fmtNumAr(x.salary, 0)} د.ع</span>
        </div>
      </div>
    `).join('')
    : '<div class="list-item muted">لا توجد بيانات رواتب سواق</div>';
}

function renderQuickKpis() {
  const holder = $('quickKpis');
  if (!holder) return;

  const rows = Array.isArray(state.trips) ? state.trips : [];
  let stationQty = 0;
  let factoryQty = 0;
  let totalRevenue = 0;
  let totalSalary = 0;

  rows.forEach((r) => {
    const qty = num(r.netQuantity || r.quantity);
    const amount = amountFromTrip(r);
    const fare = num(r.kroa || r.driverFare);
    if (isFactoryTripRecord(r)) factoryQty += qty;
    else stationQty += qty;
    totalRevenue += amount;
    totalSalary += fare;
  });

  const netRevenue = totalRevenue - totalSalary;
  holder.innerHTML = `
    <div class="quick-kpi-item"><span>النقلات</span><strong>${rows.length}</strong></div>
    <div class="quick-kpi-item"><span>كمية المحطات</span><strong>${fmtNumAr(stationQty, 3)} طن</strong></div>
    <div class="quick-kpi-item"><span>كمية المعامل</span><strong>${fmtNumAr(factoryQty, 3)} طن</strong></div>
    <div class="quick-kpi-item"><span>صافي الإيراد</span><strong>${fmtNumAr(netRevenue, 0)} د.ع</strong></div>
  `;
}

function renderAuditLog() {
  const wrap = $('auditLogList');
  if (!wrap) return;

  if (!state.ui.auditVisible) {
    wrap.classList.add('hidden');
    return;
  }

  wrap.classList.remove('hidden');
  const query = String(($('auditSearch') && $('auditSearch').value) || '').trim().toLowerCase();
  const range = String(($('auditRange') && $('auditRange').value) || 'all').trim();
  const logs = (Array.isArray(state.auditLog) ? state.auditLog : []).filter((entry) => {
    if (!isAuditInRange(entry && entry.at, range)) return false;
    if (!query) return true;
    const hay = [
      entry && entry.actor,
      entry && entry.action,
      entry && entry.before && entry.before.docNumber,
      entry && entry.before && entry.before.driverName,
      entry && entry.before && entry.before.carNumber
    ].map((v) => String(v || '').toLowerCase()).join(' | ');
    return hay.includes(query);
  });

  if (!logs.length) {
    wrap.innerHTML = '<div class="list-item muted">لا توجد تعديلات مسجلة حتى الآن.</div>';
    return;
  }

  wrap.innerHTML = logs.slice(0, 80).map((entry) => `
    <div class="list-item split-driver-item">
      <div>
        <strong>${esc(entry.before && entry.before.docNumber ? '#' + entry.before.docNumber : 'سجل تعديل')}</strong>
        <div class="muted">${esc(fmtDateAr(entry.at))} | بواسطة: ${esc(entry.actor || '-')}</div>
      </div>
      <div class="split-driver-metrics">
        <span>الكمية: ${fmtNumAr(entry.before && entry.before.quantity, 3)} -> ${fmtNumAr(entry.after && entry.after.quantity, 3)} طن</span>
        <span>الكروة: ${fmtNumAr(entry.before && entry.before.fare, 0)} -> ${fmtNumAr(entry.after && entry.after.fare, 0)} د.ع</span>
      </div>
    </div>
  `).join('');
}

function closeTripEditModal() {
  state.ui.editTripIdentity = '';
  const modal = $('tripEditModal');
  if (modal) modal.classList.add('hidden');
  setInlineMessage('tripEditMsg', '', 'info');
}

function openTripEditModal(identityEncoded) {
  const identity = decodeURIComponent(String(identityEncoded || '')).trim();
  if (!identity) return;

  const target = (state.trips || []).find((r) => getTripIdentity(r) === identity);
  if (!target) {
    window.alert('تعذر العثور على النقلة المطلوبة للتعديل.');
    return;
  }

  state.ui.editTripIdentity = identity;
  $('editTripDoc').value = String(target.docNumber || '');
  $('editTripCar').value = String(target.carNumber || '');
  $('editTripDriver').value = String(target.driverName || '');
  $('editTripDestination').value = getRecordDestination(target);
  $('editTripQty').value = String(num(target.netQuantity || target.quantity) || '');
  $('editTripLoad').value = String(target.loadDate || '');
  $('editTripUnload').value = String(target.unloadDate || '');
  $('editTripLiters').value = String(num(target.liters || target.gasLiters) || '0');
  $('editTripNotes').value = stripCompanyMarker(target.notes);
  setInlineMessage('tripEditMsg', 'يمكنك تعديل البيانات ثم الحفظ.', 'info');

  const modal = $('tripEditModal');
  if (modal) modal.classList.remove('hidden');
}

async function saveTripEdit() {
  const identity = String(state.ui.editTripIdentity || '').trim();
  if (!identity) {
    setInlineMessage('tripEditMsg', 'لا توجد نقلة محددة للتعديل.', 'error');
    return;
  }

  const target = (state.trips || []).find((r) => getTripIdentity(r) === identity);
  if (!target) {
    setInlineMessage('tripEditMsg', 'تعذر قراءة بيانات النقلة الأصلية.', 'error');
    return;
  }

  const qty = num($('editTripQty').value);
  const destinationRaw = String($('editTripDestination').value || '').trim();
  const loadDate = String($('editTripLoad').value || '').trim();
  const unloadDate = String($('editTripUnload').value || '').trim();
  const notes = String($('editTripNotes').value || '').trim();
  const liters = num($('editTripLiters').value);

  if (!destinationRaw) {
    setInlineMessage('tripEditMsg', 'الوجهة مطلوبة.', 'error');
    return;
  }
  if (qty <= 0) {
    setInlineMessage('tripEditMsg', 'الكمية يجب أن تكون أكبر من صفر.', 'error');
    return;
  }
  if (liters < 0) {
    setInlineMessage('tripEditMsg', 'القيم الرقمية يجب أن تكون موجبة.', 'error');
    return;
  }
  if (!isValidDateRange(loadDate, unloadDate)) {
    setInlineMessage('tripEditMsg', 'تاريخ التحميل/التفريغ غير صالح.', 'error');
    return;
  }

  const isFactory = isFactoryTripRecord(target);
  const normalizedDestination = isFactory ? normalizeFactoryName(destinationRaw) : normalizeStationName(destinationRaw);
  const patch = {
    quantity: String(qty),
    netQuantity: String(qty),
    liters: String(liters),
    loadDate,
    unloadDate,
    notes: buildCompanyScopedNotes(notes),
    updatedLocallyAt: new Date().toISOString()
  };
  if (isFactory) {
    patch.factory = normalizedDestination;
  } else {
    patch.station = normalizedDestination;
  }

  state.tripEdits = state.tripEdits || {};
  state.tripEdits[identity] = {
    ...(state.tripEdits[identity] || {}),
    ...patch
  };

  appendAudit('trip-edit', identity, target, { ...target, ...patch });
  state.pendingSyncEdits = state.pendingSyncEdits || {};
  state.pendingSyncEdits[identity] = {
    baseRecord: { ...target },
    patch: { ...patch },
    queuedAt: new Date().toISOString()
  };
  saveState();

  state.trips = applyTripEdits(state.trips);
  renderQuickKpis();
  renderOverview();
  renderCompanyTab();
  renderReports();
  renderAuditLog();

  const syncResult = await syncTripEditToServer(identity, target, patch);
  if (syncResult.success) {
    delete state.pendingSyncEdits[identity];
    saveState();
    setInlineMessage('tripEditMsg', 'تم حفظ التعديل ومزامنته مع الخادم.', 'success');
  } else if (syncResult.hardFail) {
    setInlineMessage('tripEditMsg', 'تم الحفظ محليًا. الخادم الحالي لا يدعم updateReceipt حاليًا.', 'info');
  } else {
    setInlineMessage('tripEditMsg', 'تم الحفظ محليًا. ستتم إعادة المحاولة للمزامنة تلقائيًا.', 'info');
  }
}

function addMaintenance() {
  const driver = state.drivers.find((d) => d.id === $('maintDriver').value);
  if (!driver) {
    window.alert('اختر السائق أولاً.');
    return;
  }

  const maintenanceType = String($('maintType').value || '').trim();
  const dueDate = String($('maintDate').value || '').trim();
  const estimatedCost = num($('maintCost').value);

  if (!maintenanceType || !dueDate) {
    window.alert('نوع الصيانة وتاريخها مطلوبان.');
    return;
  }

  if (estimatedCost < 0) {
    window.alert('الكلفة يجب أن تكون رقمًا موجبًا.');
    return;
  }

  const item = {
    id: String(Date.now()),
    carNumber: driver.carNumber,
    assignedTo: driver.name,
    maintenanceType,
    dueDate,
    estimatedCost,
    notes: $('maintNotes').value.trim(),
    status: 'مجدولة'
  };
  state.maintenance.unshift(item);
  saveState();
  renderMaintenance();
  api('saveMaintenance', {
    driverName: item.assignedTo,
    carNumber: item.carNumber,
    type: item.maintenanceType,
    cost: String(item.estimatedCost),
    notes: item.notes,
    requestDate: item.dueDate
  }).catch(() => {});
}

function renderMaintenance() {
  $('maintList').innerHTML = state.maintenance.length
    ? state.maintenance.map((m) => `<div class="list-item"><div>${esc(m.assignedTo)} | ${esc(m.carNumber)} | ${esc(m.maintenanceType)} | ${esc(m.dueDate)}</div><div>${Math.round(num(m.estimatedCost)).toLocaleString('en-US')}</div></div>`).join('')
    : '<div class="list-item muted">لا توجد صيانة</div>';
}

function renderReports() {
  const records = getFilteredReportRecords();

  const trips = records.length;
  const qty = records.reduce((s, r) => s + num(r.netQuantity || r.quantity), 0);
  const amount = records.reduce((s, r) => s + amountFromTrip(r), 0);
  const factories = records.filter((r) => isFactoryTripRecord(r)).length;
  const stations = trips - factories;
  const stationQty = records.filter((r) => !isFactoryTripRecord(r)).reduce((s, r) => s + num(r.netQuantity || r.quantity), 0);
  const factoryQty = records.filter((r) => isFactoryTripRecord(r)).reduce((s, r) => s + num(r.netQuantity || r.quantity), 0);
  const stationAmount = records.filter((r) => !isFactoryTripRecord(r)).reduce((s, r) => s + amountFromTrip(r), 0);
  const factoryAmount = records.filter((r) => isFactoryTripRecord(r)).reduce((s, r) => s + amountFromTrip(r), 0);
  const totalSalary = records.reduce((s, r) => s + num(r.kroa || r.driverFare), 0);
  const netRevenue = amount - totalSalary;

  const byDriver = new Map();
  records.forEach((r) => {
    const driverName = String(r.driverName || 'غير محدد').trim();
    const isFactory = isFactoryTripRecord(r);
    const current = byDriver.get(driverName) || {
      driverName,
      trips: 0,
      qty: 0,
      amount: 0,
      stationTrips: 0,
      factoryTrips: 0
    };
    current.trips += 1;
    current.qty += num(r.netQuantity || r.quantity);
    current.amount += amountFromTrip(r);
    if (isFactory) current.factoryTrips += 1;
    else current.stationTrips += 1;
    byDriver.set(driverName, current);
  });

  const ranked = Array.from(byDriver.values()).sort((a, b) => b.trips - a.trips || b.qty - a.qty);

  const stationDrivers = ranked.filter((x) => x.stationTrips > 0);
  const factoryDrivers = ranked.filter((x) => x.factoryTrips > 0);
  const stationDestinations = new Set(records.filter((r) => !isFactoryTripRecord(r)).map((r) => getRecordDestination(r))).size;
  const factoryDestinations = new Set(records.filter((r) => isFactoryTripRecord(r)).map((r) => getRecordDestination(r))).size;

  const renderSplitRows = (items, tripKey, emptyText) => {
    if (!items.length) return `<div class="list-item muted">${emptyText}</div>`;
    return items.map((x) => `
      <div class="list-item split-driver-item">
        <div>
          <strong>${esc(x.driverName)}</strong>
          <div class="muted">${x[tripKey]} نقلة</div>
        </div>
        <div class="split-driver-metrics">
          <span>${fmtNumAr(x.qty, 3)} طن</span>
          <span>${fmtNumAr(x.amount, 0)} د.ع</span>
        </div>
      </div>
    `).join('');
  };

  $('reportKpis').innerHTML = `
    ${renderSummaryCards([
      { label: 'إجمالي النقلات', value: String(trips) },
      { label: 'محطات - الكمية', value: `${fmtNumAr(stationQty, 3)} طن` },
      { label: 'محطات - الإيراد', value: `${fmtNumAr(stationAmount, 0)} د.ع` },
      { label: 'معامل - الكمية', value: `${fmtNumAr(factoryQty, 3)} طن` },
      { label: 'معامل - الإيراد', value: `${fmtNumAr(factoryAmount, 0)} د.ع` },
      { label: 'الإجمالي - الكمية', value: `${fmtNumAr(qty, 3)} طن` },
      { label: 'الإجمالي - الإيراد', value: `${fmtNumAr(amount, 0)} د.ع` },
      { label: 'وجهات المحطات', value: String(stationDestinations) },
      { label: 'وجهات المعامل', value: String(factoryDestinations) },
      { label: 'صافي الإيراد', value: `${fmtNumAr(netRevenue, 0)} د.ع`, sub: `بعد خصم الرواتب ${fmtNumAr(totalSalary, 0)} د.ع` }
    ])}
  `;

  $('reportStationDrivers').innerHTML = renderSplitRows(stationDrivers, 'stationTrips', 'لا توجد بيانات لمحطات');
  $('reportFactoryDrivers').innerHTML = renderSplitRows(factoryDrivers, 'factoryTrips', 'لا توجد بيانات للمعامل');

  $('reportSummary').innerHTML = `
    <div class="list-item"><div>إجمالي النقلات</div><div>${trips}</div></div>
    <div class="list-item"><div>إجمالي الكمية</div><div>${qty.toFixed(3)} طن</div></div>
    <div class="list-item"><div>إجمالي المبالغ</div><div>${Math.round(amount).toLocaleString('en-US')}</div></div>
    <div class="list-item"><div>المحطات</div><div>${stations}</div></div>
    <div class="list-item"><div>المعامل</div><div>${factories}</div></div>
    <div class="list-item"><div>صافي الإيراد</div><div>${Math.round(netRevenue).toLocaleString('en-US')}</div></div>
  `;
}

function exportReportExcel() {
  const records = getFilteredReportRecords();
  if (!records.length) {
    window.alert('لا توجد بيانات للتصدير.');
    return;
  }

  if (!window.XLSX) {
    window.alert('مكتبة XLSX غير متاحة.');
    return;
  }

  const headers = [
    'رقم الوصل',
    'اسم السائق',
    'رقم السيارة',
    'نوع النقلة',
    'الوجهة',
    'الكمية (طن)',
    'سعر الطن',
    'الإيراد (د.ع)',
    'تاريخ التحميل',
    'تاريخ التفريغ',
    'ملاحظات'
  ];

  const rows = records.map((r) => {
    const isFactory = isFactoryTripRecord(r);
    const qty = num(r.netQuantity || r.quantity);
    const priceRate = isFactory ? FACTORY_TRIP_RATE : STATION_TRIP_RATE;
    const amount = amountFromTrip(r);
    const destination = getRecordDestination(r);
    return [
      String(r.docNumber || ''),
      String(r.driverName || ''),
      String(r.carNumber || ''),
      isFactory ? 'معمل' : 'محطة',
      destination,
      Number(qty.toFixed(3)),
      priceRate,
      Math.round(amount),
      String(r.loadDate || ''),
      String(r.unloadDate || ''),
      stripCompanyMarker(r.notes)
    ];
  });

  const wsData = [headers, ...rows];
  const wb = window.XLSX.utils.book_new();
  const ws = window.XLSX.utils.aoa_to_sheet(wsData);
  ws['!cols'] = [
    { wch: 14 },
    { wch: 20 },
    { wch: 14 },
    { wch: 10 },
    { wch: 20 },
    { wch: 12 },
    { wch: 12 },
    { wch: 16 },
    { wch: 18 },
    { wch: 14 },
    { wch: 14 },
    { wch: 22 }
  ];
  window.XLSX.utils.book_append_sheet(wb, ws, 'تقرير الشركة');
  window.XLSX.writeFile(wb, `company_report_${filenameSafeMonth()}.xlsx`);
}

function exportReportPdf() {
  const records = getFilteredReportRecords();
  if (!records.length) {
    window.alert('لا توجد بيانات للتصدير.');
    return;
  }

  const rowsHtml = records.map((r) => {
    const isFactory = isFactoryTripRecord(r);
    const qty = num(r.netQuantity || r.quantity);
    const priceRate = isFactory ? FACTORY_TRIP_RATE : STATION_TRIP_RATE;
    const amount = amountFromTrip(r);
    const destination = esc(getRecordDestination(r));
    return `
      <tr>
        <td>${esc(String(r.docNumber || ''))}</td>
        <td>${esc(String(r.driverName || ''))}</td>
        <td>${esc(String(r.carNumber || ''))}</td>
        <td>${isFactory ? 'معمل' : 'محطة'}</td>
        <td>${destination}</td>
        <td>${qty.toFixed(3)}</td>
        <td>${fmtNumAr(priceRate, 0)}</td>
        <td>${fmtNumAr(amount, 0)}</td>
      </tr>
    `;
  }).join('');

  const totalAmount = records.reduce((s, r) => s + amountFromTrip(r), 0);
  const printWindow = window.open('', '_blank', 'width=1200,height=800');
  if (!printWindow) {
    window.alert('تعذر فتح نافذة الطباعة. تأكد من السماح بالنوافذ المنبثقة.');
    return;
  }

  printWindow.document.write(`
    <!DOCTYPE html>
    <html lang="ar" dir="rtl">
      <head>
        <meta charset="UTF-8" />
        <title>تقرير الشركة</title>
        <style>
          body { font-family: Tahoma, Arial, sans-serif; margin: 20px; color: #1d2c3a; }
          h1 { margin: 0 0 8px 0; font-size: 22px; }
          p { margin: 0 0 14px 0; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; }
          th, td { border: 1px solid #c6d2df; padding: 6px; text-align: right; }
          th { background: #eef4fa; }
          .totals { margin-top: 14px; font-weight: 700; }
        </style>
      </head>
      <body>
        <h1>تقرير عمليات السائقين - ${esc(state.month || '')}</h1>
        <p>تم الإنشاء: ${esc(new Date().toLocaleString('ar-IQ'))}</p>
        <table>
          <thead>
            <tr>
              <th>رقم الوصل</th>
              <th>اسم السائق</th>
              <th>رقم السيارة</th>
              <th>النوع</th>
              <th>الوجهة</th>
              <th>الكمية (طن)</th>
              <th>سعر الطن</th>
              <th>الإيراد</th>
            </tr>
          </thead>
          <tbody>${rowsHtml}</tbody>
        </table>
        <div class="totals">إجمالي الإيراد: ${fmtNumAr(totalAmount, 0)} د.ع</div>
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
}


async function loadTrips() {
  if (!state.month) return;
  const res = await api('getAllReceiptsData', { month: state.month });
  const allRows = Array.isArray(res.data) ? res.data : [];
  state.trips = applyTripEdits(filterTripsForCompanyScope(allRows));
  syncDriversFromTrips();
  await flushPendingSyncEdits();
  renderQuickKpis();
  renderOverview();
  renderCompanyTab();
  renderReports();
  renderAuditLog();
}

function switchTab(tab) {
  document.querySelectorAll('.tab').forEach((b) => b.classList.toggle('active', b.dataset.tab === tab));
  document.querySelectorAll('.panel').forEach((p) => p.classList.remove('active'));
  const panel = $('tab-' + tab);
  if (panel) panel.classList.add('active');

  const activeTabBtn = Array.from(document.querySelectorAll('.tab')).find((btn) => btn.dataset.tab === tab);
  const topTitle = document.querySelector('.erp-topbar h2');
  if (topTitle && activeTabBtn) {
    topTitle.textContent = `بوابة الشركات - ${String(activeTabBtn.textContent || '').trim()}`;
  }
}

async function initDashboard() {
  showGate('dashboard');
  applyManagerIdentityToUi();
  state.ui.auditVisible = false;
  closeTripEditModal();
  closeDriverEditModal();
  refreshDriverSelects();
  renderDrivers();
  renderMaintenance();
  await loadMonths();
  await loadTrips();
}

function bind() {
  populateFactoryOptions();
  toggleFactoryOtherField();

  $('btnActivate').addEventListener('click', verifyActivation);
  $('btnSetupManager').addEventListener('click', setupManager);
  $('btnManagerLogin').addEventListener('click', managerLogin);
  $('btnEditManagerName').addEventListener('click', openManagerEditModal);
  $('btnCloseManagerEditModal').addEventListener('click', closeManagerEditModal);
  $('btnSaveManagerName').addEventListener('click', updateManagerName);
  $('btnLogout').addEventListener('click', () => { state.authenticated = false; showGate('managerLogin'); });
  $('btnRefreshMonth').addEventListener('click', async () => { state.month = $('monthSelect').value; await loadTrips(); });
  $('monthSelect').addEventListener('change', async () => { state.month = $('monthSelect').value; await loadTrips(); });

  $('btnAddDriver').addEventListener('click', addDriver);
  $('btnSendTrip').addEventListener('click', sendTrip);
  $('btnSendFactory').addEventListener('click', sendFactory);
  $('factoryName').addEventListener('change', toggleFactoryOtherField);
  $('btnAddMaint').addEventListener('click', addMaintenance);
  $('btnLoadReport').addEventListener('click', renderReports);
  $('btnExportExcel').addEventListener('click', exportReportExcel);
  $('btnExportPdf').addEventListener('click', exportReportPdf);

  $('overviewDriverFilter').addEventListener('input', renderOverview);
  $('overviewCarFilter').addEventListener('input', renderOverview);
  $('overviewDocFilter').addEventListener('input', renderOverview);
  $('overviewSort').addEventListener('change', renderOverview);
  $('btnToggleAuditLog').addEventListener('click', () => {
    state.ui.auditVisible = !state.ui.auditVisible;
    renderAuditLog();
  });
  $('auditSearch').addEventListener('input', renderAuditLog);
  $('auditRange').addEventListener('change', renderAuditLog);
  $('btnCloseTripEditModal').addEventListener('click', closeTripEditModal);
  $('btnSaveTripEdit').addEventListener('click', saveTripEdit);
  $('btnCloseDriverEditModal').addEventListener('click', closeDriverEditModal);
  $('btnSaveDriverEdit').addEventListener('click', saveDriverEdit);

  $('overviewStats').addEventListener('click', (e) => {
    const btn = e.target.closest('.trip-edit-btn');
    if (!btn) return;
    openTripEditModal(btn.getAttribute('data-trip') || '');
  });

  $('tripEditModal').addEventListener('click', (e) => {
    if (e.target && e.target.id === 'tripEditModal') closeTripEditModal();
  });

  $('driverEditModal').addEventListener('click', (e) => {
    if (e.target && e.target.id === 'driverEditModal') closeDriverEditModal();
  });

  $('managerEditModal').addEventListener('click', (e) => {
    if (e.target && e.target.id === 'managerEditModal') closeManagerEditModal();
  });

  $('driversList').addEventListener('click', (e) => {
    const editBtn = e.target.closest('.driver-edit-btn');
    if (editBtn) {
      const editId = String(editBtn.getAttribute('data-id') || '').trim();
      if (editId) openDriverEditModal(editId);
      return;
    }

    const btn = e.target.closest('.driver-delete-btn');
    if (!btn) return;
    const id = String(btn.getAttribute('data-id') || '').trim();
    if (!id) return;
    deleteDriver(id);
  });

  document.querySelectorAll('.tab').forEach((btn) => btn.addEventListener('click', () => switchTab(btn.dataset.tab)));
}

document.addEventListener('DOMContentLoaded', async () => {
  bind();
  updateAuthDateTime();
  setInterval(updateAuthDateTime, 1000 * 30);
  try {
    const bootstrap = await window.managerDesktop.getBootstrap();
    state.bootstrap = bootstrap;
    if (bootstrap && bootstrap.activationScope) {
      state.activationScope = {
        appKey: String(bootstrap.activationScope.appKey || 'company').trim().toLowerCase() || 'company',
        companyId: String(bootstrap.activationScope.companyId || 'COMP-001').trim().toUpperCase(),
        packageName: String(bootstrap.activationScope.packageName || 'com.driver.portal.company.desktop').trim()
      };
    }
  } catch {
    // Keep defaults when bootstrap is unavailable.
  }

  // Load local state per company scope to prevent cross-company data leakage on shared devices.
  loadScopedStateFromStorage();

  $('activationCode').value = state.activationCode;
  if (!state.activationScope.companyId) {
    $('activationMsg').textContent = 'النسخة غير مهيأة: ضع companyId داخل manager-config.json قبل التفعيل.';
    showGate('activationGate');
    return;
  }

  if (state.activationCode) {
    try {
      const res = await api('companyActivationVerify', {
        activationCode: state.activationCode,
        appKey: state.activationScope.appKey,
        companyId: state.activationScope.companyId,
        packageName: state.activationScope.packageName,
        versionName: APP_VERSION_NAME,
        deviceId: getDeviceId()
      });
      state.activationAllowed = !!res.allowed;
      if (!state.activationAllowed) {
        $('activationMsg').textContent = res.message || 'كود التفعيل المخزن غير صالح.';
      }
    } catch (e) {
      const msg = String(e.message || e);
      $('activationMsg').textContent = 'تعذر التحقق من كود التفعيل المخزن: ' + msg;
      state.activationAllowed = false;
    }
  }

  if (!state.activationAllowed) {
    showGate('activationGate');
    return;
  }

  if (!state.manager) {
    showGate('managerSetup');
    return;
  }

  if (!state.authenticated) {
    applyManagerIdentityToUi();
    showGate('managerLogin');
    return;
  }

  await initDashboard();
});
