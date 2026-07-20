const GAS_MVP_SHEET_ID = '1adlJxYSgBftTcagTAyl9GrwNwrBTPOrTDQbYeBhqCiM';
const GAS_MVP_SHEET_NAME = 'GasStation_DB';
const GAS_MVP_HEADERS = [
  'row_type',
  'record_id',
  'created_at',
  'fill_date',
  'fuel_type',
  'station_name',
  'plate_number',
  'driver_name',
  'liters',
  'price_per_liter',
  'total_amount',
  'payment_amount',
  'balance_after',
  'entered_by',
  'notes',
  'status'
];

function gasMvpDispatch(e) {
  try {
    const params = gasMvpReadParams_(e);
    const action = String(params.action || '').trim();

    let result;

    switch (action) {
      case 'gas_mvp_add_transaction':
        result = gasMvpAddTransaction_(params);
        break;

      case 'gas_mvp_add_settlement':
        result = gasMvpAddSettlement_(params);
        break;

      case 'gas_mvp_add_vehicle':
        result = gasMvpAddVehicle_(params);
        break;

      case 'gas_mvp_update_settings':
        result = gasMvpUpdateSettings_(params);
        break;

      case 'gas_mvp_get_settings':
        result = gasMvpGetSettings_(params);
        break;

      case 'gas_mvp_list':
        result = gasMvpList_(params);
        break;

      default:
        result = {
          success: false,
          message: 'Unknown action: ' + action
        };
    }

    return gasMvpJson_(result);
  } catch (error) {
    return gasMvpJson_({
      success: false,
      message: String(error && error.message ? error.message : error)
    });
  }
}

function gasMvpReadParams_(e) {
  const params = {};
  if (e && e.parameter) {
    Object.keys(e.parameter).forEach(function (key) {
      params[key] = e.parameter[key];
    });
  }

  if (e && e.postData && e.postData.contents) {
    const raw = String(e.postData.contents).trim();
    if (raw) {
      if (raw[0] === '{' || raw[0] === '[') {
        try {
          const parsed = JSON.parse(raw);
          Object.keys(parsed).forEach(function (key) {
            params[key] = parsed[key];
          });
        } catch (jsonError) {
          // ignore JSON parse failure, fall back to form parsing
        }
      } else {
        raw.split('&').forEach(function (pair) {
          if (!pair) return;
          const index = pair.indexOf('=');
          const key = decodeURIComponent((index >= 0 ? pair.slice(0, index) : pair).replace(/\+/g, ' '));
          const value = decodeURIComponent((index >= 0 ? pair.slice(index + 1) : '').replace(/\+/g, ' '));
          if (key) params[key] = value;
        });
      }
    }
  }

  return params;
}

function gasMvpJson_(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function gasMvpGetSheet_() {
  const ss = SpreadsheetApp.openById(GAS_MVP_SHEET_ID);
  let sheet = ss.getSheetByName(GAS_MVP_SHEET_NAME);

  if (!sheet) {
    sheet = ss.insertSheet(GAS_MVP_SHEET_NAME);
  }

  gasMvpEnsureHeader_(sheet);
  return sheet;
}

function gasMvpEnsureHeader_(sheet) {
  const firstRow = sheet.getRange(1, 1, 1, GAS_MVP_HEADERS.length).getValues()[0];
  const headerIsEmpty = firstRow.join('').trim() === '';

  if (headerIsEmpty) {
    sheet.getRange(1, 1, 1, GAS_MVP_HEADERS.length).setValues([GAS_MVP_HEADERS]);
    sheet.setFrozenRows(1);
    return;
  }

  const currentHeaders = firstRow.map(function (item) {
    return String(item || '').trim();
  });

  const exactMatch = GAS_MVP_HEADERS.length === currentHeaders.length && GAS_MVP_HEADERS.every(function (header, index) {
    return header === currentHeaders[index];
  });

  if (!exactMatch) {
    sheet.getRange(1, 1, 1, GAS_MVP_HEADERS.length).setValues([GAS_MVP_HEADERS]);
    sheet.setFrozenRows(1);
  }
}

function gasMvpHeaderMap_() {
  const map = {};
  GAS_MVP_HEADERS.forEach(function (header, index) {
    map[header] = index + 1;
  });
  return map;
}

function gasMvpGetLastDataRow_(sheet) {
  const lastRow = sheet.getLastRow();
  return lastRow < 2 ? 1 : lastRow;
}

function gasMvpToNumber_(value, fallbackValue) {
  const num = Number(value);
  return isNaN(num) ? fallbackValue : num;
}

function gasMvpGenerateId_(prefix) {
  const now = new Date();
  const stamp = Utilities.formatDate(now, Session.getScriptTimeZone(), 'yyyyMMddHHmmssSSS');
  return prefix + '-' + stamp;
}

function gasMvpReadAllObjects_(sheet) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) return [];

  const values = sheet.getRange(2, 1, lastRow - 1, GAS_MVP_HEADERS.length).getValues();
  return values.map(function (row) {
    const obj = {};
    GAS_MVP_HEADERS.forEach(function (header, index) {
      obj[header] = row[index];
    });
    return obj;
  });
}

