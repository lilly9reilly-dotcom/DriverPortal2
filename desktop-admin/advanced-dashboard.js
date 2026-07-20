const dashboardState = {
  isLoggedIn: false,
  currentPage: 'dashboard',
  tripsSection: {
    activeTab: 'taji'
  },
  vehiclesSection: {
    activeTab: 'taji',
    groupsById: {},
    month: ''
  },
  driversSection: {
    activeTab: 'taji'
  },
  reports: {
    activeDemandTab: 'halafaya',
    month: '',
    rows: [],
    rowsLoaded: false,
    factory: ''
  },
  activations: {
    rows: [],
    mode: 'remote'
  },
  data: {
    trips: [],
    vehicles: [],
    drivers: [],
    stats: {},
    transactions: [],
    maintenance: [],
    payments: []
  },
  raw: {
    receipts: [],
    receiptsAll: [],
    tripsAll: [],
    maintenance: [],
    summary: null,
    driversDirectory: []
  },
  cache: {
    allTrips: []
  },
  charts: {},
  api: {
    bootstrap: null,
    baseUrl: '',
    adminUrl: '',
    currentMonth: '',
    months: [],
    monthsDetected: []
  }
};

const dialogState = {
  root: null,
  resolver: null
};

async function initApp() {
  try {
    const bootstrap = await window.managerDesktop.getBootstrap();
    dashboardState.api.bootstrap = bootstrap;
    dashboardState.api.adminUrl = detectAdminUrl(bootstrap);
    dashboardState.api.baseUrl = detectApiBaseUrl(bootstrap);
    prefillSettingsFromBootstrap(bootstrap);
  } catch (error) {
    console.error('Failed to bootstrap:', error);
  }
}

function togglePassword() {
  const input = document.getElementById('password');
  const button = document.querySelector('.toggle-password-new');
  if (!input || !button) return;

  if (input.type === 'password') {
    input.type = 'text';
    button.innerHTML = '<i class="fas fa-eye-slash"></i>';
  } else {
    input.type = 'password';
    button.innerHTML = '<i class="fas fa-eye"></i>';
  }
}

document.getElementById('loginForm')?.addEventListener('submit', async (e) => {
  e.preventDefault();

  const password = document.getElementById('password').value;
  const errorEl = document.getElementById('loginError');
  errorEl.textContent = '';

  if (!password) {
    errorEl.innerHTML = '<i class="fas fa-exclamation-circle"></i> يرجى إدخال كلمة المرور';
    return;
  }

  try {
    const result = await window.managerDesktop.login(password);
    if (!result.success) {
      errorEl.innerHTML = '<i class="fas fa-times-circle"></i> ' + (result.message || 'فشل تسجيل الدخول');
      return;
    }

    await loginSuccess();
  } catch (error) {
    errorEl.innerHTML = '<i class="fas fa-exclamation-triangle"></i> خطأ في الاتصال: ' + String(error);
  }
});

async function loginSuccess() {
  dashboardState.isLoggedIn = true;
  document.getElementById('loginContainer').classList.add('hidden');
  document.getElementById('appContainer').classList.remove('hidden');
  initDashboard();
  await loadAllData();
}

async function logout() {
  if (!(await uiConfirm('هل تريد تسجيل الخروج؟', 'تأكيد الخروج'))) return;

  dashboardState.isLoggedIn = false;
  document.getElementById('appContainer').classList.add('hidden');
  document.getElementById('loginContainer').classList.remove('hidden');
  document.getElementById('loginForm').reset();
  document.getElementById('password').type = 'password';
}

function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('active');
}

function openTab(tabName) {
  const sections = document.querySelectorAll('.content-section');
  sections.forEach((section) => section.classList.remove('active'));

  const selectedSection = document.getElementById(`${tabName}-section`);
  if (selectedSection) selectedSection.classList.add('active');

  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach((item) => item.classList.remove('active'));

  const activeItem = document.querySelector(`[data-tab="${tabName}"]`);
  if (activeItem) activeItem.classList.add('active');

  const titles = {
    dashboard: 'بوابة التحكم الرئيسية',
    trips: 'قسم السائقين',
    vehicles: 'بوابة التطبيقات المستقبلية',
    drivers: 'بوابة الصلاحيات والمستخدمين',
    accounting: 'بوابة التدقيق والحوكمة',
    reports: 'قسم المطالبات',
    payments: 'بوابة الشركات والتفعيل',
    maintenance: 'بوابة تطبيق الغاز',
    tracking: 'بوابة النسخ الاحتياطي والاستعادة',
    settings: 'بوابة التكامل والبيانات'
  };

  document.getElementById('currentPage').textContent = titles[tabName] || 'الصفحة';
  dashboardState.currentPage = tabName;

  if (window.innerWidth <= 768) {
    document.getElementById('sidebar').classList.remove('active');
  }

  loadTabData(tabName);
}

function initDashboard() {
  openTab('dashboard');

  const today = new Date();
  const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
  document.getElementById('dateTo').valueAsDate = today;
  document.getElementById('dateFrom').valueAsDate = startOfMonth;

  document.getElementById('tableSearch')?.addEventListener('input', filterTable);
  document.getElementById('tripSearch')?.addEventListener('input', filterTrips);
  document.getElementById('tripStatus')?.addEventListener('change', renderTripsSectionViews);
  document.getElementById('vehicleSearch')?.addEventListener('input', renderVehiclesSectionViews);
  document.getElementById('vehicleStatus')?.addEventListener('change', renderVehiclesSectionViews);
  document.getElementById('driverSearch')?.addEventListener('input', filterDrivers);
  document.getElementById('driverStatus')?.addEventListener('change', renderDriversSectionViews);
}

async function loadAllData() {
  try {
    ensureApiReady();
    let month = '';
    try {
      month = await resolveWorkingMonth();
    } catch (monthError) {
      console.warn('resolveWorkingMonth failed, fallback to current date month:', monthError);
      month = dashboardState.api.currentMonth || monthKeyFromDate(new Date().toISOString());
      dashboardState.api.currentMonth = month;
      dashboardState.api.months = dashboardState.api.months && dashboardState.api.months.length
        ? dashboardState.api.months
        : [month];
    }

    const [receiptsRes, maintenanceRes, summaryRes, driversRes] = await Promise.allSettled([
      fetchApiAction('getAllReceiptsData', { month }),
      fetchApiAction('getMaintenanceData', { month }),
      fetchApiAction('getDashboardSummary', { month, half: 'all' }),
      fetchApiAction('drivers', {})
    ]);

    const receipts = settledArray(receiptsRes, 'data');
    const maintenance = settledArray(maintenanceRes, 'data');
    const summary = settledAny(summaryRes);
    const driversDirectory = normalizeDriversDirectory(settledAny(driversRes));

    dashboardState.raw.receipts = receipts;
    dashboardState.raw.maintenance = maintenance;
    dashboardState.raw.summary = summary;
    dashboardState.raw.driversDirectory = driversDirectory;

    await loadAllTripsPool_(month, receipts);

    dashboardState.data = buildViewModels(receipts, maintenance, driversDirectory);
    dashboardState.cache.allTrips = dashboardState.data.trips.slice();
    dashboardState.reports.rows = dashboardState.data.trips.slice();
    dashboardState.reports.month = month;
    dashboardState.reports.rowsLoaded = false;

    renderEverything();
  } catch (error) {
    console.error(error);
    await uiAlert('تعذر تحميل البيانات الحقيقية من Google Sheet.\nتأكد من رابط Apps Script ونشر Web App.\n\n' + String(error), 'خطأ تحميل البيانات');
  }
}

async function loadAllTripsPool_(currentMonth, currentMonthReceipts) {
  const months = Array.isArray(dashboardState.api.months) ? dashboardState.api.months.slice() : [];
  const rolling = [];
  const now = new Date();
  for (let i = 0; i < 48; i += 1) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    rolling.push(`${d.getFullYear()}_${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  const targets = Array.from(new Set([currentMonth, ...months, ...rolling].filter(Boolean)));

  try {
    const jobs = targets.map((m) => {
      if (m === currentMonth && Array.isArray(currentMonthReceipts)) {
        return Promise.resolve({ success: true, data: currentMonthReceipts });
      }
      return fetchApiAction('getAllReceiptsData', { month: m });
    });

    const settled = await Promise.allSettled(jobs);
    const receiptsAll = [];
    const seen = new Set();
    settled.forEach((res) => {
      if (res.status !== 'fulfilled') return;
      const rows = Array.isArray(res.value && res.value.data) ? res.value.data : [];
      rows.forEach((r) => {
        const key = `${String(r.sheetName || '')}::${String(r.row || '')}::${String(r.docNumber || '')}`;
        if (seen.has(key)) return;
        seen.add(key);
        receiptsAll.push(r);
      });
    });

    dashboardState.raw.receiptsAll = receiptsAll;
    dashboardState.raw.tripsAll = receiptsAll.map((r, index) => mapReceiptToTrip_(r, index));
    const monthsDetected = Array.from(new Set(
      dashboardState.raw.tripsAll
        .map((t) => monthKeyFromDate(t.sendTime))
        .filter(Boolean)
    )).sort((a, b) => b.localeCompare(a));
    dashboardState.api.monthsDetected = monthsDetected;
  } catch (error) {
    console.warn('loadAllTripsPool_ failed, fallback to current month only:', error);
    dashboardState.raw.receiptsAll = Array.isArray(currentMonthReceipts) ? currentMonthReceipts.slice() : [];
    dashboardState.raw.tripsAll = dashboardState.raw.receiptsAll.map((r, index) => mapReceiptToTrip_(r, index));
    dashboardState.api.monthsDetected = [currentMonth].filter(Boolean);
  }
}

function renderEverything() {
  safeRenderSection_('updateStats', updateStats);
  safeRenderSection_('updateRecentTrips', updateRecentTrips);
  safeRenderSection_('renderTripsSectionViews', renderTripsSectionViews);
  safeRenderSection_('renderVehiclesSectionViews', renderVehiclesSectionViews);
  safeRenderSection_('renderDriversSectionViews', renderDriversSectionViews);
  safeRenderSection_('initAccountingData', initAccountingData);
  safeRenderSection_('loadMaintenanceData', loadMaintenanceData);
  safeRenderSection_('initTracking', initTracking);
  safeRenderSection_('initCharts', initCharts);
  safeRenderSection_('initReports', initReports);
}

function safeRenderSection_(name, fn) {
  try {
    fn();
  } catch (error) {
    console.error('Render section failed:', name, error);
  }
}

function loadTabData(tabName) {
  if (tabName === 'trips') {
    renderTripsSectionViews();
  }
  if (tabName === 'vehicles') renderVehiclesSectionViews();
  if (tabName === 'drivers') renderDriversSectionViews();
  if (tabName === 'accounting') initAccountingData();
  if (tabName === 'reports') initReports();
  if (tabName === 'payments') loadActivationCodesDesktop();
  if (tabName === 'maintenance') loadMaintenanceData();
  if (tabName === 'tracking') initTracking();
  if (tabName === 'settings') prefillSettingsFromBootstrap(dashboardState.api.bootstrap);
}

function buildViewModels(receipts, maintenanceRows, driversDirectory) {
  const trips = (receipts || []).map((r, index) => mapReceiptToTrip_(r, index));

  const maintenance = (maintenanceRows || []).map((m, index) => ({
    id: index + 1,
    requestId: String(m.requestId || ''),
    vehicle: String(m.carNumber || ''),
    type: String(m.type || 'صيانة'),
    date: String(m.date || ''),
    cost: toNumber(m.cost),
    status: inferMaintenanceStatus(m),
    notes: String(m.notes || '')
  }));

  const vehiclesMap = new Map();
  trips.forEach((trip) => {
    const key = String(trip.carNumber || '').trim();
    if (!key) return;
    if (!vehiclesMap.has(key)) {
      vehiclesMap.set(key, {
        id: vehiclesMap.size + 1,
        number: key,
        type: 'صهريج',
        capacity: 0,
        status: 'active',
        owner: trip.owner || '-',
        lastMaintenance: '',
        trips: 0,
        revenue: 0
      });
    }

    const vehicle = vehiclesMap.get(key);
    vehicle.trips += 1;
    vehicle.revenue += toNumber(trip.price);
    if (!vehicle.owner && trip.owner) vehicle.owner = trip.owner;
  });

  maintenance.forEach((m) => {
    const key = String(m.vehicle || '').trim();
    if (!key) return;
    if (!vehiclesMap.has(key)) {
      vehiclesMap.set(key, {
        id: vehiclesMap.size + 1,
        number: key,
        type: 'صهريج',
        capacity: 0,
        status: 'maintenance',
        owner: '-',
        lastMaintenance: m.date,
        trips: 0,
        revenue: 0
      });
    }
    const vehicle = vehiclesMap.get(key);
    if (m.status !== 'completed') vehicle.status = 'maintenance';
    vehicle.lastMaintenance = pickLatestDate(vehicle.lastMaintenance, m.date);
  });

  const vehicles = Array.from(vehiclesMap.values());

  const driversMap = new Map();
  trips.forEach((trip) => {
    const key = normalizeText(trip.driverName);
    if (!key) return;

    if (!driversMap.has(key)) {
      const found = findDriverDirectoryRow(trip.driverName, driversDirectory);
      driversMap.set(key, {
        id: driversMap.size + 1,
        name: trip.driverName,
        phone: found.phone,
        license: found.license,
        vehicle: found.carNumber || trip.carNumber,
        trips: 0,
        salary: toNumber(found.salary),
        dues: 0
      });
    }

    const driver = driversMap.get(key);
    driver.trips += 1;
    driver.dues += toNumber(trip.price);
    if (!driver.vehicle && trip.carNumber) driver.vehicle = trip.carNumber;
  });

  const drivers = Array.from(driversMap.values());

  const transactions = [];
  trips.forEach((trip, index) => {
    transactions.push({
      id: index + 1,
      number: `TR-${trip.docNumber || index + 1}`,
      type: 'invoice',
      description: `نقلة #${trip.docNumber} - ${trip.driverName}`,
      amount: toNumber(trip.price),
      date: trip.sendTime,
      status: 'paid'
    });
  });

  maintenance.forEach((m) => {
    transactions.push({
      id: transactions.length + 1,
      number: `MT-${m.requestId || transactions.length + 1}`,
      type: 'expense',
      description: `${m.type} - ${m.vehicle || 'غير محدد'}`,
      amount: toNumber(m.cost),
      date: m.date,
      status: m.status === 'completed' ? 'paid' : 'pending'
    });
  });

  const payments = drivers.map((d, index) => ({
    id: index + 1,
    recipient: d.name,
    amount: toNumber(d.salary),
    method: 'تحويل بنكي',
    date: dashboardState.api.currentMonth,
    status: d.salary > 0 ? 'pending' : 'completed',
    notes: d.salary > 0 ? 'مستحق راتب' : 'لا يوجد راتب مسجل'
  }));

  return {
    trips,
    vehicles,
    drivers,
    transactions,
    maintenance,
    payments,
    stats: {}
  };
}

