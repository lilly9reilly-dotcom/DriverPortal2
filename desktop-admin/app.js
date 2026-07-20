const state = {
  tabs: [],
  activeId: null,
  appTitle: 'لوحة مدير الحسابات'
};

const elements = {
  loginShell: document.getElementById('loginShell'),
  appShell: document.getElementById('appShell'),
  loginBtn: document.getElementById('loginBtn'),
  passwordInput: document.getElementById('passwordInput'),
  loginError: document.getElementById('loginError'),
  passwordHint: document.getElementById('passwordHint'),
  appTitle: document.getElementById('appTitle'),
  loginTitle: document.getElementById('loginTitle'),
  tabs: document.getElementById('tabs'),
  viewer: document.getElementById('viewer'),
  viewerLoader: document.getElementById('viewerLoader'),
  activeTabTitle: document.getElementById('activeTabTitle'),
  logoutBtn: document.getElementById('logoutBtn')
};

async function boot() {
  const bootstrap = await window.managerDesktop.getBootstrap();
  state.tabs = Array.isArray(bootstrap.tabs) ? bootstrap.tabs : [];
  state.appTitle = bootstrap.appTitle || state.appTitle;

  elements.appTitle.textContent = state.appTitle;
  elements.loginTitle.textContent = state.appTitle;
  elements.passwordHint.textContent = bootstrap.passwordHint || '';

  bindEvents();
  renderTabs();
}

function bindEvents() {
  elements.loginBtn.addEventListener('click', onLogin);
  elements.passwordInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      onLogin();
    }
  });

  elements.viewer.addEventListener('load', () => {
    elements.viewerLoader.classList.add('hidden');
  });

  elements.logoutBtn.addEventListener('click', () => {
    elements.appShell.classList.add('hidden');
    elements.loginShell.classList.remove('hidden');
    elements.passwordInput.value = '';
    elements.loginError.textContent = '';
    elements.passwordInput.focus();
  });
}

async function onLogin() {
  const password = elements.passwordInput.value || '';
  elements.loginError.textContent = '';

  const result = await window.managerDesktop.login(password);
  if (!result.success) {
    elements.loginError.textContent = result.message || 'فشل تسجيل الدخول';
    return;
  }

  elements.loginShell.classList.add('hidden');
  elements.appShell.classList.remove('hidden');

  if (state.tabs.length > 0) {
    activateTab(state.tabs[0].id);
  }
}

function renderTabs() {
  elements.tabs.innerHTML = '';

  if (!state.tabs.length) {
    const empty = document.createElement('div');
    empty.textContent = 'لا توجد تبويبات معرفة';
    empty.style.padding = '8px';
    elements.tabs.appendChild(empty);
    return;
  }

  state.tabs.forEach((tab) => {
    const btn = document.createElement('button');
    btn.className = 'tab-btn';
    btn.textContent = tab.label;
    btn.dataset.tabId = tab.id;
    btn.addEventListener('click', () => activateTab(tab.id));
    elements.tabs.appendChild(btn);
  });
}

function activateTab(tabId) {
  const tab = state.tabs.find((t) => t.id === tabId);
  if (!tab) {
    return;
  }

  state.activeId = tab.id;
  elements.activeTabTitle.textContent = tab.label;

  const tabButtons = elements.tabs.querySelectorAll('.tab-btn');
  tabButtons.forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tabId === tab.id);
  });

  const openMode = String(tab.openMode || 'embedded').toLowerCase();
  if (openMode === 'external') {
    elements.viewerLoader.classList.remove('hidden');
    elements.viewer.src = 'about:blank';
    elements.viewerLoader.textContent = 'هذا التبويب يفتح في المتصفح الخارجي لضمان عمله بدون قيود تضمين...';

    window.managerDesktop.openExternal(tab.url).then(() => {
      elements.viewerLoader.textContent = 'تم فتح التبويب في المتصفح الخارجي. يمكنك العودة للتطبيق ومتابعة العمل.';
    }).catch((error) => {
      elements.viewerLoader.textContent = 'تعذر فتح الرابط الخارجي: ' + String(error);
    });

    return;
  }

  elements.viewerLoader.textContent = 'جاري تحميل الصفحة...';
  elements.viewerLoader.classList.remove('hidden');
  elements.viewer.src = tab.url;
}

boot().catch((error) => {
  elements.loginError.textContent = 'تعذر تهيئة التطبيق: ' + String(error);
});
