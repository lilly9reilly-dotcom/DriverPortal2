// =============== الحالة العامة ===============
const dashboardState = {
  isLoggedIn: false,
  currentPage: 'dashboard',
  data: {
    trips: [],
    vehicles: [],
    drivers: [],
    stats: {}
  },
  charts: {}
};

// =============== دورة تهيئة التطبيق ===============
async function initApp() {
  try {
    const bootstrap = await window.managerDesktop.getBootstrap();
    console.log('Bootstrap loaded:', bootstrap);
  } catch (error) {
    console.error('Failed to bootstrap:', error);
  }
}

// =============== التحكم في الدخول ===============
function togglePassword() {
  const input = document.getElementById('password');
  const button = document.querySelector('.toggle-password');

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

  try {
    const result = await window.managerDesktop.login(password);

    if (result.success) {
      loginSuccess();
    } else {
      errorEl.textContent = result.message || 'فشل تسجيل الدخول';
    }
  } catch (error) {
    errorEl.textContent = 'خطأ في الاتصال: ' + String(error);
  }
});

function loginSuccess() {
  dashboardState.isLoggedIn = true;

  document.getElementById('loginContainer').classList.add('hidden');
  document.getElementById('appContainer').classList.remove('hidden');

  initDashboard();
  loadDashboardData();
}

function logout() {
  if (confirm('هل تريد تسجيل الخروج؟')) {
    dashboardState.isLoggedIn = false;
    document.getElementById('appContainer').classList.add('hidden');
    document.getElementById('loginContainer').classList.remove('hidden');
    document.getElementById('loginForm').reset();
    document.getElementById('password').type = 'password';
  }
}

// =============== التحكم في الواجهة ===============
function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('active');
}

function openTab(tabName) {
  // إخفاء جميع الأقسام
  const sections = document.querySelectorAll('.content-section');
  sections.forEach((section) => section.classList.remove('active'));

  // إظهار القسم المختار
  const selectedSection = document.getElementById(`${tabName}-section`);
  if (selectedSection) {
    selectedSection.classList.add('active');
  }

  // تحديث الزر النشط
  const navItems = document.querySelectorAll('.nav-item');
  navItems.forEach((item) => item.classList.remove('active'));

  const activeItem = document.querySelector(`[data-tab="${tabName}"]`);
  if (activeItem) {
    activeItem.classList.add('active');
  }

  // تحديث عنوان الصفحة
  const titles = {
    dashboard: 'لوحة التحكم الرئيسية',
    trips: 'إدارة النقلات المتقدمة',
    vehicles: 'إدارة السيارات والأسطول',
    drivers: 'إدارة السائقين والرواتب',
    accounting: 'الحسابات والمالية المتقدمة',
    reports: 'التقارير والإحصائيات المتقدمة',
    payments: 'إدارة المدفوعات والتحويلات',
    maintenance: 'إدارة الصيانة الدورية',
    tracking: 'تتبع الأسطول المباشر',
    settings: 'الإعدادات والتكوين المتقدم'
  };

  document.getElementById('currentPage').textContent = titles[tabName] || 'الصفحة';
  dashboardState.currentPage = tabName;

  // إغلاق الـ sidebar على الجوال
  if (window.innerWidth <= 768) {
    document.getElementById('sidebar').classList.remove('active');
  }
}

function initDashboard() {
  // تهيئة المتغيرات
  openTab('dashboard');

  // تعيين التاريخ الافتراضي
  const today = new Date();
  const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);

  document.getElementById('dateTo').valueAsDate = today;
  document.getElementById('dateFrom').valueAsDate = startOfMonth;

  // إضافة مستمعي الأحداث للبحث
  document.getElementById('tableSearch')?.addEventListener('input', filterTable);
}

// =============== تحميل البيانات ===============
async function loadDashboardData() {
  try {
    // تحميل البيانات من الـ IPC
    const data = await window.managerDesktop.getDashboardData?.();

    if (data) {
      updateStats(data);
      updateRecentTrips(data.trips || []);
      initCharts(data);
    } else {
      // بيانات وهمية للاختبار
      updateStatsDummy();
      updateRecentTripsDummy();
      initChartsDummy();
    }
  } catch (error) {
    console.error('Error loading data:', error);
    // بيانات وهمية كـ fallback
    updateStatsDummy();
    updateRecentTripsDummy();
    initChartsDummy();
  }
}

function updateStats(data) {
  document.getElementById('totalTrips').textContent = formatNumber(data.totalTrips || 0);
  document.getElementById('totalRevenue').textContent = formatCurrency(data.totalRevenue || 0);
  document.getElementById('activeVehicles').textContent = formatNumber(data.activeVehicles || 0);
  document.getElementById('totalDrivers').textContent = formatNumber(data.totalDrivers || 0);
}

function updateStatsDummy() {
  document.getElementById('totalTrips').textContent = '298';
  document.getElementById('totalRevenue').textContent = '127,133,065,090';
  document.getElementById('activeVehicles').textContent = '15';
  document.getElementById('totalDrivers').textContent = '42';
}