function updateStats() {
  const trips = dashboardState.data.trips;
  const vehicles = dashboardState.data.vehicles;
  const drivers = dashboardState.data.drivers;

  const totalRevenue = trips.reduce((sum, t) => sum + toNumber(t.price), 0);
  const activeVehicles = vehicles.filter((v) => v.status === 'active').length;

  document.getElementById('totalTrips').textContent = formatNumber(trips.length);
  document.getElementById('totalRevenue').textContent = formatCurrency(totalRevenue);
  document.getElementById('activeVehicles').textContent = formatNumber(activeVehicles);
  document.getElementById('totalDrivers').textContent = formatNumber(drivers.length);
}

function updateRecentTrips() {
  const tbody = document.getElementById('recentTripsBody');
  const trips = dashboardState.data.trips.slice(0, 10);

  if (!trips.length) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 20px; color: #999">لا توجد بيانات</td></tr>';
    return;
  }

  tbody.innerHTML = trips.map((trip) => `
    <tr>
      <td>${escapeHtml(trip.docNumber)}</td>
      <td>${escapeHtml(trip.driverName)}</td>
      <td>${escapeHtml(trip.carNumber)}</td>
      <td>${escapeHtml(trip.owner)}</td>
      <td>${escapeHtml(trip.station)}</td>
      <td>${formatNumber(trip.quantity)} طن</td>
      <td>${formatCurrency(trip.price)}</td>
      <td>${formatDate(trip.sendTime)}</td>
    </tr>
  `).join('');
}

function loadTripsGrid() {
  const grid = document.getElementById('tripsGrid');
  const trips = getTripsSectionRows();

  if (!trips.length) {
    grid.innerHTML = '<div class="data-card"><h4>لا توجد نقلات</h4><p>لا توجد نقلات للشهر المحدد.</p></div>';
    return;
  }

  grid.innerHTML = trips.map((trip) => `
    <div class="data-card ${isFactoryTrip_(trip) ? 'trip-card-factory' : ''}">
      <span class="card-badge badge-success">مكتمل</span>
      <h4>النقلة #${escapeHtml(trip.docNumber)}</h4>
      <p><strong>السائق:</strong> ${escapeHtml(trip.driverName)}</p>
      <p><strong>السيارة:</strong> ${escapeHtml(trip.carNumber)}</p>
      <p><strong>الوجهة:</strong> ${escapeHtml(trip.station)}</p>
      <p><strong>الكمية:</strong> ${formatNumber(trip.quantity)} طن</p>
      <p><strong>الإيراد:</strong> ${formatCurrency(trip.price)}</p>
      <p><strong>التاريخ:</strong> ${formatDate(trip.sendTime)}</p>
      <button class="btn btn-secondary" onclick="editTrip(${trip.id})" style="margin-top:10px; width:100%;">تعديل</button>
    </div>
  `).join('');
}

function populateAllTripsTable() {
  const tbody = document.getElementById('allTripsBody');
  if (!tbody) return;

  const trips = getTripsSectionRows();
  if (!trips.length) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:20px;">لا توجد بيانات</td></tr>';
    return;
  }

  tbody.innerHTML = trips.map((trip, index) => `
    <tr class="${isFactoryTrip_(trip) ? 'trip-row-factory' : ''}">
      <td>${index + 1}</td>
      <td>${escapeHtml(trip.docNumber)}</td>
      <td>${escapeHtml(trip.driverName)}</td>
      <td>${escapeHtml(trip.carNumber)}</td>
      <td>${escapeHtml(trip.owner || '-')}</td>
      <td>${escapeHtml(trip.station || '-')}</td>
      <td><span class="badge-success" style="padding:3px 8px;border-radius:4px;font-size:11px;">مكتمل</span></td>
      <td>${formatDate(trip.sendTime)}</td>
      <td><button class="btn btn-secondary" onclick="editTrip(${trip.id})">تعديل</button></td>
    </tr>
  `).join('');
}

function filterTrips() {
  renderTripsSectionViews();
}

async function refreshTripsData() {
  await loadAllData();
  await uiAlert('تم تحديث البيانات من Google Sheet', 'تم التحديث');
}

function openAddTripModal() {
  createTripInsideApp();
}

function isFactoryTrip_(trip) {
  const sourceSheet = normalizeText(trip && trip.sourceSheet ? trip.sourceSheet : '');
  const source = normalizeText(trip && trip.source ? trip.source : '');
  const station = normalizeText(trip && trip.station ? trip.station : '');
  return sourceSheet.startsWith('f_') || sourceSheet.startsWith('مع_') || source.includes('factory') || station.includes('معمل');
}

function getTripsSubTabLabel_(tab) {
  const labels = {
    taji: 'قسم التاجي',
    factory: 'قسم المعامل'
  };
  return labels[String(tab || 'taji')] || 'قسم التاجي';
}

function setTripsSubTab(tab) {
  dashboardState.tripsSection.activeTab = String(tab || 'taji');
  renderTripsSectionViews();
}

function isTajiTrip_(trip) {
  // Operationally, Taji section represents all non-factory movements.
  return !isFactoryTrip_(trip);
}

function getTripsSectionRows() {
  const rows = Array.isArray(dashboardState.data.trips) ? dashboardState.data.trips.slice() : [];
  const activeTab = String((dashboardState.tripsSection && dashboardState.tripsSection.activeTab) || 'taji');
  const q = normalizeText(document.getElementById('tripSearch')?.value || '');
  const statusFilter = String(document.getElementById('tripStatus')?.value || '').trim().toLowerCase();

  let filtered = rows.filter((trip) => {
    if (activeTab === 'factory' && !isFactoryTrip_(trip)) return false;
    if (activeTab === 'taji' && !isTajiTrip_(trip)) return false;
    return true;
  });

  if (q) {
    filtered = filtered.filter((trip) => {
      const text = [
        trip.docNumber,
        trip.driverName,
        trip.carNumber,
        trip.station,
        trip.owner,
        trip.vehicleOwner
      ].map((v) => normalizeText(v)).join(' ');
      return text.includes(q);
    });
  }

  if (statusFilter) {
    filtered = filtered.filter((trip) => {
      const tripStatus = String(trip.status || 'completed').toLowerCase();
      return tripStatus === statusFilter;
    });
  }

  return filtered;
}

function renderTripsSectionViews() {
  const allTrips = Array.isArray(dashboardState.data.trips) ? dashboardState.data.trips : [];
  const counts = {
    taji: allTrips.filter((trip) => isTajiTrip_(trip)).length,
    factory: allTrips.filter((trip) => isFactoryTrip_(trip)).length
  };

  ['taji', 'factory'].forEach((tab) => {
    const btn = document.getElementById('tripTab-' + tab);
    if (btn) btn.classList.toggle('active', tab === dashboardState.tripsSection.activeTab);
    const countEl = document.getElementById('tripCount-' + tab);
    if (countEl) countEl.textContent = String(counts[tab] || 0);
  });

  const rows = getTripsSectionRows();
  const qty = rows.reduce((sum, trip) => sum + toNumber(trip.quantity), 0);
  const amount = rows.reduce((sum, trip) => sum + toNumber(trip.price), 0);

  const totalEl = document.getElementById('tripSectionTotal');
  if (totalEl) totalEl.textContent = formatNumber(rows.length);
  const qtyEl = document.getElementById('tripSectionQty');
  if (qtyEl) qtyEl.textContent = formatNumber(qty);
  const amountEl = document.getElementById('tripSectionAmount');
  if (amountEl) amountEl.textContent = formatCurrency(amount);

  const label = getTripsSubTabLabel_(dashboardState.tripsSection.activeTab);
  const labelEl = document.getElementById('tripSectionLabel');
  if (labelEl) labelEl.textContent = label;
  const titleEl = document.getElementById('tripTableTitle');
  if (titleEl) titleEl.textContent = label;

  loadTripsGrid();
  populateAllTripsTable();
}

function getVehiclesSubTabLabel_(tab) {
  const labels = {
    taji: 'قسم التاجي',
    factory: 'قسم المعامل'
  };
  return labels[String(tab || 'taji')] || 'قسم التاجي';
}

function setVehiclesSubTab(tab) {
  dashboardState.vehiclesSection.activeTab = String(tab || 'taji');
  renderVehiclesSectionViews();
}

function ensureVehicleMonthFilter() {
  const select = document.getElementById('vehicleMonthFilter');
  if (!select) return;

  const monthsDetected = Array.isArray(dashboardState.api.monthsDetected) ? dashboardState.api.monthsDetected : [];
  const months = monthsDetected.length
    ? monthsDetected
    : (Array.isArray(dashboardState.api.months) ? dashboardState.api.months : []);
  const target = dashboardState.vehiclesSection.month || '';

  select.innerHTML = '<option value="">كل الأشهر المتاحة</option>' +
    months.map((m) => `<option value="${m}">${m}</option>`).join('');

  if (target && months.includes(target)) {
    select.value = target;
  } else {
    dashboardState.vehiclesSection.month = '';
    select.value = '';
  }
}

function onVehicleMonthFilterChange() {
  const month = String(document.getElementById('vehicleMonthFilter')?.value || '').trim();
  dashboardState.vehiclesSection.month = month;
  renderVehiclesSectionViews();
}

function getVehiclesSectionData_() {
  const allTrips = (Array.isArray(dashboardState.raw.tripsAll) && dashboardState.raw.tripsAll.length)
    ? dashboardState.raw.tripsAll.slice()
    : (Array.isArray(dashboardState.data.trips) ? dashboardState.data.trips.slice() : []);
  const vehicles = Array.isArray(dashboardState.data.vehicles) ? dashboardState.data.vehicles.slice() : [];
  const selectedMonth = String((dashboardState.vehiclesSection && dashboardState.vehiclesSection.month) || '').trim();
  const trips = selectedMonth
    ? allTrips.filter((trip) => {
      const key = monthKeyFromDate(trip.sendTime);
      return key === selectedMonth;
    })
    : allTrips;
  const ownerMaps = buildOwnerResolutionMaps_(trips);
  const activeTab = String((dashboardState.vehiclesSection && dashboardState.vehiclesSection.activeTab) || 'taji');
  const q = normalizeText(document.getElementById('vehicleSearch')?.value || '');
  const status = String(document.getElementById('vehicleStatus')?.value || '').trim().toLowerCase();

  const scopedTrips = trips.filter((trip) => {
    if (activeTab === 'factory') return isFactoryTrip_(trip);
    return isTajiTrip_(trip);
  });

  const vehicleByNumber = new Map();
  vehicles.forEach((v) => {
    const key = String(v.number || '').trim();
    if (!key) return;
    vehicleByNumber.set(key, v);
  });

  const byOwner = new Map();
  scopedTrips.forEach((trip) => {
    const ownerDisplay = resolveOwnerDisplay_(trip, ownerMaps);
    const ownerKey = ownerKeyFromDisplay_(ownerDisplay);
    const car = String(trip.carNumber || '').trim();
    if (!byOwner.has(ownerKey)) {
      byOwner.set(ownerKey, {
        ownerName: ownerDisplay,
        tripCount: 0,
        quantity: 0,
        amount: 0,
        drivers: new Map(),
        cars: new Set(),
        hasMaintenance: false
      });
    }
    const item = byOwner.get(ownerKey);
    item.tripCount += 1;
    item.quantity += toNumber(trip.quantity);
    item.amount += toNumber(trip.price);
    if (car) {
      item.cars.add(car);
      const vehicleMeta = vehicleByNumber.get(car);
      if (vehicleMeta && String(vehicleMeta.status || '').toLowerCase() === 'maintenance') {
        item.hasMaintenance = true;
      }
    }
    const driverKey = normalizeText(trip.driverName);
    if (driverKey) {
      const prev = item.drivers.get(driverKey) || { name: trip.driverName, trips: 0 };
      prev.trips += 1;
      item.drivers.set(driverKey, prev);
    }
  });

  let rows = Array.from(byOwner.values()).map((metrics, idx) => {
    const allDrivers = Array.from(metrics.drivers.values()).sort((a, b) => b.trips - a.trips);
    const carsList = Array.from(metrics.cars.values());
    const driversSummary = allDrivers.map((d) => `${d.name} (${d.trips})`).join(' - ');
    return {
      id: idx + 1,
      companyName: metrics.ownerName,
      status: metrics.hasMaintenance ? 'maintenance' : 'active',
      sectionTrips: metrics.tripCount,
      sectionQty: metrics.quantity,
      sectionAmount: metrics.amount,
      sectionCarsCount: carsList.length,
      sectionCarsPreview: carsList.slice(0, 3).join(' - '),
      sectionCarsAll: carsList
    };
  });

  // Keep highest-activity companies first for quick operational answers.
  rows.sort((a, b) => {
    const byTrips = toNumber(b.sectionTrips) - toNumber(a.sectionTrips);
    if (byTrips !== 0) return byTrips;

    const byQty = toNumber(b.sectionQty) - toNumber(a.sectionQty);
    if (byQty !== 0) return byQty;

    return toNumber(b.sectionAmount) - toNumber(a.sectionAmount);
  });

  if (status) {
    rows = rows.filter((vehicle) => String(vehicle.status || '').toLowerCase() === status);
  }

  if (q) {
    rows = rows.filter((vehicle) => {
      const text = [
        vehicle.companyName,
        vehicle.sectionCarsPreview,
        vehicle.status
      ].map((v) => normalizeText(v)).join(' ');
      return text.includes(q);
    });
  }

  dashboardState.vehiclesSection.groupsById = rows.reduce((acc, row) => {
    acc[row.id] = row;
    return acc;
  }, {});

  return {
    rows,
    scopedTrips
  };
}