function gasMvpComputeBalance_(rows) {
  let balance = 0;

  rows.forEach(function (row) {
    const rowType = String(row.row_type || '').trim().toUpperCase();
    if (rowType === 'TRANSACTION') {
      balance += gasMvpToNumber_(row.total_amount, 0);
    } else if (rowType === 'SETTLEMENT') {
      balance -= gasMvpToNumber_(row.payment_amount, 0);
    }
  });

  return balance;
}

function gasMvpFindRowIndexByRecordId_(sheet, recordId) {
  const lastRow = sheet.getLastRow();
  if (lastRow < 2) return -1;

  const recordValues = sheet.getRange(2, 2, lastRow - 1, 1).getValues();
  for (let i = 0; i < recordValues.length; i++) {
    if (String(recordValues[i][0] || '').trim() === String(recordId || '').trim()) {
      return i + 2;
    }
  }

  return -1;
}

function gasMvpAppendRowObject_(sheet, obj) {
  const row = GAS_MVP_HEADERS.map(function (header) {
    return obj[header] !== undefined ? obj[header] : '';
  });
  sheet.appendRow(row);
}

function gasMvpUpdateRowObject_(sheet, rowIndex, obj) {
  const row = GAS_MVP_HEADERS.map(function (header) {
    return obj[header] !== undefined ? obj[header] : '';
  });
  sheet.getRange(rowIndex, 1, 1, GAS_MVP_HEADERS.length).setValues([row]);
}

function gasMvpUpsertSettingRow_(sheet, key, value, stationName) {
  const existingRowIndex = gasMvpFindRowIndexByRecordId_(sheet, key);

  const rowObject = {
    row_type: 'SETTING',
    record_id: key,
    created_at: new Date().toISOString(),
    fill_date: '',
    fuel_type: '',
    station_name: stationName || '',
    plate_number: '',
    driver_name: '',
    liters: '',
    price_per_liter: value,
    total_amount: '',
    payment_amount: '',
    balance_after: '',
    entered_by: 'system',
    notes: String(value),
    status: 'ACTIVE'
  };

  if (existingRowIndex > 0) {
    gasMvpUpdateRowObject_(sheet, existingRowIndex, rowObject);
  } else {
    gasMvpAppendRowObject_(sheet, rowObject);
  }
}

function gasMvpAddTransaction_(params) {
  const sheet = gasMvpGetSheet_();
  const rows = gasMvpReadAllObjects_(sheet);
  const recordId = String(params.record_id || gasMvpGenerateId_('TX'));
  const existing = rows.some(function (row) {
    return String(row.record_id || '').trim() === recordId;
  });

  if (existing) {
    return {
      success: false,
      message: 'Duplicate record_id'
    };
  }

  const liters = gasMvpToNumber_(params.liters, 0);
  const pricePerLiter = gasMvpToNumber_(params.price_per_liter, 0);
  const totalAmount = gasMvpToNumber_(params.total_amount, liters * pricePerLiter);
  const balanceAfter = gasMvpComputeBalance_(rows) + totalAmount;

  const rowObject = {
    row_type: 'TRANSACTION',
    record_id: recordId,
    created_at: params.created_at || new Date().toISOString(),
    fill_date: params.fill_date || '',
    fuel_type: params.fuel_type || 'اعتيادي',
    station_name: params.station_name || '',
    plate_number: params.plate_number || '',
    driver_name: params.driver_name || '',
    liters: liters,
    price_per_liter: pricePerLiter,
    total_amount: totalAmount,
    payment_amount: '',
    balance_after: balanceAfter,
    entered_by: params.entered_by || '',
    notes: params.notes || '',
    status: params.status || 'ACTIVE'
  };

  gasMvpAppendRowObject_(sheet, rowObject);

  return {
    success: true,
    message: 'Transaction saved',
    record_id: recordId,
    balance_after: balanceAfter
  };
}

