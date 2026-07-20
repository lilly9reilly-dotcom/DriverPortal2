const { app, BrowserWindow, dialog, ipcMain, shell } = require('electron');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const DEFAULT_ADMIN_URL = process.env.DRIVER_PORTAL_ADMIN_URL ||
  'https://script.google.com/macros/s/AKfycbwCreVvebaAN7C4W2OZu6ura7cza42P2lIssNt4sVBv1raDqZkQYY-ZZyNNcl9_iynhAw/exec?page=admin';
const DEFAULT_DEMAND_TEMPLATE_URL = process.env.DRIVER_PORTAL_DEMAND_TEMPLATE_URL ||
  'https://script.google.com/macros/s/AKfycbw-3wKRuKImCvvB4ip3PGokDP18yJz6HDW2QylDmvQGxAbyn8Wq-FIlHQ9ms-i7wlCEQA/exec?page=demand_template';
const CONFIG_PATH = path.join(__dirname, 'manager-config.json');
const API_REQUEST_TIMEOUT_MS = 15000;
const API_REQUEST_RETRIES = 1;
const ALLOWED_API_ACTIONS = new Set([
  'getAvailableMonths',
  'getAllReceiptsData',
  'getDashboardSummary',
  'getMaintenanceData',
  'drivers',
  'systemHealthCheck',
  'createSystemBackup',
  'companyActivationList',
  'activationList',
  'listCompanyActivationCodes',
  'companyActivationCreate',
  'activationCreate',
  'createCompanyActivationCode',
  'companyActivationSetEnabled',
  'activationSetEnabled',
  'setCompanyActivationEnabled',
  'companyActivationUnbind',
  'activationUnbind',
  'unbindCompanyActivationCode',
  'companyActivationUpdateScope',
  'activationUpdateScope',
  'updateCompanyActivationScope',
  'companyActivationAuditList',
  'schemaAudit',
  'schemaFix',
  'repairFactorySheetLayout',
  'cleanupSafetyPreview',
  'cleanupSafetyApply',
  'cleanupRoutedSupportSheets',
  'deleteReceiptRow',
  'trip',
  'factory',
  'saveMaintenance',
  'buildDemandTemplateSheet'
]);

function getApiBaseUrl(config) {
  const tabs = Array.isArray(config && config.tabs) ? config.tabs : [];
  const adminTab = tabs.find((tab) => tab.id === 'admin_dashboard') || tabs[0];
  const adminUrl = String((adminTab && adminTab.url) || DEFAULT_ADMIN_URL || '').trim();

  try {
    const parsed = new URL(adminUrl);
    parsed.search = '';
    parsed.hash = '';
    return parsed.toString();
  } catch {
    return '';
  }
}

async function fetchWithTimeout_(url, options, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, {
      ...(options || {}),
      signal: controller.signal
    });
  } finally {
    clearTimeout(timer);
  }
}

async function postToBackendWithRetry_(apiBase, paramsText) {
  let lastError = null;

  for (let attempt = 0; attempt <= API_REQUEST_RETRIES; attempt += 1) {
    try {
      return await fetchWithTimeout_(apiBase, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
          Accept: 'application/json'
        },
        body: paramsText
      }, API_REQUEST_TIMEOUT_MS);
    } catch (error) {
      lastError = error;
    }
  }

  throw lastError || new Error('network request failed');
}

function hashPassword(password) {
  return crypto.createHash('sha256').update(String(password || ''), 'utf8').digest('hex');
}

function getDefaultConfig() {
  return {
    appTitle: 'لوحة مدير الحسابات',
    security: {
      passwordHash: hashPassword('123456'),
      passwordHint: 'كلمة المرور الافتراضية: 123456 (غيّرها فورًا من ملف الإعدادات)'
    },
    tabs: [
      {
        id: 'admin_dashboard',
        label: 'لوحة المدير العام',
        url: DEFAULT_ADMIN_URL
      },
      {
        id: 'demand_template',
        label: 'مولد المطالبات',
        url: DEFAULT_DEMAND_TEMPLATE_URL
      },
      {
        id: 'google_sheet',
        label: 'Google Sheet الرئيسي',
        url: 'https://docs.google.com/spreadsheets/d/1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM/edit'
      }
    ]
  };
}

function ensureConfigFile() {
  if (!fs.existsSync(CONFIG_PATH)) {
    fs.writeFileSync(CONFIG_PATH, JSON.stringify(getDefaultConfig(), null, 2), 'utf8');
  }
}