function renderVehiclesSectionViews() {
  ensureVehicleMonthFilter();
  const allTripsRaw = (Array.isArray(dashboardState.raw.tripsAll) && dashboardState.raw.tripsAll.length)
    ? dashboardState.raw.tripsAll
    : (Array.isArray(dashboardState.data.trips) ? dashboardState.data.trips : []);
  const selectedMonth = String((dashboardState.vehiclesSection && dashboardState.vehiclesSection.month) || '').trim();
  const allTrips = selectedMonth
    ? allTripsRaw.filter((trip) => monthKeyFromDate(trip.sendTime) === selectedMonth)
    : allTripsRaw;
  const ownerMaps = buildOwnerResolutionMaps_(allTrips);
  const countDistinctOwners = (predicate) => {
    const set = new Set();
    allTrips.filter(predicate).forEach((trip) => {
      const owner = ownerKeyFromDisplay_(resolveOwnerDisplay_(trip, ownerMaps));
      if (owner) set.add(owner);
    });
    return set.size;
  };

  const counts = {
    taji: countDistinctOwners((trip) => isTajiTrip_(trip)),
    factory: countDistinctOwners((trip) => isFactoryTrip_(trip))
  };

  ['taji', 'factory'].forEach((tab) => {
    const btn = document.getElementById('vehicleTab-' + tab);
    if (btn) btn.classList.toggle('active', tab === dashboardState.vehiclesSection.activeTab);
    const countEl = document.getElementById('vehicleCount-' + tab);
    if (countEl) countEl.textContent = String(counts[tab] || 0);
  });

  const scoped = getVehiclesSectionData_();
  const rows = scoped.rows;
  const totalTrips = rows.reduce((sum, row) => sum + toNumber(row.sectionTrips), 0);
  const totalAmount = rows.reduce((sum, row) => sum + toNumber(row.sectionAmount), 0);

  const totalEl = document.getElementById('vehicleSectionTotal');
  if (totalEl) totalEl.textContent = formatNumber(rows.length);
  const tripsEl = document.getElementById('vehicleSectionTrips');
  if (tripsEl) tripsEl.textContent = formatNumber(totalTrips);
  const amountEl = document.getElementById('vehicleSectionAmount');
  if (amountEl) amountEl.textContent = formatCurrency(totalAmount);

  const labelEl = document.getElementById('vehicleSectionLabel');
  if (labelEl) labelEl.textContent = getVehiclesSubTabLabel_(dashboardState.vehiclesSection.activeTab);

  loadVehiclesGrid(rows);
}

function loadVehiclesGrid(rowsOverride) {
  const grid = document.getElementById('vehiclesGrid');
  const vehicles = Array.isArray(rowsOverride) ? rowsOverride : getVehiclesSectionData_().rows;

  if (!vehicles.length) {
    grid.innerHTML = '<div class="data-card"><h4>لا توجد سيارات</h4><p>لم يتم العثور على سيارات بهذا الشهر.</p></div>';
    return;
  }

  grid.innerHTML = vehicles.map((v) => `
    <div class="data-card ${dashboardState.vehiclesSection.activeTab === 'factory' ? 'vehicle-card-factory' : ''}">
      <span class="card-badge ${v.status === 'maintenance' ? 'badge-warning' : 'badge-success'}">${v.status === 'maintenance' ? 'صيانة' : 'صالح'}</span>
      <h4>${escapeHtml(v.companyName || '-')}</h4>
      <p><strong>عدد سيارات الشركة:</strong> ${formatNumber(v.sectionCarsCount || 0)}</p>
      <p><strong>أول السيارات:</strong> ${escapeHtml(v.sectionCarsPreview || '-')}</p>
      <p><strong>إجمالي نقلات الشركة:</strong> ${formatNumber(v.sectionTrips || 0)}</p>
      <p><strong>إجمالي كمية الشركة:</strong> ${formatNumber(v.sectionQty || 0, 3)} طن</p>
      <p><strong>إجمالي إيراد الشركة:</strong> ${formatCurrency(v.sectionAmount || 0)}</p>
      <p><strong>حالة الشركة:</strong> ${v.status === 'maintenance' ? 'ضمنها سيارة صيانة' : 'نشطة'}</p>
      <button class="btn btn-secondary" onclick="viewVehicleSectionDetails(${v.id})" style="margin-top:10px; width:100%;">تفاصيل الشركة</button>
    </div>
  `).join('');
}

function viewVehicleSectionDetails(id) {
  const group = dashboardState.vehiclesSection.groupsById ? dashboardState.vehiclesSection.groupsById[id] : null;
  if (!group) return;

  const activeTab = String((dashboardState.vehiclesSection && dashboardState.vehiclesSection.activeTab) || 'taji');
  const ownerKey = ownerKeyFromDisplay_(group.companyName);
  const baseTrips = (Array.isArray(dashboardState.raw.tripsAll) && dashboardState.raw.tripsAll.length)
    ? dashboardState.raw.tripsAll
    : (dashboardState.data.trips || []);
  const ownerMaps = buildOwnerResolutionMaps_(baseTrips);
  const selectedMonth = String((dashboardState.vehiclesSection && dashboardState.vehiclesSection.month) || '').trim();
  const trips = baseTrips.filter((trip) => {
    const tripOwnerKey = ownerKeyFromDisplay_(resolveOwnerDisplay_(trip, ownerMaps));
    if (tripOwnerKey !== ownerKey) return false;
    if (selectedMonth && monthKeyFromDate(trip.sendTime) !== selectedMonth) return false;
    return activeTab === 'factory' ? isFactoryTrip_(trip) : isTajiTrip_(trip);
  });

  const sorted = trips.slice().sort((a, b) => {
    const da = parseDateLoose(a.sendTime);
    const db = parseDateLoose(b.sendTime);
    const ta = da ? da.getTime() : 0;
    const tb = db ? db.getTime() : 0;
    return tb - ta;
  });

  const details = sorted.map((trip, idx) => {
    return `${idx + 1}. ${trip.docNumber || '-'} | ${formatNumber(trip.quantity || 0, 3)} طن | ${formatCurrency(trip.price || 0)} | ${trip.station || '-'} | ${formatDate(trip.sendTime || '')}`;
  }).join('\n');

  const totalQty = trips.reduce((sum, trip) => sum + toNumber(trip.quantity), 0);
  const totalAmount = trips.reduce((sum, trip) => sum + toNumber(trip.price), 0);
  const title = `${group.companyName} - ${getVehiclesSubTabLabel_(activeTab)}`;
  const header = [
    `الشركة/المالك: ${group.companyName || '-'}`,
    `الشهر: ${selectedMonth || 'كل الأشهر'}`,
    `عدد السيارات: ${formatNumber((group.sectionCarsAll || []).length)}`,
    `عدد النقلات: ${formatNumber(trips.length)}`,
    `إجمالي الكمية: ${formatNumber(totalQty, 3)} طن`,
    `إجمالي الإيراد: ${formatCurrency(totalAmount)}`,
    ''
  ].join('\n');

  uiAlert(header + (details || 'لا توجد نقلات مطابقة في هذا القسم.'), title);
}

function updateVehicleStats() {
  const vehicles = dashboardState.data.vehicles;
  document.getElementById('totalVehicles').textContent = formatNumber(vehicles.length);
  document.getElementById('workingVehicles').textContent = formatNumber(vehicles.filter((v) => v.status === 'active').length);
  document.getElementById('maintenanceVehicles').textContent = formatNumber(vehicles.filter((v) => v.status === 'maintenance').length);
  document.getElementById('brokenVehicles').textContent = formatNumber(vehicles.filter((v) => v.status === 'broken').length);
}

function openAddVehicleModal() {
  uiAlert('إضافة سيارة مباشرة تحتاج endpoint مخصص في Apps Script. حالياً تُنشأ السيارة تلقائياً عند تسجيل أول نقلة لها.', 'معلومة');
}

function loadDriversGrid() {
  const grid = document.getElementById('driversGrid');
  const drivers = getDriversSectionRows();

  if (!drivers.length) {
    grid.innerHTML = '<div class="data-card"><h4>لا توجد بيانات سائقين</h4><p>لم يتم العثور على سائقين بهذا الشهر.</p></div>';
    populateDriversTable([]);
    return;
  }

  grid.innerHTML = drivers.map((d) => `
    <div class="data-card ${d.vehicle ? '' : 'driver-card-unassigned'}">
      <h4>${escapeHtml(d.name)}</h4>
      <p><strong>الهاتف:</strong> ${escapeHtml(d.phone || '-')}</p>
      <p><strong>الرخصة:</strong> ${escapeHtml(d.license || '-')}</p>
      <p><strong>السيارة:</strong> ${escapeHtml(d.vehicle || '-')}</p>
      <p><strong>عدد النقلات:</strong> ${formatNumber(d.trips)}</p>
      <p><strong>الراتب:</strong> ${formatCurrency(d.salary)}</p>
      <button class="btn btn-secondary" onclick="editDriver(${d.id})" style="margin-top:10px; width:100%;">عرض</button>
    </div>
  `).join('');

  populateDriversTable(drivers);
}

function populateDriversTable(rowsOverride) {
  const tbody = document.getElementById('driversBody');
  const drivers = Array.isArray(rowsOverride) ? rowsOverride : getDriversSectionRows();

  if (!drivers.length) {
    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:20px;">لا توجد بيانات</td></tr>';
    return;
  }

  tbody.innerHTML = drivers.map((d, index) => `
    <tr class="${d.vehicle ? '' : 'driver-row-unassigned'}">
      <td>${index + 1}</td>
      <td>${escapeHtml(d.name)}</td>
      <td>${escapeHtml(d.phone || '-')}</td>
      <td>${escapeHtml(d.license || '-')}</td>
      <td>${escapeHtml(d.vehicle || '-')}</td>
      <td>${formatNumber(d.trips)}</td>
      <td>${formatCurrency(d.salary)}</td>
      <td><span class="${d.vehicle ? 'badge-success' : 'badge-warning'}" style="padding:3px 8px;border-radius:4px;font-size:11px;">${d.vehicle ? 'مرتبط' : 'غير مرتبط'}</span></td>
      <td><button class="btn btn-secondary" onclick="editDriver(${d.id})">عرض</button></td>
    </tr>
  `).join('');
}

function filterDrivers() {
  renderDriversSectionViews();
}

function getDriversSubTabLabel_(tab) {
  const labels = {
    taji: 'قسم التاجي',
    factory: 'قسم المعامل'
  };
  return labels[String(tab || 'taji')] || 'قسم التاجي';
}

function setDriversSubTab(tab) {
  dashboardState.driversSection.activeTab = String(tab || 'taji');
  renderDriversSectionViews();
}

function getDriversSectionRows() {
  const activeTab = String((dashboardState.driversSection && dashboardState.driversSection.activeTab) || 'taji');
  const allTrips = Array.isArray(dashboardState.data.trips) ? dashboardState.data.trips.slice() : [];
  const directory = Array.isArray(dashboardState.data.drivers) ? dashboardState.data.drivers.slice() : [];
  const q = normalizeText(document.getElementById('driverSearch')?.value || '');
  const statusFilter = String(document.getElementById('driverStatus')?.value || '').trim().toLowerCase();

  const scopedTrips = allTrips.filter((trip) => {
    return activeTab === 'factory' ? isFactoryTrip_(trip) : isTajiTrip_(trip);
  });

  const byDriver = new Map();
  const salaryByDriver = new Map();
  directory.forEach((d) => salaryByDriver.set(normalizeText(d.name), d));

  scopedTrips.forEach((trip) => {
    const key = normalizeText(trip.driverName);
    if (!key) return;
    if (!byDriver.has(key)) {
      const directoryRow = salaryByDriver.get(key);
      byDriver.set(key, {
        id: byDriver.size + 1,
        name: trip.driverName,
        phone: directoryRow ? directoryRow.phone : '',
        license: directoryRow ? directoryRow.license : '',
        vehicle: '',
        trips: 0,
        salary: toNumber(directoryRow ? directoryRow.salary : 0),
        dues: 0,
        vehicles: new Set()
      });
    }

    const row = byDriver.get(key);
    row.trips += 1;
    row.dues += toNumber(trip.price);
    if (trip.carNumber) row.vehicles.add(String(trip.carNumber));
  });

  let filtered = Array.from(byDriver.values()).map((row) => {
    const cars = Array.from(row.vehicles.values());
    return {
      ...row,
      vehicle: cars.slice(0, 2).join(' - '),
      vehiclesCount: cars.length
    };
  });

  if (q) {
    filtered = filtered.filter((driver) => {
      const text = [
        driver.name,
        driver.phone,
        driver.license,
        driver.vehicle
      ].map((v) => normalizeText(v)).join(' ');
      return text.includes(q);
    });
  }

  if (statusFilter === 'due') {
    filtered = filtered.filter((driver) => toNumber(driver.salary) > 0);
  }
  if (statusFilter === 'zero') {
    filtered = filtered.filter((driver) => toNumber(driver.salary) <= 0);
  }

  return filtered;
}