function gasMvpAddSettlement_(params) {
  const sheet = gasMvpGetSheet_();
  const rows = gasMvpReadAllObjects_(sheet);
  const recordId = String(params.record_id || gasMvpGenerateId_('ST'));
  const existing = rows.some(function (row) {
    return String(row.record_id || '').trim() === recordId;
  });

  if (existing) {
    return {
      success: false,
      message: 'Duplicate record_id'
    };
  }

  const paymentAmount = gasMvpToNumber_(params.payment_amount || params.amount, 0);
  const balanceAfter = gasMvpComputeBalance_(rows) - paymentAmount;

  const rowObject = {
    row_type: 'SETTLEMENT',
    record_id: recordId,
    created_at: params.created_at || new Date().toISOString(),
    fill_date: '',
    fuel_type: '',
    station_name: params.station_name || '',
    plate_number: '',
    driver_name: '',
    liters: '',
    price_per_liter: '',
    total_amount: '',
    payment_amount: paymentAmount,
    balance_after: balanceAfter,
    entered_by: params.created_by || params.entered_by || '',
    notes: params.notes || '',
    status: params.status || 'ACTIVE'
  };

  gasMvpAppendRowObject_(sheet, rowObject);

  return {
    success: true,
    message: 'Settlement saved',
    record_id: recordId,
    balance_after: balanceAfter
  };
}

function gasMvpAddVehicle_(params) {
  const sheet = gasMvpGetSheet_();
  const rows = gasMvpReadAllObjects_(sheet);
  const recordId = String(params.record_id || gasMvpGenerateId_('VH'));
  const plateNumber = String(params.plate_number || '').trim();
  const driverName = String(params.driver_name || '').trim();

  if (!plateNumber || !driverName) {
    return {
      success: false,
      message: 'plate_number and driver_name are required'
    };
  }

  const duplicate = rows.some(function (row) {
    return String(row.row_type || '').toUpperCase() === 'VEHICLE' &&
      String(row.plate_number || '').trim() === plateNumber &&
      String(row.driver_name || '').trim() === driverName;
  });

  if (duplicate) {
    return {
      success: false,
      message: 'Vehicle already exists'
    };
  }

  const rowObject = {
    row_type: 'VEHICLE',
    record_id: recordId,
    created_at: params.created_at || new Date().toISOString(),
    fill_date: '',
    fuel_type: '',
    station_name: params.station_name || '',
    plate_number: plateNumber,
    driver_name: driverName,
    liters: '',
    price_per_liter: '',
    total_amount: '',
    payment_amount: '',
    balance_after: '',
    entered_by: params.entered_by || 'system',
    notes: params.notes || '',
    status: params.active || params.status || 'ACTIVE'
  };

  gasMvpAppendRowObject_(sheet, rowObject);

  return {
    success: true,
    message: 'Vehicle saved',
    record_id: recordId
  };
}

function gasMvpUpdateSettings_(params) {
  const sheet = gasMvpGetSheet_();
  const stationName = params.station_name || '';
  const normalPrice = gasMvpToNumber_(params.default_price_normal, 0);
  const commercialPrice = gasMvpToNumber_(params.default_price_commercial, 0);

  if (normalPrice > 0) {
    gasMvpUpsertSettingRow_(sheet, 'default_price_normal', normalPrice, stationName);
  }

  if (commercialPrice > 0) {
    gasMvpUpsertSettingRow_(sheet, 'default_price_commercial', commercialPrice, stationName);
  }

  if (stationName) {
    gasMvpUpsertSettingRow_(sheet, 'station_name', stationName, stationName);
  }

  return {
    success: true,
    message: 'Settings updated'
  };
}

function gasMvpGetSettings_() {
  const sheet = gasMvpGetSheet_();
  const rows = gasMvpReadAllObjects_(sheet).filter(function (row) {
    return String(row.row_type || '').toUpperCase() === 'SETTING';
  });

  const settings = {
    station_name: '',
    default_price_normal: 430,
    default_price_commercial: 430
  };

  rows.forEach(function (row) {
    const key = String(row.record_id || '').trim();
    if (key === 'station_name') {
      settings.station_name = String(row.station_name || row.notes || '').trim();
    } else if (key === 'default_price_normal') {
      settings.default_price_normal = gasMvpToNumber_(row.price_per_liter || row.notes, 430);
    } else if (key === 'default_price_commercial') {
      settings.default_price_commercial = gasMvpToNumber_(row.price_per_liter || row.notes, 430);
    }
  });

  return {
    success: true,
    settings: settings
  };
}

function gasMvpList_(params) {
  const sheet = gasMvpGetSheet_();
  const rows = gasMvpReadAllObjects_(sheet);
  const filterType = String(params.row_type || '').trim().toUpperCase();

  const filtered = filterType
    ? rows.filter(function (row) {
        return String(row.row_type || '').trim().toUpperCase() === filterType;
      })
    : rows;

  return {
    success: true,
    count: filtered.length,
    balance: gasMvpComputeBalance_(rows),
    rows: filtered
  };
}