function updateRecentTrips(trips) {
  const tbody = document.getElementById('recentTripsBody');

  if (!trips || trips.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 20px; color: #999">لا توجد بيانات</td></tr>';
    return;
  }

  tbody.innerHTML = trips
    .slice(0, 10)
    .map(
      (trip) => `
    <tr>
      <td>${escapeHtml(trip.docNumber || '')}</td>
      <td>${escapeHtml(trip.driverName || '')}</td>
      <td>${escapeHtml(trip.carNumber || '')}</td>
      <td>${escapeHtml(trip.owner || '')}</td>
      <td>${escapeHtml(trip.station || '')}</td>
      <td>${formatNumber(trip.quantity || 0)} طن</td>
      <td>${formatCurrency(trip.price || 0)}</td>
      <td>${formatDate(trip.sendTime || '')}</td>
    </tr>
  `
    )
    .join('');

  dashboardState.data.trips = trips;
}

function updateRecentTripsDummy() {
  const tbody = document.getElementById('recentTripsBody');
  const dummyTrips = [
    { docNumber: '001', driverName: 'أحمد محمد', carNumber: 'بغ 123', owner: 'الشركة الأولى', station: 'حلفاية', quantity: 15, price: 630000, sendTime: '2026-06-19 10:30' },
    { docNumber: '002', driverName: 'علي حسين', carNumber: 'بغ 456', owner: 'الشركة الثانية', station: 'التاجي', quantity: 20, price: 840000, sendTime: '2026-06-19 09:15' },
    { docNumber: '003', driverName: 'محمد خالد', carNumber: 'بغ 789', owner: 'الشركة الأولى', station: 'الرصافة', quantity: 18, price: 756000, sendTime: '2026-06-19 08:00' }
  ];

  updateRecentTrips(dummyTrips);
}

// =============== الرسوم البيانية ===============
function initCharts(data) {
  // مخطط توزيع النقلات
  const ownerChart = document.getElementById('ownerChart')?.getContext('2d');
  if (ownerChart && dashboardState.charts.owner) {
    dashboardState.charts.owner.destroy();
  }

  if (ownerChart) {
    dashboardState.charts.owner = new Chart(ownerChart, {
      type: 'doughnut',
      data: {
        labels: ['الشركة الأولى', 'الشركة الثانية', 'الشركة الثالثة'],
        datasets: [
          {
            data: [120, 100, 78],
            backgroundColor: ['#667eea', '#f093fb', '#4facfe'],
            borderColor: 'white',
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              font: { family: "'Segoe UI', Arial", size: 12 },
              padding: 15
            }
          }
        }
      }
    });
  }

  // مخطط الإيرادات اليومية
  const revenueChart = document.getElementById('revenueChart')?.getContext('2d');
  if (revenueChart && dashboardState.charts.revenue) {
    dashboardState.charts.revenue.destroy();
  }

  if (revenueChart) {
    dashboardState.charts.revenue = new Chart(revenueChart, {
      type: 'line',
      data: {
        labels: ['السبت', 'الأحد', 'الاثنين', 'الثلاثاء', 'الأربعاء', 'الخميس', 'الجمعة'],
        datasets: [
          {
            label: 'الإيراد اليومي',
            data: [12000000, 19000000, 15000000, 25000000, 22000000, 30000000, 28000000],
            borderColor: '#667eea',
            backgroundColor: 'rgba(102, 126, 234, 0.1)',
            borderWidth: 2,
            fill: true,
            tension: 0.4,
            pointRadius: 5,
            pointBackgroundColor: '#667eea',
            pointBorderColor: 'white',
            pointBorderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            labels: {
              font: { family: "'Segoe UI', Arial", size: 12 }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function (value) {
                return formatCurrency(value / 1000000) + ' م';
              }
            }
          }
        }
      }
    });
  }
}

function initChartsDummy() {
  initCharts({});
}

// =============== البحث والتصفية ===============
function filterTable() {
  const searchValue = document.getElementById('tableSearch').value.toLowerCase();
  const tableRows = document.querySelectorAll('#recentTripsBody tr');

  tableRows.forEach((row) => {
    const text = row.textContent.toLowerCase();
    row.style.display = text.includes(searchValue) ? '' : 'none';
  });
}

// =============== التصدير ===============
function exportTableData() {
  const table = document.querySelector('.data-table');
  let csv = '';

  // رؤوس الأعمدة
  const headers = [];
  table.querySelectorAll('th').forEach((th) => {
    headers.push(th.textContent);
  });
  csv += headers.join(',') + '\n';

  // بيانات الصفوف
  table.querySelectorAll('tbody tr').forEach((tr) => {
    if (tr.style.display !== 'none') {
      const row = [];
      tr.querySelectorAll('td').forEach((td) => {
        row.push(`"${td.textContent}"`);
      });
      csv += row.join(',') + '\n';
    }
  });

  // تنزيل الملف
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);

  link.setAttribute('href', url);
  link.setAttribute('download', `trips-export-${new Date().toISOString().split('T')[0]}.csv`);
  link.click();
}

function applyDateFilter() {
  const from = document.getElementById('dateFrom').value;
  const to = document.getElementById('dateTo').value;

  if (!from || !to) {
    alert('يرجى تحديد النطاق الزمني');
    return;
  }

  console.log('Filter applied:', from, to);
  loadDashboardData();
}

// =============== دوال مساعدة ===============
function formatNumber(num) {
  return new Intl.NumberFormat('ar-IQ').format(num || 0);
}

function formatCurrency(num) {
  return new Intl.NumberFormat('ar-IQ', { style: 'currency', currency: 'IQD' }).format(num || 0);
}

function formatDate(dateString) {
  if (!dateString) return '-';

  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('ar-IQ', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  } catch {
    return dateString;
  }
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// =============== التهيئة ===============
document.addEventListener('DOMContentLoaded', () => {
  initApp();
});