function renderDriversSectionViews() {
  const allTrips = Array.isArray(dashboardState.data.trips) ? dashboardState.data.trips : [];
  const counts = {
    taji: allTrips.filter((trip) => isTajiTrip_(trip)).length,
    factory: allTrips.filter((trip) => isFactoryTrip_(trip)).length
  };

  ['taji', 'factory'].forEach((tab) => {
    const btn = document.getElementById('driverTab-' + tab);
    if (btn) btn.classList.toggle('active', tab === dashboardState.driversSection.activeTab);
    const countEl = document.getElementById('driverCount-' + tab);
    if (countEl) countEl.textContent = String(counts[tab] || 0);
  });

  const rows = getDriversSectionRows();
  const totalTrips = rows.reduce((sum, driver) => sum + toNumber(driver.trips), 0);
  const totalSalary = rows.reduce((sum, driver) => sum + toNumber(driver.salary), 0);

  const totalEl = document.getElementById('driverSectionTotal');
  if (totalEl) totalEl.textContent = formatNumber(rows.length);
  const tripsEl = document.getElementById('driverSectionTrips');
  if (tripsEl) tripsEl.textContent = formatNumber(totalTrips);
  const salaryEl = document.getElementById('driverSectionSalary');
  if (salaryEl) salaryEl.textContent = formatCurrency(totalSalary);

  const label = getDriversSubTabLabel_(dashboardState.driversSection.activeTab);
  const labelEl = document.getElementById('driverSectionLabel');
  if (labelEl) labelEl.textContent = label;
  const titleEl = document.getElementById('driverTableTitle');
  if (titleEl) titleEl.textContent = 'قائمة السائقين - ' + label;

  loadDriversGrid();
}

function openAddDriverModal() {
  uiAlert('إضافة سائق مباشرة تحتاج endpoint مخصص في Apps Script. حالياً يُنشأ السائق تلقائياً عند تسجيل أول نقلة باسمه.', 'معلومة');
}

function generatePayroll() {
  const scoped = getDriversSectionRows();
  const lines = scoped.map((d, i) => `${i + 1}. ${d.name} - ${formatCurrency(d.salary)}`);
  uiAlert(lines.length ? `كشف الرواتب\n\n${lines.join('\n')}` : 'لا توجد رواتب حالية', 'الرواتب');
}

function initAccountingData() {
  loadActivationAuditDesktop();
}

async function loadActivationAuditDesktop() {
  const tbody = document.getElementById('auditTrailBodyDesktop');
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px;">جاري تحميل سجل التدقيق...</td></tr>';

  try {
    const result = await fetchApiAction('companyActivationAuditList', { limit: 300 });
    const rows = Array.isArray(result && result.data) ? result.data : [];
    renderActivationAuditDesktop(rows);
  } catch (error) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center;padding:20px;color:#c0392b;">تعذر تحميل سجل التدقيق: ${escapeHtml(String(error))}</td></tr>`;
  }
}

function renderActivationAuditDesktop(rows) {
  const tbody = document.getElementById('auditTrailBodyDesktop');
  if (!tbody) return;

  const created = rows.filter((x) => String(x.action || '') === 'create_code').length;
  const disabled = rows.filter((x) => String(x.action || '') === 'disable_code').length;
  const scoped = rows.filter((x) => String(x.action || '') === 'update_scope').length;
  const unbound = rows.filter((x) => String(x.action || '') === 'unbind_device').length;

  document.getElementById('totalIncome').textContent = formatNumber(created);
  document.getElementById('totalExpenses').textContent = formatNumber(disabled);
  document.getElementById('netBalance').textContent = formatNumber(scoped);
  document.getElementById('pendingPayroll').textContent = formatNumber(unbound);

  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px;">لا توجد عمليات تدقيق بعد</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map((item, index) => {
    const action = String(item.action || 'unknown');
    const labels = {
      create_code: 'إنشاء كود',
      enable_code: 'تفعيل كود',
      disable_code: 'إيقاف كود',
      unbind_device: 'فك ارتباط',
      update_scope: 'تحديث نطاق'
    };
    return `
      <tr>
        <td>${index + 1}</td>
        <td>${formatDate(item.timestamp)}</td>
        <td>${escapeHtml(labels[action] || action)}</td>
        <td>${escapeHtml(String(item.code || '-'))}</td>
        <td>${escapeHtml(String(item.actor || 'admin'))}</td>
        <td>${escapeHtml(String(item.appKey || '-'))}</td>
        <td>${escapeHtml(String(item.companyId || '-'))}</td>
        <td>${escapeHtml(String(item.details || '-'))}</td>
      </tr>
    `;
  }).join('');
}

function initReports() {
  ensureDemandMonthFilter();
  renderDemandSection();
}

function changeReportType() {
  renderDemandSection();
}

function loadReport() {
  renderDemandSection();
}

function getFactoryName_(trip) {
  return String((trip && trip.station) || '').trim();
}

function mapReceiptToTrip_(r, index) {
  return {
    id: index + 1,
    row: toNumber(r.row),
    sheetName: String(r.sheetName || ''),
    docNumber: String(r.docNumber || ''),
    driverName: String(r.driverName || ''),
    carNumber: String(r.carNumber || ''),
    owner: String(r.owner || r.vehicleOwner || ''),
    vehicleOwner: String(r.vehicleOwner || ''),
    station: String(r.destination || r.station || ''),
    quantity: toNumber(r.netQuantity || r.quantity),
    grossQuantity: toNumber(r.quantity),
    price: toNumber(r.price || r.finalAmount || r.storedPrice),
    gasCost: toNumber(r.gasCost),
    kroa: toNumber(r.kroa),
    distance: toNumber(r.distance),
    sendTime: String(r.timestamp || r.unloadDate || r.loadDate || ''),
    status: 'completed',
    sourceSheet: String(r.sheetName || ''),
    notes: String(r.notes || ''),
    source: String(r.source || '')
  };
}