function loadConfig() {
  ensureConfigFile();

  try {
    const parsed = JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
    const fallback = getDefaultConfig();

    const appTitle = String(parsed.appTitle || fallback.appTitle);
    const security = parsed.security || {};
    const passwordHash = String(security.passwordHash || '').trim();
    const passwordHint = String(security.passwordHint || fallback.security.passwordHint || '');

    const tabs = Array.isArray(parsed.tabs)
      ? parsed.tabs
          .map((tab) => ({
            id: String(tab.id || '').trim(),
            label: String(tab.label || '').trim(),
            url: String(tab.url || '').trim()
          }))
          .filter((tab) => tab.id && tab.label && /^https?:\/\//i.test(tab.url))
      : fallback.tabs;

    return {
      appTitle,
      security: {
        passwordHash: passwordHash || fallback.security.passwordHash,
        passwordHint
      },
      tabs: tabs.length ? tabs : fallback.tabs
    };
  } catch (error) {
    dialog.showErrorBox('لوحة مدير الحسابات', 'تعذر قراءة ملف الإعدادات manager-config.json. سيتم استخدام الإعدادات الافتراضية.');
    return getDefaultConfig();
  }
}

function createMainWindow() {
  const config = loadConfig();

  const mainWindow = new BrowserWindow({
    width: 1500,
    height: 920,
    minWidth: 1100,
    minHeight: 700,
    autoHideMenuBar: true,
    title: config.appTitle,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:\/\//i.test(url)) {
      shell.openExternal(url);
    }
    return { action: 'deny' };
  });

  mainWindow.webContents.on('did-fail-load', (_event, errorCode, errorDescription, validatedURL) => {
    const message = [
      'تعذر تحميل لوحة المدير العام.',
      `Error: ${errorCode} - ${errorDescription}`,
      `URL: ${validatedURL || DEFAULT_ADMIN_URL}`
    ].join('\n');

    dialog.showErrorBox(config.appTitle, message);
  });

  ipcMain.handle('auth:bootstrap', async () => {
    return {
      appTitle: config.appTitle,
      tabs: config.tabs.map((tab) => ({
        id: tab.id,
        label: tab.label,
        url: tab.url,
        openMode: String(tab.openMode || 'embedded').toLowerCase() === 'external' ? 'external' : 'embedded'
      })),
      passwordHint: config.security.passwordHint
    };
  });

  ipcMain.handle('app:open-external', async (_event, url) => {
    const normalized = String(url || '').trim();
    if (!/^https?:\/\//i.test(normalized)) {
      return { success: false, message: 'invalid url' };
    }

    await shell.openExternal(normalized);
    return { success: true };
  });

  ipcMain.handle('auth:login', async (_event, password) => {
    const inputHash = hashPassword(password);
    const isValid = inputHash === config.security.passwordHash;

    if (isValid) {
      return { success: true, message: 'ok' };
    }

    return {
      success: false,
      message: 'كلمة المرور غير صحيحة'
    };
  });

  ipcMain.handle('api:request', async (_event, payload) => {
    try {
      const action = String(payload && payload.action ? payload.action : '').trim();
      if (!action) {
        return { success: false, message: 'action is required' };
      }

      if (!ALLOWED_API_ACTIONS.has(action)) {
        return { success: false, message: 'action is not allowed' };
      }

      const apiBase = getApiBaseUrl(config);
      if (!apiBase) {
        return { success: false, message: 'api base url is not configured' };
      }

      const params = new URLSearchParams();
      params.set('action', action);

      const data = payload && payload.data && typeof payload.data === 'object' ? payload.data : {};
      Object.keys(data).forEach((key) => {
        const value = data[key];
        if (value === undefined || value === null) return;
        params.set(key, String(value));
      });

      const response = await postToBackendWithRetry_(apiBase, params.toString());

      if (!response.ok) {
        return { success: false, message: `HTTP ${response.status}` };
      }

      const body = await response.json();
      if (body && body.success === false) {
        return body;
      }
      return body;
    } catch (error) {
      const msg = String(error && error.message ? error.message : error || 'network error').toLowerCase();
      if (msg.includes('fetch failed') || msg.includes('abort') || msg.includes('timeout') || msg.includes('network') || msg.includes('econn') || msg.includes('failed to fetch')) {
        return {
          success: false,
          message: 'تعذر الاتصال بخادم النظام. تحقق من الإنترنت أو جدار الحماية ثم أعد المحاولة.'
        };
      }
      return { success: false, message: String(error) };
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'dashboard.html'));
}

app.whenReady().then(() => {
  createMainWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createMainWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