function ownerKeyFromDisplay_(value) {
  const base = String(value || '').trim();
  if (!base) return 'غير_محدد';

  const normalized = base
    .replace(/[أإآ]/g, 'ا')
    .replace(/ى/g, 'ي')
    .replace(/ة/g, 'ه')
    .replace(/\b(شركة|شركه)\b/g, ' ')
    .replace(/[^\u0600-\u06FFa-zA-Z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  return normalizeText(normalized) || 'غير_محدد';
}

function isOwnerCodeLike_(value) {
  const raw = String(value || '').trim();
  if (!raw) return true;
  if (/^\d+$/.test(raw)) return true;
  if (/^[a-z0-9\-_/]+$/i.test(raw) && !/\s/.test(raw) && /\d/.test(raw)) return true;
  return false;
}

function buildOwnerResolutionMaps_(trips) {
  const byCarCandidates = new Map();
  const byDriverCandidates = new Map();

  (trips || []).forEach((trip) => {
    const car = String(trip && trip.carNumber ? trip.carNumber : '').trim();
    const driverKey = normalizeText(trip && trip.driverName ? trip.driverName : '');
    if (!car) return;

    const ownerCandidates = [trip.owner, trip.vehicleOwner]
      .map((v) => String(v || '').trim())
      .filter(Boolean)
      .filter((v) => !isOwnerCodeLike_(v));

    if (!ownerCandidates.length) return;
    if (!byCarCandidates.has(car)) byCarCandidates.set(car, new Map());
    const map = byCarCandidates.get(car);

    ownerCandidates.forEach((name) => {
      const key = ownerKeyFromDisplay_(name);
      const prev = map.get(key) || { name, count: 0 };
      prev.count += 1;
      if (name.length > prev.name.length) prev.name = name;
      map.set(key, prev);

      if (driverKey) {
        if (!byDriverCandidates.has(driverKey)) byDriverCandidates.set(driverKey, new Map());
        const dMap = byDriverCandidates.get(driverKey);
        const dPrev = dMap.get(key) || { name, count: 0 };
        dPrev.count += 1;
        if (name.length > dPrev.name.length) dPrev.name = name;
        dMap.set(key, dPrev);
      }
    });
  });

  const carPreferredOwner = new Map();
  byCarCandidates.forEach((candidates, car) => {
    const best = Array.from(candidates.values()).sort((a, b) => b.count - a.count)[0];
    if (best && best.name) carPreferredOwner.set(car, best.name);
  });

  const driverPreferredOwner = new Map();
  byDriverCandidates.forEach((candidates, driverKey) => {
    const best = Array.from(candidates.values()).sort((a, b) => b.count - a.count)[0];
    if (best && best.name) driverPreferredOwner.set(driverKey, best.name);
  });

  return { carPreferredOwner, driverPreferredOwner };
}

function resolveOwnerDisplay_(trip, maps) {
  const owner = String(trip && trip.owner ? trip.owner : '').trim();
  const vehicleOwner = String(trip && trip.vehicleOwner ? trip.vehicleOwner : '').trim();
  const car = String(trip && trip.carNumber ? trip.carNumber : '').trim();

  if (owner && !isOwnerCodeLike_(owner)) return owner;
  if (vehicleOwner && !isOwnerCodeLike_(vehicleOwner)) return vehicleOwner;

  const driverKey = normalizeText(trip && trip.driverName ? trip.driverName : '');
  if (driverKey && maps && maps.driverPreferredOwner && maps.driverPreferredOwner.has(driverKey)) {
    return String(maps.driverPreferredOwner.get(driverKey) || '').trim() || owner || vehicleOwner;
  }

  if (car && maps && maps.carPreferredOwner && maps.carPreferredOwner.has(car)) {
    return String(maps.carPreferredOwner.get(car) || '').trim() || owner || vehicleOwner || car;
  }

  if (owner && !isOwnerCodeLike_(owner)) return owner;
  if (vehicleOwner && !isOwnerCodeLike_(vehicleOwner)) return vehicleOwner;
  return 'غير محدد';
}

function setDemandTab(tabId) {
  dashboardState.reports.activeDemandTab = String(tabId || 'halafaya');
  if (dashboardState.reports.activeDemandTab !== 'factories') {
    dashboardState.reports.factory = '';
  }
  renderDemandSection();
}

function ensureDemandMonthFilter() {
  const select = document.getElementById('demandMonthFilter');
  if (!select) return;

  const months = Array.isArray(dashboardState.api.months) ? dashboardState.api.months : [];
  const targetMonth = dashboardState.reports.month || dashboardState.api.currentMonth || months[0] || '';

  select.innerHTML = months.map((m) => `<option value="${m}">${m}</option>`).join('');
  if (!months.length) {
    select.innerHTML = '<option value="">لا توجد أشهر متاحة</option>';
    return;
  }

  if (!months.includes(targetMonth)) {
    dashboardState.reports.month = months[0];
    select.value = months[0];
  } else {
    dashboardState.reports.month = targetMonth;
    select.value = targetMonth;
  }
}

async function onDemandMonthFilterChange() {
  const month = String(document.getElementById('demandMonthFilter')?.value || '').trim();
  if (!month) return;

  try {
    ensureApiReady();
    const result = await fetchApiAction('getAllReceiptsData', { month });
    const receipts = Array.isArray(result && result.data) ? result.data : [];

    dashboardState.reports.month = month;
    dashboardState.reports.factory = '';
    dashboardState.reports.rows = receipts.map((r, index) => ({
      id: index + 1,
      row: toNumber(r.row),
      sheetName: String(r.sheetName || ''),
      sourceSheet: String(r.sheetName || ''),
      source: String(r.source || ''),
      docNumber: String(r.docNumber || ''),
      driverName: String(r.driverName || ''),
      carNumber: String(r.carNumber || ''),
      owner: String(r.owner || r.vehicleOwner || ''),
      vehicleOwner: String(r.vehicleOwner || ''),
      station: String(r.destination || r.station || ''),
      quantity: toNumber(r.netQuantity || r.quantity),
      price: toNumber(r.price || r.finalAmount || r.storedPrice),
      sendTime: String(r.timestamp || r.unloadDate || r.loadDate || ''),
      status: 'completed'
    }));
    dashboardState.reports.rowsLoaded = true;

    renderDemandSection();
  } catch (error) {
    await uiAlert('تعذر تحميل بيانات شهر المطالبات: ' + String(error), 'قسم المطالبات');
  }
}

function onDemandFactoryFilterChange() {
  const value = String(document.getElementById('demandFactoryFilter')?.value || '').trim();
  dashboardState.reports.factory = value;
  renderDemandSection();
}

function updateDemandFactoryFilter_(allTrips) {
  const select = document.getElementById('demandFactoryFilter');
  if (!select) return;

  const activeTab = dashboardState.reports.activeDemandTab || 'halafaya';
  if (activeTab !== 'factories') {
    select.style.display = 'none';
    return;
  }

  const factories = Array.from(new Set(
    (allTrips || [])
      .filter((trip) => classifyDemandTab(trip) === 'factories')
      .map((trip) => getFactoryName_(trip))
      .filter(Boolean)
  )).sort((a, b) => a.localeCompare(b, 'ar'));

  select.innerHTML = '';
  const defaultOption = document.createElement('option');
  defaultOption.value = '';
  defaultOption.textContent = 'كل المعامل';
  select.appendChild(defaultOption);

  factories.forEach((name) => {
    const option = document.createElement('option');
    option.value = name;
    option.textContent = name;
    select.appendChild(option);
  });

  if (dashboardState.reports.factory && factories.includes(dashboardState.reports.factory)) {
    select.value = dashboardState.reports.factory;
  } else {
    dashboardState.reports.factory = '';
    select.value = '';
  }

  select.style.display = '';
}

function classifyDemandTab(trip) {
  const station = normalizeText(trip && trip.station ? trip.station : '');
  const sheetName = normalizeText(trip && trip.sourceSheet ? trip.sourceSheet : '');
  const source = normalizeText(trip && trip.source ? trip.source : '');

  if (source.includes('factory') || sheetName.startsWith('f_') || sheetName.startsWith('مع_') || station.includes('معمل')) {
    return 'factories';
  }
  if (station.includes('حلفاي')) return 'halafaya';
  if (station.includes('رصاف')) return 'rusafa';
  if (station.includes('دور')) return 'dora';
  return 'other';
}

function demandTabLabel(tabId) {
  const labels = {
    halafaya: 'مطالبة حلفاية',
    rusafa: 'مطالبة الرصافة',
    dora: 'مطالبة الدورة',
    factories: 'قسم المعامل'
  };
  return labels[String(tabId || '')] || 'قسم المطالبات';
}

function renderDemandSection() {
  const tabs = ['halafaya', 'rusafa', 'dora', 'factories'];
  const allTrips = dashboardState.reports.rowsLoaded
    ? dashboardState.reports.rows
    : (dashboardState.data.trips || []);

  updateDemandFactoryFilter_(allTrips);

  tabs.forEach((tab) => {
    const btn = document.getElementById('demandTab-' + tab);
    if (btn) btn.classList.toggle('active', tab === dashboardState.reports.activeDemandTab);

    const countEl = document.getElementById('demandCount-' + tab);
    if (countEl) {
      const count = allTrips.filter((trip) => classifyDemandTab(trip) === tab).length;
      countEl.textContent = String(count);
    }
  });

  const activeTab = dashboardState.reports.activeDemandTab || 'halafaya';
  let rows = allTrips.filter((trip) => classifyDemandTab(trip) === activeTab);
  if (activeTab === 'factories' && dashboardState.reports.factory) {
    rows = rows.filter((trip) => getFactoryName_(trip) === dashboardState.reports.factory);
  }

  const totalQty = rows.reduce((sum, trip) => sum + toNumber(trip.quantity), 0);
  const totalAmount = rows.reduce((sum, trip) => sum + toNumber(trip.price), 0);
  const uniqueDrivers = new Set(rows.map((trip) => normalizeText(trip.driverName)).filter(Boolean)).size;

  const titleEl = document.getElementById('demandTableTitle');
  if (titleEl) titleEl.textContent = 'تفاصيل ' + demandTabLabel(activeTab);

  const currentLabel = document.getElementById('demandCurrentLabel');
  if (currentLabel) currentLabel.textContent = demandTabLabel(activeTab);

  const totalCountEl = document.getElementById('demandTotalCount');
  if (totalCountEl) totalCountEl.textContent = formatNumber(rows.length);

  const totalQtyEl = document.getElementById('demandTotalQty');
  if (totalQtyEl) totalQtyEl.textContent = formatNumber(totalQty, 3);

  const totalAmountEl = document.getElementById('demandTotalAmount');
  if (totalAmountEl) totalAmountEl.textContent = formatCurrency(totalAmount);

  const factoryCountEl = document.getElementById('demandFactoryCount');
  if (factoryCountEl) factoryCountEl.textContent = formatNumber(uniqueDrivers);

  renderDemandsTable(rows);
}

function renderDemandsTable(rows) {
  const tbody = document.getElementById('demandsDataBody');
  if (!tbody) return;

  if (!rows || !rows.length) {
    tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;padding:20px;">لا توجد وصولات لهذا القسم</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map((trip, index) => {
    const isFactory = classifyDemandTab(trip) === 'factories';
    return `
      <tr class="${isFactory ? 'demand-row-factory' : ''}">
        <td>${index + 1}</td>
        <td>${escapeHtml(trip.docNumber || '')}</td>
        <td>${escapeHtml(trip.driverName || '')}</td>
        <td>${escapeHtml(trip.carNumber || '')}</td>
        <td>${escapeHtml(trip.station || '-')}</td>
        <td>${formatNumber(trip.quantity || 0, 3)} طن</td>
        <td>${formatCurrency(trip.price || 0)}</td>
        <td>${escapeHtml(trip.owner || trip.vehicleOwner || '-')}</td>
        <td>${formatDate(trip.sendTime || '')}</td>
        <td><span class="demand-type-badge ${isFactory ? 'demand-type-factory' : 'demand-type-normal'}">${isFactory ? 'معمل' : 'اعتيادي'}</span></td>
      </tr>
    `;
  }).join('');
}

function initReportCharts() {
  const trips = dashboardState.data.trips;
  const maintenance = dashboardState.data.maintenance;

  const weekly = [0, 0, 0, 0];
  trips.forEach((trip) => {
    const day = extractDay(trip.sendTime);
    if (!day) return;
    const index = Math.min(3, Math.floor((day - 1) / 7));
    weekly[index] += toNumber(trip.price);
  });

  const fuelCost = trips.reduce((s, t) => s + toNumber(t.gasCost), 0);
  const kroaCost = trips.reduce((s, t) => s + toNumber(t.kroa), 0);
  const maintenanceCost = maintenance.reduce((s, m) => s + toNumber(m.cost), 0);
  const otherCost = Math.max(0, Math.round((fuelCost + kroaCost + maintenanceCost) * 0.05));

  const ctx1 = document.getElementById('revenueComparisonChart')?.getContext('2d');
  const ctx2 = document.getElementById('expenseDistributionChart')?.getContext('2d');

  if (ctx1) {
    if (dashboardState.charts.revenueComparison) dashboardState.charts.revenueComparison.destroy();
    dashboardState.charts.revenueComparison = new Chart(ctx1, {
      type: 'bar',
      data: {
        labels: ['الأسبوع 1', 'الأسبوع 2', 'الأسبوع 3', 'الأسبوع 4'],
        datasets: [{
          label: 'الإيرادات الفعلية',
          data: weekly,
          backgroundColor: '#667eea'
        }]
      },
      options: { responsive: true, maintainAspectRatio: true }
    });
  }

  if (ctx2) {
    if (dashboardState.charts.expenseDistribution) dashboardState.charts.expenseDistribution.destroy();
    dashboardState.charts.expenseDistribution = new Chart(ctx2, {
      type: 'doughnut',
      data: {
        labels: ['وقود', 'كروة', 'صيانة', 'أخرى'],
        datasets: [{
          data: [fuelCost, kroaCost, maintenanceCost, otherCost],
          backgroundColor: ['#667eea', '#f093fb', '#4facfe', '#95a5a6']
        }]
      },
      options: { responsive: true, maintainAspectRatio: true }
    });
  }
}

function populateReportData() {
  const tbody = document.getElementById('reportDataBody');
  const income = dashboardState.data.transactions.filter((t) => t.type === 'invoice').reduce((s, t) => s + toNumber(t.amount), 0);
  const expense = dashboardState.data.transactions.filter((t) => t.type !== 'invoice').reduce((s, t) => s + toNumber(t.amount), 0);
  const net = income - expense;

  const reportData = [
    { metric: 'إجمالي الإيرادات', value: formatCurrency(income), comparison: 'فعلي', percentage: '100%' },
    { metric: 'إجمالي المصروفات', value: formatCurrency(expense), comparison: 'فعلي', percentage: income ? `${Math.round((expense / income) * 100)}%` : '0%' },
    { metric: 'الربح الصافي', value: formatCurrency(net), comparison: 'فعلي', percentage: income ? `${Math.max(0, Math.round((net / income) * 100))}%` : '0%' },
    { metric: 'عدد النقلات', value: formatNumber(dashboardState.data.trips.length), comparison: dashboardState.api.currentMonth || '-', percentage: '100%' }
  ];

  tbody.innerHTML = reportData.map((item) => `
    <tr>
      <td>${item.metric}</td>
      <td>${item.value}</td>
      <td><span style="color:#27ae60;font-weight:600;">${item.comparison}</span></td>
      <td>${item.percentage}</td>
    </tr>
  `).join('');
}

function generateReport() {
  exportDemandsExcel();
}

function getActiveDemandRows_() {
  const activeTab = dashboardState.reports.activeDemandTab || 'halafaya';
  const allTrips = dashboardState.reports.rowsLoaded
    ? dashboardState.reports.rows
    : (dashboardState.data.trips || []);
  let rows = allTrips.filter((trip) => classifyDemandTab(trip) === activeTab);
  if (activeTab === 'factories' && dashboardState.reports.factory) {
    rows = rows.filter((trip) => getFactoryName_(trip) === dashboardState.reports.factory);
  }
  return rows;
}

function exportDemandsExcel() {
  const activeTab = dashboardState.reports.activeDemandTab || 'halafaya';
  const rows = getActiveDemandRows_();
  if (!rows.length) {
    uiAlert('لا توجد بيانات لتصدير هذا القسم حالياً.', 'قسم المطالبات');
    return;
  }

  if (!window.XLSX) {
    uiAlert('مكتبة Excel غير محملة حالياً. أعد تشغيل التطبيق.', 'قسم المطالبات');
    return;
  }

  const month = dashboardState.reports.month || dashboardState.api.currentMonth || 'month';
  const jsonRows = rows.map((trip, index) => ({
    '#': index + 1,
    'رقم الوصل': trip.docNumber,
    'السائق': trip.driverName,
    'رقم السيارة': trip.carNumber,
    'الوجهة': trip.station,
    'الكمية طن': toNumber(trip.quantity || 0),
    'المبلغ': toNumber(trip.price || 0),
    'المالك': trip.owner || trip.vehicleOwner || '',
    'التاريخ': trip.sendTime || '',
    'النوع': classifyDemandTab(trip) === 'factories' ? 'معمل' : 'اعتيادي'
  }));

  const ws = window.XLSX.utils.json_to_sheet(jsonRows);
  const wb = window.XLSX.utils.book_new();
  window.XLSX.utils.book_append_sheet(wb, ws, 'Demands');
  window.XLSX.writeFile(wb, `demands_${activeTab}_${month}.xlsx`);
  uiAlert('تم تصدير ملف Excel بنجاح.', 'قسم المطالبات');
}

function exportDemandsPdf() {
  const activeTab = dashboardState.reports.activeDemandTab || 'halafaya';
  const rows = getActiveDemandRows_();
  if (!rows.length) {
    uiAlert('لا توجد بيانات لتصدير هذا القسم حالياً.', 'قسم المطالبات');
    return;
  }

  const month = dashboardState.reports.month || dashboardState.api.currentMonth || '-';
  const htmlRows = rows.map((trip, index) => {
    const isFactory = classifyDemandTab(trip) === 'factories';
    return `<tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(trip.docNumber)}</td>
      <td>${escapeHtml(trip.driverName)}</td>
      <td>${escapeHtml(trip.carNumber)}</td>
      <td>${escapeHtml(trip.station || '-')}</td>
      <td>${formatNumber(trip.quantity || 0)}</td>
      <td>${formatCurrency(trip.price || 0)}</td>
      <td>${escapeHtml(trip.owner || trip.vehicleOwner || '-')}</td>
      <td>${formatDate(trip.sendTime || '')}</td>
      <td>${isFactory ? 'معمل' : 'اعتيادي'}</td>
    </tr>`;
  }).join('');

  const win = window.open('', '_blank');
  if (!win) {
    uiAlert('تعذر فتح نافذة الطباعة. تحقق من إعدادات النوافذ المنبثقة.', 'قسم المطالبات');
    return;
  }

  win.document.write(`
    <html lang="ar" dir="rtl">
    <head>
      <meta charset="utf-8" />
      <title>${demandTabLabel(activeTab)} - ${month}</title>
      <style>
        body{font-family:Tahoma,Arial,sans-serif;padding:16px;color:#222}
        h2{margin:0 0 8px}
        .meta{margin-bottom:14px;color:#666}
        table{width:100%;border-collapse:collapse}
        th,td{border:1px solid #ccc;padding:8px;text-align:right;font-size:12px}
        th{background:#f3f5f7}
      </style>
    </head>
    <body>
      <h2>${demandTabLabel(activeTab)}</h2>
      <div class="meta">الشهر: ${month}</div>
      <table>
        <thead>
          <tr><th>#</th><th>رقم الوصل</th><th>السائق</th><th>السيارة</th><th>الوجهة</th><th>الكمية</th><th>المبلغ</th><th>المالك</th><th>التاريخ</th><th>النوع</th></tr>
        </thead>
        <tbody>${htmlRows}</tbody>
      </table>
    </body>
    </html>
  `);
  win.document.close();
  win.focus();
  win.print();
}

async function loadActivationCodesDesktop() {
  const tbody = document.getElementById('activationCodesBodyDesktop');
  if (!tbody) return;

  tbody.innerHTML = '<tr><td colspan="11" style="text-align:center;padding:20px;">جاري تحميل بيانات التفعيل...</td></tr>';
  try {
    const result = await activationRequest_('list', {});
    dashboardState.activations.rows = Array.isArray(result && result.data) ? result.data : [];
    dashboardState.activations.mode = 'remote';
    renderActivationCodesDesktop();
  } catch (error) {
    dashboardState.activations.mode = 'remote';
    tbody.innerHTML = `<tr><td colspan="11" style="text-align:center;padding:20px;color:#c0392b;">فشل تحميل أكواد التفعيل: ${escapeHtml(String(error))}</td></tr>`;
    if (/Unknown action:|action is not allowed/i.test(String(error))) {
      const diag = await diagnoseActivationApiSupportDesktop();
      const lines = [
        'API التفعيل غير مدعوم في رابط النشر الحالي.',
        'لأمان النظام تم إيقاف أي إنشاء محلي للأكواد.',
        '',
        `رابط API الحالي: ${diag.baseUrl || '-'}`,
        `getAvailableMonths: ${diag.monthsOk ? 'OK' : 'غير مدعوم'}`,
        `companyActivationList: ${diag.listOk ? 'OK' : 'غير مدعوم'}`,
        `companyActivationCreate: ${diag.createOk ? 'OK' : 'غير مدعوم'}`,
        '',
        'الحل: اعمل Deploy جديد لنسخة Apps Script التي تحتوي أوامر companyActivation* ثم حدّث URL في manager-config.json.'
      ];
      await uiAlert(lines.join('\n'), 'بوابة الشركات والتفعيل');
    }
  }
}

async function activationRequest_(purpose, payload) {
  const chains = {
    list: ['companyActivationList', 'activationList', 'listCompanyActivationCodes'],
    create: ['companyActivationCreate', 'activationCreate', 'createCompanyActivationCode'],
    setEnabled: ['companyActivationSetEnabled', 'activationSetEnabled', 'setCompanyActivationEnabled'],
    unbind: ['companyActivationUnbind', 'activationUnbind', 'unbindCompanyActivationCode'],
    updateScope: ['companyActivationUpdateScope', 'activationUpdateScope', 'updateCompanyActivationScope']
  };

  const actions = chains[purpose] || [];
  let lastError = null;
  for (const action of actions) {
    try {
      return await fetchApiAction(action, payload || {});
    } catch (error) {
      lastError = error;
      if (!/Unknown action:/i.test(String(error))) {
        throw error;
      }
    }
  }

  throw lastError || new Error('Activation action failed');
}

async function diagnoseActivationApiSupportDesktop() {
  ensureApiReady();
  const baseUrl = dashboardState.api.baseUrl || '';
  const run = async (action) => {
    try {
      const res = await window.managerDesktop.apiRequest(action, {});
      return !(res && res.success === false && /Unknown action:|action is not allowed/i.test(String(res.message || '')));
    } catch {
      return false;
    }
  };

  const monthsOk = await run('getAvailableMonths');
  const listOk = await run('companyActivationList');
  const createOk = await run('companyActivationCreate');

  return {
    baseUrl,
    monthsOk,
    listOk,
    createOk
  };
}

function exportActivationCodesDesktop() {
  const rows = Array.isArray(dashboardState.activations.rows) ? dashboardState.activations.rows : [];
  if (!rows.length) {
    uiAlert('لا توجد أكواد للتصدير حالياً.', 'بوابة الشركات والتفعيل');
    return;
  }

  const lines = ['code,enabled,appKey,companyId,maxDevices,allowedPackage,bound,lastSeenAt,notes'];
  rows.forEach((item) => {
    lines.push([
      safeCsv(item.code),
      safeCsv(item.enabled ? '1' : '0'),
      safeCsv(item.appKey || 'company'),
      safeCsv(item.companyId || ''),
      safeCsv(item.maxDevices || 1),
      safeCsv(item.allowedPackage || item.packageName || ''),
      safeCsv(item.bound ? '1' : '0'),
      safeCsv(item.lastSeenAt || ''),
      safeCsv(item.notes || '')
    ].join(','));
  });

  const stamp = new Date().toISOString().slice(0, 10);
  downloadTextFile(`activation_codes_${stamp}.csv`, lines.join('\n'));
  uiAlert('تم تصدير الأكواد إلى CSV بنجاح.', 'بوابة الشركات والتفعيل');
}

function renderActivationCodesDesktop() {
  const tbody = document.getElementById('activationCodesBodyDesktop');
  if (!tbody) return;

  const rows = dashboardState.activations.rows || [];
  if (!rows.length) {
    tbody.innerHTML = '<tr><td colspan="11" style="text-align:center;padding:20px;">لا توجد أكواد تفعيل</td></tr>';
    return;
  }

  tbody.innerHTML = rows.map((item, index) => {
    const code = String(item.code || '').trim();
    const isEnabled = !!item.enabled;
    const encoded = encodeURIComponent(code);
    const statusLabel = isEnabled ? 'مفعّل' : 'موقوف';
    const lastSeen = item.lastSeenAt ? formatDate(item.lastSeenAt) : '-';

    return `
      <tr>
        <td>${index + 1}</td>
        <td><span style="font-weight:700; color:#1d3b5c;">${escapeHtml(code)}</span></td>
        <td>${statusLabel}</td>
        <td>${escapeHtml(String(item.appKey || 'company'))}</td>
        <td>${escapeHtml(String(item.companyId || '-'))}</td>
        <td>${escapeHtml(String(item.maxDevices || 1))}</td>
        <td>${escapeHtml(String(item.allowedPackage || item.packageName || '-'))}</td>
        <td>${item.bound ? 'نعم' : 'لا'}</td>
        <td>${escapeHtml(lastSeen)}</td>
        <td>${escapeHtml(String(item.notes || '-'))}</td>
        <td>
          <button class="btn btn-secondary" onclick="toggleActivationCodeDesktop(decodeURIComponent('${encoded}'), ${isEnabled ? 'true' : 'false'})">${isEnabled ? 'إيقاف' : 'تفعيل'}</button>
          <button class="btn btn-secondary" onclick="updateActivationScopeDesktop(decodeURIComponent('${encoded}'))">نطاق</button>
          <button class="btn btn-secondary" onclick="unbindActivationCodeDesktop(decodeURIComponent('${encoded}'))">فك ارتباط</button>
        </td>
      </tr>
    `;
  }).join('');
}

async function createActivationCodeDesktop() {
  const code = String(document.getElementById('activationCodeInputDesktop')?.value || '').trim().toUpperCase();
  const appKey = String(document.getElementById('activationAppKeyInputDesktop')?.value || 'company').trim().toLowerCase();
  const companyId = String(document.getElementById('activationCompanyIdInputDesktop')?.value || '').trim().toUpperCase();
  const maxDevicesRaw = Number(document.getElementById('activationMaxDevicesInputDesktop')?.value || 1);
  const maxDevices = Number.isFinite(maxDevicesRaw) ? Math.max(1, Math.min(20, Math.floor(maxDevicesRaw))) : 1;
  const allowedPackage = String(document.getElementById('activationAllowedPackageInputDesktop')?.value || '').trim();

  try {
    if (!companyId) {
      await uiAlert('معرف الشركة إلزامي لإنشاء كود تفعيل آمن.', 'تنبيه');
      return;
    }

    const result = await activationRequest_('create', {
      code,
      appKey,
      companyId,
      allowedPackage,
      maxDevices,
      actor: 'desktop_admin',
      enabled: true
    });

    if (!result || result.success === false) {
      await uiAlert(result && result.message ? result.message : 'فشل إنشاء كود التفعيل', 'خطأ');
      return;
    }

    document.getElementById('activationCodeInputDesktop').value = '';
    document.getElementById('activationCompanyIdInputDesktop').value = '';
    await uiAlert(`تم إنشاء كود التفعيل: ${result.code || ''}`, 'تم');
    await loadActivationCodesDesktop();
  } catch (error) {
    await uiAlert('فشل إنشاء كود التفعيل: ' + String(error), 'خطأ');
  }
}

async function toggleActivationCodeDesktop(code, currentlyEnabled) {
  if (!code) return;
  try {
    const result = await activationRequest_('setEnabled', {
      code,
      enabled: currentlyEnabled ? '0' : '1',
      actor: 'desktop_admin'
    });
    if (!result || result.success === false) {
      await uiAlert(result && result.message ? result.message : 'فشل تحديث حالة الكود', 'خطأ');
      return;
    }
    await loadActivationCodesDesktop();
  } catch (error) {
    await uiAlert('فشل تحديث حالة الكود: ' + String(error), 'خطأ');
  }
}

async function unbindActivationCodeDesktop(code) {
  if (!code) return;
  const confirmed = await uiConfirm('سيتم فك ارتباط الجهاز عن هذا الكود. هل تريد المتابعة؟', 'تأكيد');
  if (!confirmed) return;

  try {
    const result = await activationRequest_('unbind', { code, actor: 'desktop_admin' });
    if (!result || result.success === false) {
      await uiAlert(result && result.message ? result.message : 'فشل فك الارتباط', 'خطأ');
      return;
    }
    await loadActivationCodesDesktop();
  } catch (error) {
    await uiAlert('فشل فك الارتباط: ' + String(error), 'خطأ');
  }
}

async function updateActivationScopeDesktop(code) {
  if (!code) return;
  const item = (dashboardState.activations.rows || []).find((x) => String(x.code || '') === code);
  if (!item) {
    await uiAlert('تعذر العثور على الكود المطلوب', 'تنبيه');
    return;
  }

  const appKey = prompt('نوع التطبيق (company/gas/owner/driver)', String(item.appKey || 'company'));
  if (appKey == null) return;
  const companyId = prompt('معرف الشركة (إلزامي)', String(item.companyId || ''));
  if (companyId == null) return;
  const maxDevicesInput = prompt('الحد الأقصى للأجهزة (1..20)', String(item.maxDevices || 1));
  if (maxDevicesInput == null) return;
  const allowedPackage = prompt('الحزمة المسموحة (اختياري)', String(item.allowedPackage || item.packageName || ''));
  if (allowedPackage == null) return;

  const normalizedCompanyId = String(companyId || '').trim().toUpperCase();
  if (!normalizedCompanyId) {
    await uiAlert('معرف الشركة إلزامي.', 'تنبيه');
    return;
  }
  const maxDevicesRaw = Number(maxDevicesInput || 1);
  const maxDevices = Number.isFinite(maxDevicesRaw) ? Math.max(1, Math.min(20, Math.floor(maxDevicesRaw))) : 1;

  try {
    const result = await activationRequest_('updateScope', {
      code,
      appKey: String(appKey || '').trim().toLowerCase(),
      companyId: normalizedCompanyId,
      allowedPackage: String(allowedPackage || '').trim(),
      maxDevices,
      actor: 'desktop_admin'
    });

    if (!result || result.success === false) {
      await uiAlert(result && result.message ? result.message : 'فشل تحديث نطاق الصلاحية', 'خطأ');
      return;
    }
    await loadActivationCodesDesktop();
  } catch (error) {
    await uiAlert('فشل تحديث نطاق الصلاحية: ' + String(error), 'خطأ');
  }
}

function loadMaintenanceData() {
  const m = dashboardState.data.maintenance;
  document.getElementById('scheduledMaintenance').textContent = formatNumber(m.filter((x) => x.status === 'scheduled').length);
  document.getElementById('ongoingMaintenance').textContent = formatNumber(m.filter((x) => x.status === 'ongoing').length);
  document.getElementById('completedMaintenance').textContent = formatNumber(m.filter((x) => x.status === 'completed').length);
  document.getElementById('maintenanceCost').textContent = formatCurrency(m.reduce((s, x) => s + toNumber(x.cost), 0));
  populateMaintenanceTable();
}

function populateMaintenanceTable() {
  const tbody = document.getElementById('maintenanceBody');
  const m = dashboardState.data.maintenance;

  if (!m.length) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:20px;">لا توجد صيانة مسجلة</td></tr>';
    return;
  }

  tbody.innerHTML = m.map((item, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(item.vehicle || '-')}</td>
      <td>${escapeHtml(item.type || '-')}</td>
      <td>${formatDate(item.date)}</td>
      <td>${formatCurrency(item.cost)}</td>
      <td><span class="badge-${item.status === 'completed' ? 'success' : 'warning'}" style="padding:3px 8px;border-radius:4px;font-size:11px;">${item.status === 'completed' ? 'مكتملة' : 'قيد الإجراء'}</span></td>
      <td>${escapeHtml(item.notes || '-')}</td>
    </tr>
  `).join('');
}

function openMaintenanceModal() {
  createMaintenanceInsideApp();
}

function exportMaintenanceData() {
  const lines = ['vehicle,type,date,cost,status,notes'];
  dashboardState.data.maintenance.forEach((m) => {
    lines.push([safeCsv(m.vehicle), safeCsv(m.type), safeCsv(m.date), safeCsv(m.cost), safeCsv(m.status), safeCsv(m.notes)].join(','));
  });
  downloadTextFile('maintenance.csv', lines.join('\n'));
}

function initTracking() {
  const movingEl = document.getElementById('movingVehicles');
  if (!movingEl) return;

  const vehicles = dashboardState.data.vehicles;
  const trips = dashboardState.data.trips;

  document.getElementById('movingVehicles').textContent = formatNumber(vehicles.filter((v) => v.status === 'active').length);
  document.getElementById('stoppedVehicles').textContent = formatNumber(vehicles.filter((v) => v.status !== 'active').length);
  document.getElementById('totalDistance').textContent = formatNumber(trips.reduce((s, t) => s + toNumber(t.distance), 0));
  document.getElementById('avgTime').textContent = formatNumber(trips.length);
  loadActiveVehiclesList();
}

async function runSystemHealthCheckDesktop() {
  try {
    ensureApiReady();
    const month = await resolveWorkingMonth();
    const result = await fetchApiAction('systemHealthCheck', { month, templateSheetName: '60' });
    if (!result || result.success === false) {
      await uiAlert(result && result.message ? result.message : 'فشل فحص النظام', 'فحص النظام');
      return;
    }

    const summary = result.summary || {};
    const message = `نتيجة الفحص: ${summary.passed || 0}/${summary.total || 0} ${summary.healthy ? '(سليم)' : '(يحتاج مراجعة)'}`;
    await uiAlert(message, 'فحص النظام');
  } catch (error) {
    await uiAlert('تعذر فحص النظام: ' + String(error), 'فحص النظام');
  }
}

function loadActiveVehiclesList() {
  const list = document.getElementById('activeVehiclesList');
  const vehicles = dashboardState.data.vehicles;

  if (!vehicles.length) {
    list.innerHTML = '<div class="loading">لا توجد سيارات</div>';
    return;
  }

  list.innerHTML = vehicles.map((v) => `
    <div class="vehicle-item" onclick="selectVehicleOnMap(${v.id})">
      <div class="vehicle-item-name">🚛 ${escapeHtml(v.number)}</div>
      <div class="vehicle-item-status">
        <i class="fas fa-circle" style="color:${v.status === 'active' ? '#27ae60' : '#f39c12'};font-size:8px;"></i>
        ${v.status === 'active' ? 'نشطة' : 'صيانة'}
      </div>
    </div>
  `).join('');
}

function centerMap() {
  uiAlert('تم تحديث بيانات التتبع بناءً على النقلات الفعلية.', 'التتبع');
}

function selectVehicleOnMap(vehicleId) {
  const v = dashboardState.data.vehicles.find((x) => x.id === vehicleId);
  uiAlert(v ? `السيارة: ${v.number}\nالحالة: ${v.status === 'active' ? 'نشطة' : 'صيانة'}` : 'لا توجد بيانات', 'تفاصيل السيارة');
}

function saveSettings() {
  const settings = {
    companyName: document.getElementById('companyName').value,
    companyEmail: document.getElementById('companyEmail').value,
    companyPhone: document.getElementById('companyPhone').value,
    twoFactorAuth: document.getElementById('twoFactorAuth').checked,
    sheetsId: document.getElementById('sheetsId').value,
    notifyTrips: document.getElementById('notifyTrips').checked,
    notifyMaintenance: document.getElementById('notifyMaintenance').checked,
    notifyPayments: document.getElementById('notifyPayments').checked
  };

  localStorage.setItem('driverPortalSettings', JSON.stringify(settings));
  uiAlert('تم حفظ الإعدادات بنجاح', 'الإعدادات');
}

async function resetSettings() {
  if (!(await uiConfirm('هل أنت متأكد من إعادة تعيين الإعدادات؟', 'إعادة التعيين'))) return;
  localStorage.removeItem('driverPortalSettings');
  location.reload();
}

function changePassword() {
  const oldPassword = document.getElementById('oldPassword').value;
  const newPassword = document.getElementById('newPassword').value;
  if (!oldPassword || !newPassword) {
    uiAlert('يرجى ملء جميع الحقول', 'تنبيه');
    return;
  }

  uiAlert('تغيير كلمة المرور يتم من manager-config.json في بيئة سطح المكتب أو عبر لوحة الإدارة.', 'معلومة');
}

async function testSheetConnection() {
  try {
    ensureApiReady();
    await fetchApiAction('getAvailableMonths', {});
    await uiAlert('الاتصال مع Google Sheets يعمل بنجاح', 'اختبار الاتصال');
  } catch (error) {
    await uiAlert('فشل الاتصال: ' + String(error), 'اختبار الاتصال');
  }
}

async function createBackup() {
  try {
    ensureApiReady();
    const result = await fetchApiAction('createSystemBackup', {});
    const today = new Date().toLocaleDateString('ar-IQ');
    document.getElementById('lastBackup').textContent = 'آخر نسخة: ' + today;
    await uiAlert((result && result.message) || 'تم إنشاء النسخة الاحتياطية', 'نسخة احتياطية');
  } catch (error) {
    await uiAlert('تعذر إنشاء النسخة الاحتياطية: ' + String(error), 'نسخة احتياطية');
  }
}

function restoreBackup() {
  uiAlert('استعادة النسخة الاحتياطية تتم من لوحة الإدارة الخلفية لحماية البيانات.', 'نسخة احتياطية');
}

function initCharts() {
  const trips = dashboardState.data.trips;

  const ownersMap = new Map();
  trips.forEach((trip) => {
    const owner = trip.owner || 'غير محدد';
    ownersMap.set(owner, (ownersMap.get(owner) || 0) + 1);
  });

  const ownerLabels = Array.from(ownersMap.keys());
  const ownerValues = Array.from(ownersMap.values());

  const ownerCtx = document.getElementById('ownerChart')?.getContext('2d');
  if (ownerCtx && dashboardState.charts.owner) dashboardState.charts.owner.destroy();
  if (ownerCtx) {
    dashboardState.charts.owner = new Chart(ownerCtx, {
      type: 'doughnut',
      data: {
        labels: ownerLabels.length ? ownerLabels : ['لا توجد بيانات'],
        datasets: [{
          data: ownerValues.length ? ownerValues : [1],
          backgroundColor: ['#667eea', '#f093fb', '#4facfe', '#43e97b', '#f39c12'],
          borderColor: 'white',
          borderWidth: 2
        }]
      },
      options: { responsive: true, maintainAspectRatio: true }
    });
  }

  const revMap = new Map();
  trips.forEach((trip) => {
    const key = dayKey(trip.sendTime);
    if (!key) return;
    revMap.set(key, (revMap.get(key) || 0) + toNumber(trip.price));
  });

  const labels = Array.from(revMap.keys()).sort().slice(-7);
  const values = labels.map((k) => revMap.get(k));

  const revCtx = document.getElementById('revenueChart')?.getContext('2d');
  if (revCtx && dashboardState.charts.revenue) dashboardState.charts.revenue.destroy();
  if (revCtx) {
    dashboardState.charts.revenue = new Chart(revCtx, {
      type: 'line',
      data: {
        labels: labels.length ? labels : ['-'],
        datasets: [{
          label: 'الإيراد اليومي الفعلي',
          data: values.length ? values : [0],
          borderColor: '#667eea',
          backgroundColor: 'rgba(102, 126, 234, 0.1)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 4
        }]
      },
      options: { responsive: true, maintainAspectRatio: true }
    });
  }
}

function filterTable() {
  const q = (document.getElementById('tableSearch').value || '').toLowerCase();
  const rows = document.querySelectorAll('#recentTripsBody tr');
  rows.forEach((row) => {
    row.style.display = row.textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}

function applyDateFilter() {
  const from = document.getElementById('dateFrom').value;
  const to = document.getElementById('dateTo').value;

  if (!from || !to) {
    uiAlert('يرجى تحديد النطاق الزمني', 'تنبيه');
    return;
  }

  const fromDate = new Date(from);
  const toDate = new Date(to);
  toDate.setHours(23, 59, 59, 999);

  const filtered = dashboardState.cache.allTrips.filter((trip) => {
    const d = parseDateLoose(trip.sendTime);
    if (!d) return false;
    return d >= fromDate && d <= toDate;
  });

  dashboardState.data.trips = filtered;
  updateStats();
  updateRecentTrips();
  loadTripsGrid();
  populateAllTripsTable();
  initCharts();
}

function formatNumber(num, maxFractionDigits = 0) {
  return new Intl.NumberFormat('ar-IQ', {
    minimumFractionDigits: 0,
    maximumFractionDigits: Math.max(0, toNumber(maxFractionDigits))
  }).format(toNumber(num));
}

function formatCurrency(num) {
  return new Intl.NumberFormat('ar-IQ', {
    style: 'currency',
    currency: 'IQD',
    maximumFractionDigits: 0
  }).format(toNumber(num));
}

function formatDate(dateString) {
  if (!dateString) return '-';
  const d = parseDateLoose(dateString);
  if (!d) return String(dateString);
  return d.toLocaleDateString('ar-IQ', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text || '';
  return div.innerHTML;
}

function toNumber(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function normalizeText(value) {
  return String(value || '').trim().replace(/\s+/g, ' ').toLowerCase();
}

function parseDateLoose(value) {
  if (!value) return null;
  const txt = String(value).trim().replace(/\//g, '-');
  const d = new Date(txt);
  return Number.isNaN(d.getTime()) ? null : d;
}

function dayKey(value) {
  const d = parseDateLoose(value);
  if (!d) return '';
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function extractDay(value) {
  const d = parseDateLoose(value);
  return d ? d.getDate() : 0;
}

function inferMaintenanceStatus(item) {
  const note = String(item.notes || '').toLowerCase();
  if (note.includes('تم') || note.includes('مكتمل')) return 'completed';
  if (note.includes('قيد') || note.includes('ongoing')) return 'ongoing';
  return 'scheduled';
}

function pickLatestDate(a, b) {
  const da = parseDateLoose(a);
  const db = parseDateLoose(b);
  if (!da) return b || '';
  if (!db) return a || '';
  return db > da ? b : a;
}

function safeCsv(value) {
  const s = String(value ?? '');
  if (s.includes(',') || s.includes('"') || s.includes('\n')) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

function downloadTextFile(filename, text) {
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function detectAdminUrl(bootstrap) {
  const tabs = Array.isArray(bootstrap?.tabs) ? bootstrap.tabs : [];
  const adminTab = tabs.find((t) => t.id === 'admin_dashboard') || tabs[0];
  return adminTab?.url || '';
}

function detectApiBaseUrl(bootstrap) {
  const adminUrl = detectAdminUrl(bootstrap);
  try {
    const u = new URL(adminUrl);
    u.search = '';
    return u.toString();
  } catch {
    return '';
  }
}

function ensureApiReady() {
  if (!dashboardState.api.baseUrl) {
    dashboardState.api.baseUrl = detectApiBaseUrl(dashboardState.api.bootstrap);
  }
  if (!dashboardState.api.baseUrl) {
    throw new Error('لم يتم العثور على رابط API في manager-config.json');
  }
}

async function fetchApiAction(action, params) {
  const body = await window.managerDesktop.apiRequest(action, params || {});
  if (body && body.success === false) {
    throw new Error(normalizeApiErrorMessage(action, body.message || `API failure: ${action}`));
  }

  return body;
}

function normalizeApiErrorMessage(action, message) {
  const text = String(message || '').trim();
  if (/Unknown action:/i.test(text) && /companyActivation/i.test(text)) {
    return 'إصدار Apps Script المنشور قديم ولا يحتوي أوامر التفعيل الجديدة. يجب عمل Deploy جديد ثم إعادة فتح البرنامج.';
  }
  if (/Unknown action:/i.test(text) && /systemHealthCheck|createSystemBackup|getDashboardSummary|getAllReceiptsData/i.test(text)) {
    return 'الرابط الحالي يشير إلى نشر قديم من Apps Script. حدّث النشر ثم أعد تشغيل البرنامج.';
  }
  return text || `API failure: ${action}`;
}

function settledArray(result, key) {
  if (result.status !== 'fulfilled') return [];
  const v = result.value;
  if (Array.isArray(v)) return v;
  if (Array.isArray(v?.[key])) return v[key];
  return [];
}

function settledAny(result) {
  return result.status === 'fulfilled' ? result.value : null;
}

function normalizeDriversDirectory(payload) {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload.data)) return payload.data;
  if (Array.isArray(payload.drivers)) return payload.drivers;
  return [];
}

function findDriverDirectoryRow(name, rows) {
  const key = normalizeText(name);
  const row = (rows || []).find((r) => normalizeText(r.name || r.driverName || r.fullName) === key);
  if (!row) {
    return { phone: '', license: '', carNumber: '', salary: 0 };
  }

  return {
    phone: String(row.phone || row.mobile || ''),
    license: String(row.license || row.licenseNo || ''),
    carNumber: String(row.carNumber || row.vehicle || ''),
    salary: toNumber(row.salary || row.monthlySalary)
  };
}

async function resolveWorkingMonth() {
  const monthsPayload = await fetchApiAction('getAvailableMonths', {});
  const months = Array.isArray(monthsPayload?.data)
    ? monthsPayload.data
    : Array.isArray(monthsPayload)
      ? monthsPayload
      : [];

  dashboardState.api.months = months;

  const fromDate = document.getElementById('dateFrom')?.value;
  const fromMonth = monthKeyFromDate(fromDate);

  dashboardState.api.currentMonth = months.includes(fromMonth)
    ? fromMonth
    : (months[0] || monthKeyFromDate(new Date().toISOString()));

  return dashboardState.api.currentMonth;
}

function monthKeyFromDate(value) {
  const d = parseDateLoose(value) || new Date();
  return `${d.getFullYear()}_${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function prefillSettingsFromBootstrap(bootstrap) {
  if (!bootstrap) return;

  const saved = localStorage.getItem('driverPortalSettings');
  const settings = saved ? JSON.parse(saved) : {};

  const sheetTab = (bootstrap.tabs || []).find((t) => t.id === 'google_sheet');
  if (document.getElementById('companyName') && !document.getElementById('companyName').value) {
    document.getElementById('companyName').value = settings.companyName || 'شركة الناقلات النموذجية';
  }
  if (document.getElementById('companyEmail') && !document.getElementById('companyEmail').value) {
    document.getElementById('companyEmail').value = settings.companyEmail || 'info@tmiraq.iq';
  }
  if (document.getElementById('companyPhone') && !document.getElementById('companyPhone').value) {
    document.getElementById('companyPhone').value = settings.companyPhone || '+964 770 000 0000';
  }
  if (document.getElementById('sheetsId') && !document.getElementById('sheetsId').value) {
    document.getElementById('sheetsId').value = settings.sheetsId || (sheetTab?.url || dashboardState.api.baseUrl || '');
  }
}

async function openAdminPage() {
  const url = dashboardState.api.adminUrl || detectAdminUrl(dashboardState.api.bootstrap);
  if (!url) {
    await uiAlert('لا يوجد رابط لوحة إدارة معرف.', 'تعذر الفتح');
    return;
  }

  try {
    await window.managerDesktop.openExternal(url);
  } catch (error) {
    await uiAlert('تعذر فتح لوحة الإدارة: ' + String(error), 'تعذر الفتح');
  }
}

async function deleteTripRow(trip) {
  try {
    await fetchApiAction('deleteReceiptRow', {
      row: trip.row,
      month: dashboardState.api.currentMonth,
      sheetName: trip.sheetName
    });
    await uiAlert('تم حذف النقلة بنجاح', 'عملية ناجحة');
    await loadAllData();
  } catch (error) {
    await uiAlert('تعذر حذف النقلة: ' + String(error), 'فشل العملية');
  }
}

async function createTripInsideApp() {
  try {
    const today = new Date().toISOString().slice(0, 10);
    const values = await uiForm({
      title: 'إضافة حركة جديدة',
      fields: [
        { name: 'action', label: 'نوع الحركة', type: 'select', value: 'trip', options: [
          { value: 'trip', label: 'نقلة اعتيادية' },
          { value: 'factory', label: 'نقلة معمل' }
        ] },
        { name: 'docNumber', label: 'رقم الوصل', required: true },
        { name: 'driverName', label: 'اسم السائق', required: true },
        { name: 'carNumber', label: 'رقم السيارة', required: true },
        { name: 'quantity', label: 'الكمية (طن)', value: '0' },
        { name: 'loadDate', label: 'تاريخ التحميل', type: 'date', value: today, required: true },
        { name: 'unloadDate', label: 'تاريخ التفريغ', type: 'date', value: today, required: true },
        { name: 'owner', label: 'المالك', value: '' },
        { name: 'vehicleOwner', label: 'مالك السيارة أو المالك (لنقلة المعمل)', value: '' },
        { name: 'station', label: 'المحطة/الوجهة (للنقلة الاعتيادية)', value: '' },
        { name: 'factory', label: 'اسم المعمل (لنقلة المعمل)', value: '' },
        { name: 'liters', label: 'لترات الكاز', value: '0' },
        { name: 'bojer', label: 'رقم البوجر', value: '' },
        { name: 'distance', label: 'المسافة كم', value: '0' },
        { name: 'kroa', label: 'الكروة', value: '0' },
        { name: 'notes', label: 'ملاحظات', value: '' }
      ],
      submitText: 'حفظ الحركة'
    });
    if (!values) return;

    const action = String(values.action || 'trip').trim().toLowerCase() === 'factory' ? 'factory' : 'trip';

    const payload = {
      docNumber: values.docNumber,
      driverName: values.driverName,
      carNumber: values.carNumber,
      quantity: values.quantity || '0',
      loadDate: values.loadDate,
      unloadDate: values.unloadDate,
      notes: values.notes || '',
      month: dashboardState.api.currentMonth,
      tripMonth: dashboardState.api.currentMonth
    };

    if (action === 'factory') {
      payload.factory = values.factory || '';
      payload.owner = values.owner || '';
      payload.vehicleOwner = values.vehicleOwner || '';
      payload.kroa = values.kroa || '0';
    } else {
      payload.owner = values.owner || '';
      payload.station = values.station || '';
      payload.liters = values.liters || '0';
      payload.bojer = values.bojer || '';
      payload.distance = values.distance || '0';
      payload.kroa = values.kroa || '0';
    }

    const result = await fetchApiAction(action, payload);
    await uiAlert((result && result.message) || 'تمت العملية بنجاح', 'حفظ الحركة');
    await loadAllData();
  } catch (error) {
    await uiAlert('تعذر حفظ الحركة: ' + String(error), 'فشل الحفظ');
  }
}

async function createMaintenanceInsideApp() {
  try {
    const values = await uiForm({
      title: 'إضافة طلب صيانة',
      fields: [
        { name: 'driverName', label: 'اسم السائق', required: true },
        { name: 'carNumber', label: 'رقم السيارة', required: true },
        { name: 'problem', label: 'وصف المشكلة', required: true },
        { name: 'type', label: 'نوع الصيانة', value: 'صيانة دورية' },
        { name: 'cost', label: 'التكلفة التقديرية', value: '0' },
        { name: 'location', label: 'الموقع', value: '' },
        { name: 'notes', label: 'ملاحظات', value: '' }
      ],
      submitText: 'حفظ الطلب'
    });
    if (!values) return;

    const payload = {
      driverName: values.driverName,
      carNumber: values.carNumber,
      vehicle: values.carNumber,
      problem: values.problem,
      type: values.type || 'صيانة دورية',
      cost: values.cost || '0',
      location: values.location || '',
      notes: values.notes || '',
      requestDate: new Date().toISOString().slice(0, 10)
    };

    const result = await fetchApiAction('saveMaintenance', payload);
    await uiAlert((result && result.message) || 'تم إنشاء طلب الصيانة بنجاح', 'الصيانة');
    await loadAllData();
  } catch (error) {
    await uiAlert('تعذر حفظ طلب الصيانة: ' + String(error), 'فشل الحفظ');
  }
}

async function editTrip(id) {
  const trip = dashboardState.data.trips.find((item) => item.id === id);
  if (!trip) return;

  const isFactory = isFactoryTrip_(trip);
  const values = await uiForm({
    title: 'تعديل وصلة',
    fields: [
      { name: 'operation', label: 'الإجراء', type: 'select', value: 'update', options: [
        { value: 'update', label: 'تعديل السجل' },
        { value: 'delete', label: 'حذف السجل' }
      ] },
      { name: 'action', label: 'نوع الحركة', type: 'select', value: isFactory ? 'factory' : 'trip', options: [
        { value: 'trip', label: 'نقلة اعتيادية' },
        { value: 'factory', label: 'نقلة معمل' }
      ] },
      { name: 'docNumber', label: 'رقم الوصل', required: true, value: trip.docNumber || '' },
      { name: 'driverName', label: 'اسم السائق', required: true, value: trip.driverName || '' },
      { name: 'carNumber', label: 'رقم السيارة', required: true, value: trip.carNumber || '' },
      { name: 'quantity', label: 'الكمية (طن)', value: String(toNumber(trip.quantity || 0)) },
      { name: 'owner', label: 'المالك', value: trip.owner || '' },
      { name: 'vehicleOwner', label: 'مالك السيارة أو المالك (لنقلة المعمل)', value: trip.vehicleOwner || '' },
      { name: 'station', label: 'المحطة/الوجهة (للنقلة الاعتيادية)', value: isFactory ? '' : (trip.station || '') },
      { name: 'factory', label: 'اسم المعمل (لنقلة المعمل)', value: isFactory ? (trip.station || '') : '' },
      { name: 'notes', label: 'ملاحظات', value: trip.notes || '' }
    ],
    submitText: 'تنفيذ'
  });

  if (!values) return;

  if (!trip.row || !trip.sheetName) {
    await uiAlert('تعذر تعديل/حذف السجل: بيانات الصف أو اسم الورقة غير متوفرة.', 'فشل العملية');
    return;
  }

  if (String(values.operation || 'update') === 'delete') {
    await deleteTripRow(trip);
    return;
  }

  const monthMatch = String(trip.sheetName || '').match(/(\d{4}_\d{2})$/i);
  const targetMonth = monthMatch ? monthMatch[1] : (dashboardState.api.currentMonth || '');
  const action = String(values.action || (isFactory ? 'factory' : 'trip')).toLowerCase() === 'factory' ? 'factory' : 'trip';

  try {
    await fetchApiAction('deleteReceiptRow', {
      row: trip.row,
      month: targetMonth,
      sheetName: trip.sheetName
    });

    const payload = {
      docNumber: values.docNumber,
      driverName: values.driverName,
      carNumber: values.carNumber,
      quantity: values.quantity || '0',
      loadDate: new Date().toISOString().slice(0, 10),
      unloadDate: new Date().toISOString().slice(0, 10),
      notes: values.notes || '',
      month: targetMonth,
      tripMonth: targetMonth
    };

    if (action === 'factory') {
      payload.factory = values.factory || values.station || '';
      payload.owner = values.owner || '';
      payload.vehicleOwner = values.vehicleOwner || '';
      payload.kroa = '0';
    } else {
      payload.owner = values.owner || '';
      payload.station = values.station || values.factory || '';
      payload.liters = '0';
      payload.bojer = '';
      payload.distance = '0';
      payload.kroa = '0';
    }

    await fetchApiAction(action, payload);
    await uiAlert('تم تعديل السجل بنجاح.', 'عملية ناجحة');
    await loadAllData();
  } catch (error) {
    await uiAlert('تعذر تعديل السجل: ' + String(error), 'فشل العملية');
  }
}

function editVehicle(id) {
  const vehicle = dashboardState.data.vehicles.find((item) => item.id === id);
  if (!vehicle) return;
  uiAlert(`السيارة: ${vehicle.number}\nالحالة: ${vehicle.status === 'active' ? 'نشطة' : 'صيانة'}\nالنقلات: ${formatNumber(vehicle.trips)}\nالإيراد: ${formatCurrency(vehicle.revenue)}`, 'تفاصيل السيارة');
}

function editDriver(id) {
  const driver = dashboardState.data.drivers.find((item) => item.id === id);
  if (!driver) return;
  uiAlert(`السائق: ${driver.name}\nالهاتف: ${driver.phone || '-'}\nالسيارة: ${driver.vehicle || '-'}\nالنقلات: ${formatNumber(driver.trips)}\nالراتب: ${formatCurrency(driver.salary)}`, 'تفاصيل السائق');
}

function editTransaction(id) {
  uiAlert(`عرض المعاملة ${id} من البيانات الفعلية`, 'المعاملات');
}

function editPayment(id) {
  uiAlert(`عرض الدفعة ${id} من البيانات الفعلية`, 'الدفعات');
}

function exportTableData() {
  const lines = ['docNumber,driverName,carNumber,owner,station,quantity,price,date'];
  dashboardState.data.trips.forEach((trip) => {
    lines.push([
      safeCsv(trip.docNumber),
      safeCsv(trip.driverName),
      safeCsv(trip.carNumber),
      safeCsv(trip.owner),
      safeCsv(trip.station),
      safeCsv(trip.quantity),
      safeCsv(trip.price),
      safeCsv(trip.sendTime)
    ].join(','));
  });
  downloadTextFile(`trips_${dashboardState.api.currentMonth || 'export'}.csv`, lines.join('\n'));
}

function uiAlert(message, title) {
  return openDialog({
    mode: 'alert',
    title: title || 'تنبيه',
    message,
    okText: 'حسناً'
  });
}

function uiConfirm(message, title) {
  return openDialog({
    mode: 'confirm',
    title: title || 'تأكيد',
    message,
    okText: 'تأكيد',
    cancelText: 'إلغاء'
  });
}

function uiForm(config) {
  return openDialog({
    mode: 'form',
    title: config.title || 'إدخال بيانات',
    fields: Array.isArray(config.fields) ? config.fields : [],
    okText: config.submitText || 'حفظ',
    cancelText: config.cancelText || 'إلغاء'
  });
}

function ensureDialogRoot() {
  if (dialogState.root) return dialogState.root;

  const root = document.createElement('div');
  root.className = 'ui-dialog-overlay hidden';
  root.innerHTML = `
    <div class="ui-dialog" role="dialog" aria-modal="true" aria-live="polite">
      <div class="ui-dialog-header">
        <h3 class="ui-dialog-title"></h3>
      </div>
      <div class="ui-dialog-body"></div>
      <div class="ui-dialog-actions">
        <button type="button" class="ui-btn ui-btn-cancel">إلغاء</button>
        <button type="button" class="ui-btn ui-btn-ok">حسناً</button>
      </div>
    </div>
  `;

  root.addEventListener('click', (event) => {
    if (event.target === root) closeDialog(null);
  });

  document.body.appendChild(root);
  dialogState.root = root;
  return root;
}

function openDialog(options) {
  const root = ensureDialogRoot();
  const dialog = root.querySelector('.ui-dialog');
  const titleEl = root.querySelector('.ui-dialog-title');
  const bodyEl = root.querySelector('.ui-dialog-body');
  const okBtn = root.querySelector('.ui-btn-ok');
  const cancelBtn = root.querySelector('.ui-btn-cancel');

  titleEl.textContent = options.title || 'تنبيه';
  bodyEl.innerHTML = '';

  const mode = options.mode || 'alert';
  if (mode === 'form') {
    const form = document.createElement('form');
    form.className = 'ui-dialog-form';

    (options.fields || []).forEach((field) => {
      const row = document.createElement('label');
      row.className = 'ui-dialog-field';

      const title = document.createElement('span');
      title.className = 'ui-dialog-field-label';
      title.textContent = field.label || field.name;
      row.appendChild(title);

      let input;
      if (field.type === 'select') {
        input = document.createElement('select');
        (field.options || []).forEach((opt) => {
          const option = document.createElement('option');
          option.value = String(opt.value || '');
          option.textContent = String(opt.label || opt.value || '');
          if (String(field.value || '') === option.value) {
            option.selected = true;
          }
          input.appendChild(option);
        });
      } else {
        input = document.createElement('input');
        input.type = field.type || 'text';
        input.value = field.value == null ? '' : String(field.value);
      }

      input.name = field.name;
      if (field.required) input.required = true;
      input.className = 'ui-dialog-input';
      row.appendChild(input);
      form.appendChild(row);
    });

    bodyEl.appendChild(form);
  } else {
    const message = document.createElement('div');
    message.className = 'ui-dialog-message';
    message.textContent = options.message || '';
    bodyEl.appendChild(message);
  }

  okBtn.textContent = options.okText || 'حسناً';
  cancelBtn.textContent = options.cancelText || 'إلغاء';
  cancelBtn.style.display = mode === 'alert' ? 'none' : '';

  const onKey = (event) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      closeDialog(null);
    }
  };

  okBtn.onclick = () => {
    if (mode === 'form') {
      const values = {};
      const form = bodyEl.querySelector('form');
      const controls = form ? Array.from(form.querySelectorAll('input,select,textarea')) : [];
      for (const control of controls) {
        const key = control.name;
        const value = String(control.value || '').trim();
        if (control.required && !value) {
          control.focus();
          return;
        }
        values[key] = value;
      }
      closeDialog(values);
      return;
    }

    closeDialog(mode === 'confirm' ? true : true);
  };

  cancelBtn.onclick = () => closeDialog(mode === 'confirm' ? false : null);

  dialogState.resolver = (result) => {
    root.classList.add('hidden');
    document.removeEventListener('keydown', onKey, true);
    dialogState.resolver = null;
    return result;
  };

  root.classList.remove('hidden');
  document.addEventListener('keydown', onKey, true);
  setTimeout(() => {
    const firstInput = bodyEl.querySelector('input,select,textarea');
    if (firstInput) {
      firstInput.focus();
    } else {
      okBtn.focus();
    }
  }, 0);

  return new Promise((resolve) => {
    const previous = dialogState.resolver;
    dialogState.resolver = (result) => {
      if (previous) previous(null);
      const value = result;
      root.classList.add('hidden');
      document.removeEventListener('keydown', onKey, true);
      dialogState.resolver = null;
      resolve(value);
    };
  });
}

function closeDialog(result) {
  if (typeof dialogState.resolver === 'function') {
    dialogState.resolver(result);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initApp();
});
