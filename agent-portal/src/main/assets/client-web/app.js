(() => {
  const cfg = window.CLIENT_PORTAL_CONFIG || { mode: "demo", apiUrl: "", allowLiveWrite: false };
  const state = {
    clients: [],
    session: null,
    db: null,
    receipts: [],
    moves: [],
    chart: null,
    payout: { zainCash: "", mastercard: "", bankCard: "", bankAccount: "", bankName: "", notes: "" },
    settlements: [],
    notices: []
  };

  const $ = (id) => document.getElementById(id);
  const fmt = {
    int(n) { return Number(n || 0).toLocaleString("en-US", { maximumFractionDigits: 0 }); },
    qty(n) { return Number(n || 0).toLocaleString("en-US", { maximumFractionDigits: 3 }); },
    money(n) { return Number(n || 0).toLocaleString("en-US", { maximumFractionDigits: 0 }); }
  };

  function esc(v) {
    return String(v ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  async function loadJson(path) {
    const res = await fetch(path);
    if (!res.ok) throw new Error("تعذر تحميل " + path);
    return res.json();
  }

  function movesKey(id) { return "clientPortalMoves:" + id; }
  function readMoves(id) {
    try { return JSON.parse(localStorage.getItem(movesKey(id)) || "[]"); }
    catch { return []; }
  }
  function writeMoves(id, moves) {
    localStorage.setItem(movesKey(id), JSON.stringify(moves));
  }
  function saveSession(s) { localStorage.setItem("clientPortalSession", JSON.stringify(s)); }
  function readSession() {
    try { return JSON.parse(localStorage.getItem("clientPortalSession") || "null"); }
    catch { return null; }
  }
  function clearSession() { localStorage.removeItem("clientPortalSession"); }

  function payoutKey(id) { return "clientPortalPayout:" + id; }
  function settlementsKey(id) { return "clientPortalSettlements:" + id; }
  function noticesKey(id) { return "clientPortalNotices:" + id; }
  function adminNoticesKey() { return "adminMoneyNotices"; }

  function readPayout(id) {
    try { return Object.assign({ zainCash: "", mastercard: "", bankCard: "", bankAccount: "", bankName: "", notes: "" }, JSON.parse(localStorage.getItem(payoutKey(id)) || "{}")); }
    catch { return { zainCash: "", mastercard: "", bankCard: "", bankAccount: "", bankName: "", notes: "" }; }
  }
  function writePayout(id, payout) { localStorage.setItem(payoutKey(id), JSON.stringify(payout)); }

  function readSettlements(id) {
    try { return JSON.parse(localStorage.getItem(settlementsKey(id)) || "[]"); }
    catch { return []; }
  }
  function writeSettlements(id, rows) { localStorage.setItem(settlementsKey(id), JSON.stringify(rows)); }

  function readNotices(id) {
    try { return JSON.parse(localStorage.getItem(noticesKey(id)) || "[]"); }
    catch { return []; }
  }
  function writeNotices(id, rows) { localStorage.setItem(noticesKey(id), JSON.stringify(rows)); }

  function readAdminNotices() {
    try { return JSON.parse(localStorage.getItem(adminNoticesKey()) || "[]"); }
    catch { return []; }
  }
  function writeAdminNotices(rows) { localStorage.setItem(adminNoticesKey(), JSON.stringify(rows)); }

  function maskSecret(v) {
    const s = String(v || "").trim();
    if (!s) return "غير مضاف";
    if (s.length <= 4) return s;
    return "****" + s.slice(-4);
  }

  function isFactory(r) {
    return String(r.movement || "").includes("معمل") || String(r.destination || "").includes("معمل");
  }
  function tripStatus(r) {
    if (r.status) return r.status;
    if (r.unloadDate) return "مكتمل";
    if (r.loadDate) return "نقل";
    return "جديد";
  }

  async function apiLogin(code) {
    if (cfg.mode === "live" && cfg.apiUrl) {
      const url = cfg.apiUrl + (cfg.apiUrl.includes("?") ? "&" : "?") +
        "action=clientLogin&code=" + encodeURIComponent(code);
      const res = await fetch(url);
      if (!res.ok) throw new Error("فشل الاتصال بالخادم");
      const json = await res.json();
      if (!json.success) throw new Error(json.message || "كود غير صحيح");
      return json.data;
    }
    const client = state.clients.find((c) => String(c.loginCode).toUpperCase() === code.toUpperCase());
    if (!client) throw new Error("كود الدخول غير صحيح");
    return client;
  }

  async function apiLoadDb(client) {
    if (cfg.mode === "live" && cfg.apiUrl) {
      const url = cfg.apiUrl + (cfg.apiUrl.includes("?") ? "&" : "?") +
        "action=clientGetState&code=" + encodeURIComponent(client.loginCode) +
        "&dbSheet=" + encodeURIComponent(client.dbSheet);
      const res = await fetch(url);
      if (!res.ok) throw new Error("تعذر جلب القاعدة");
      const json = await res.json();
      if (!json.success) throw new Error(json.message || "فشل جلب البيانات");
      return json.data;
    }
    return loadJson("data/" + client.dbSheet + ".json");
  }

  async function doLogin() {
    $("loginError").hidden = true;
    const code = ($("loginCode").value || "").trim();
    if (!code) {
      $("loginError").hidden = false;
      $("loginError").textContent = "أدخل كود الدخول";
      return;
    }
    try {
      await openClient(await apiLogin(code));
    } catch (err) {
      $("loginError").hidden = false;
      $("loginError").textContent = err.message || String(err);
    }
  }

  async function openClient(client) {
    const db = await apiLoadDb(client);
    state.session = client;
    state.db = db;
    state.receipts = db.receipts || [];
    state.moves = readMoves(client.id);
    state.payout = readPayout(client.id);
    state.settlements = readSettlements(client.id);
    state.notices = readNotices(client.id);
    saveSession(client);
    $("loginView").classList.add("hidden");
    $("appView").classList.remove("hidden");
    $("clientTitle").textContent = client.name;
    $("clientMeta").textContent = client.kind + " · " + client.dbSheet + " · " + client.cars.length + " سيارات";
    $("dbName").textContent = client.dbSheet;
    fillMonths();
    renderAll();
    showTab("home");
  }

  function fillMonths() {
    const months = Array.from(new Set(state.receipts.map((r) => r.month).filter(Boolean))).sort().reverse();
    $("monthFilter").innerHTML =
      '<option value="all">كل الأشهر</option>' +
      months.map((m) => '<option value="' + m + '">' + m + "</option>").join("");
  }

  function filteredTrips() {
    const month = $("monthFilter").value;
    const type = $("typeFilter").value;
    const q = ($("searchInput").value || "").trim();
    return state.receipts.filter((r) => {
      if (month !== "all" && r.month !== month) return false;
      if (type === "معمل" && !isFactory(r)) return false;
      if (type === "محطة" && isFactory(r)) return false;
      if (q) {
        const blob = [r.docNumber, r.driverName, r.carNumber, r.destination].join(" ");
        if (!blob.includes(q)) return false;
      }
      return true;
    });
  }

  function totals(rows) {
    let qty = 0, amount = 0, gas = 0, station = 0, factory = 0;
    rows.forEach((r) => {
      qty += Number(r.quantity || 0);
      amount += Number(r.amount || 0);
      gas += Number(r.gasValue || 0);
      if (isFactory(r)) factory += 1;
      else station += 1;
    });
    const received = state.moves
      .filter((m) => m.type === "استلام")
      .reduce((s, m) => s + Number(m.amount || 0), 0);
    return { qty, amount, gas, station, factory, received, balance: amount - received };
  }

  function renderAll() {
    const rows = filteredTrips();
    const t = totals(rows);
    $("statBalance").textContent = fmt.money(t.balance);
    $("statCount").textContent = fmt.int(rows.length);
    $("statQty").textContent = fmt.qty(t.qty);
    $("statAmount").textContent = fmt.money(t.amount);
    $("statStation").textContent = fmt.int(t.station);
    $("statFactory").textContent = fmt.int(t.factory);
    $("accEarned").textContent = fmt.money(t.amount);
    $("accReceived").textContent = fmt.money(t.received);
    $("accBalance").textContent = fmt.money(t.balance);
    $("accGas").textContent = fmt.money(t.gas);
    $("repStation").textContent = fmt.int(t.station);
    $("repFactory").textContent = fmt.int(t.factory);

    if (state.moves.length) {
      const last = state.moves[state.moves.length - 1];
      $("moneyNotice").classList.remove("hidden");
      $("moneyNotice").textContent = "آخر استلام: " + fmt.money(last.amount) + " د.ع عبر " + last.method;
    } else {
      $("moneyNotice").classList.add("hidden");
    }

    renderTrips(rows);
    renderStatements(rows.filter((r) => !isFactory(r)));
    renderMoves();
    renderCars();
    renderChart(t);
    renderPayoutCards();
    fillPayoutForm();
    renderSettlements();
    renderNotices();
    updateReceiveMethodOptions();
  }

  function renderTrips(rows) {
    const body = $("tripsBody");
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="9">لا توجد نقلات ضمن التصفية</td></tr>';
      return;
    }
    body.innerHTML = rows.slice(0, 500).map((r) => {
      const badge = isFactory(r)
        ? '<span class="badge factory">معمل</span>'
        : '<span class="badge station">محطة</span>';
      return "<tr>" +
        "<td>" + esc(r.docNumber) + "</td>" +
        "<td>" + esc(r.carNumber) + "</td>" +
        "<td>" + esc(r.driverName) + "</td>" +
        "<td>" + esc(r.destination) + "</td>" +
        "<td>" + badge + "</td>" +
        '<td><span class="badge status">' + esc(tripStatus(r)) + "</span></td>" +
        "<td>" + fmt.qty(r.quantity) + "</td>" +
        "<td>" + fmt.money(r.amount) + "</td>" +
        "<td>" + esc(r.unloadDate) + "</td>" +
        "</tr>";
    }).join("");
  }

  function renderStatements(rows) {
    const body = $("statementsBody");
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="6">لا توجد وصولات محطات</td></tr>';
      return;
    }
    body.innerHTML = rows.slice(0, 500).map((r) =>
      "<tr>" +
      "<td>" + esc(r.docNumber) + "</td>" +
      "<td>" + esc(r.carNumber) + "</td>" +
      "<td>" + esc(r.destination) + "</td>" +
      "<td>" + fmt.qty(r.quantity) + "</td>" +
      "<td>" + fmt.money(r.amount) + "</td>" +
      "<td>" + esc(r.unloadDate) + "</td>" +
      "</tr>"
    ).join("");
  }

  function renderMoves() {
    const body = $("movesBody");
    if (!state.moves.length) {
      body.innerHTML = '<tr><td colspan="5">لا توجد حركات بعد</td></tr>';
      return;
    }
    body.innerHTML = state.moves.slice().reverse().map((m) =>
      "<tr>" +
      "<td>" + esc(m.date) + "</td>" +
      "<td>" + esc(m.type) + "</td>" +
      "<td>" + esc(m.method) + "</td>" +
      "<td>" + fmt.money(m.amount) + "</td>" +
      "<td>" + esc(m.note || "") + "</td>" +
      "</tr>"
    ).join("");
  }

  function renderCars() {
    const cars = (state.session && state.session.cars) || [];
    const counts = {};
    state.receipts.forEach((r) => {
      const c = String(r.carNumber || "");
      if (!c) return;
      counts[c] = (counts[c] || 0) + 1;
    });
    $("carsGrid").innerHTML = cars.map((car) =>
      '<div class="car-item"><strong>' + esc(car) + "</strong><span>" +
      fmt.int(counts[car] || 0) + " وصل</span></div>"
    ).join("");
  }

  function renderChart(t) {
    const canvas = $("tripsChart");
    if (!canvas || typeof Chart === "undefined") return;
    if (state.chart) state.chart.destroy();
    state.chart = new Chart(canvas, {
      type: "doughnut",
      data: {
        labels: ["محطات", "معامل"],
        datasets: [{ data: [t.station, t.factory], backgroundColor: ["#1d5f8a", "#1c7a54"] }]
      },
      options: {
        plugins: { legend: { position: "bottom", labels: { font: { family: "Alyamama" } } } }
      }
    });
  }

  function showTab(name) {
    document.querySelectorAll(".tab").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.tab === name);
    });
    document.querySelectorAll(".tab-panel").forEach((panel) => {
      panel.classList.toggle("hidden", panel.id !== "tab-" + name);
    });
  }

  function doLogout(e) {
    if (e) e.preventDefault();
    clearSession();
    state.session = null;
    state.db = null;
    state.receipts = [];
    state.moves = [];
    if (state.chart) {
      try { state.chart.destroy(); } catch (_) {}
      state.chart = null;
    }
    $("appView").classList.add("hidden");
    $("loginView").classList.remove("hidden");
    $("loginCode").value = "";
    $("loginError").hidden = true;
  }

  function registerReceive() {
    if (!state.session) return;
    const amount = Number($("receiveAmount").value || 0);
    const method = $("receiveMethod").value;
    if (!(amount > 0)) {
      $("receiveMsg").textContent = "أدخل مبلغاً صحيحاً";
      return;
    }
    if (cfg.mode === "live" && !cfg.allowLiveWrite) {
      $("receiveMsg").textContent = "الكتابة على الحي مغلقة — التسجيل محلي للمعاينة فقط";
    }
    state.moves.push({
      date: new Date().toISOString().slice(0, 10),
      type: "استلام",
      method: method,
      amount: amount,
      note: "تسجيل من تطبيق المعتمد"
    });
    writeMoves(state.session.id, state.moves);
    $("receiveAmount").value = "";
    $("receiveMsg").textContent = "تم تسجيل الاستلام محلياً";
    renderAll();
  }

  function exportCsv() {
    const rows = filteredTrips();
    const header = ["الوصل", "السيارة", "السائق", "الوجهة", "النوع", "الحالة", "الكمية", "المبلغ", "التفريغ"];
    const lines = [header.join(",")].concat(rows.map((r) => [
      r.docNumber, r.carNumber, r.driverName, r.destination,
      isFactory(r) ? "معمل" : "محطة", tripStatus(r), r.quantity, r.amount, r.unloadDate
    ].map((x) => '"' + String(x ?? "").replaceAll('"', '""') + '"').join(",")));
    const blob = new Blob(["\ufeff" + lines.join("\n")], { type: "text/csv;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = ((state.session && state.session.dbSheet) || "client") + "_trips.csv";
    a.click();
  }


  function fillPayoutForm() {
    const p = state.payout || {};
    if ($("payZain")) $("payZain").value = p.zainCash || "";
    if ($("payMaster")) $("payMaster").value = p.mastercard || "";
    if ($("payBankCard")) $("payBankCard").value = p.bankCard || "";
    if ($("payBankName")) $("payBankName").value = p.bankName || "";
    if ($("payBankAccount")) $("payBankAccount").value = p.bankAccount || "";
    if ($("payNotes")) $("payNotes").value = p.notes || "";
  }

  function renderPayoutCards() {
    const p = state.payout || {};
    if ($("payoutCard")) $("payoutCard").textContent = p.mastercard ? maskSecret(p.mastercard) : "غير مضاف — من الإعدادات";
    if ($("payoutBank")) {
      const bank = [p.bankName, p.bankAccount ? maskSecret(p.bankAccount) : "", p.bankCard ? ("بطاقة " + maskSecret(p.bankCard)) : ""]
        .filter(Boolean).join(" · ");
      $("payoutBank").textContent = bank || "غير مضاف — من الإعدادات";
    }
    if ($("payoutZain")) $("payoutZain").textContent = p.zainCash ? p.zainCash : "غير مضاف — من الإعدادات";
  }

  function updateReceiveMethodOptions() {
    const sel = $("receiveMethod");
    if (!sel) return;
    const p = state.payout || {};
    const opts = [];
    if (p.zainCash) opts.push(["زين كاش", "زين كاش"]);
    if (p.mastercard) opts.push(["ماستركارد", "ماستركارد"]);
    if (p.bankCard) opts.push(["بطاقة مصرفية", "بطاقة مصرفية"]);
    if (p.bankAccount || p.bankName) opts.push(["حساب مصرفي", "حساب مصرفي"]);
    opts.push(["نقداً", "نقداً"]);
    const current = sel.value;
    sel.innerHTML = opts.map(([v, l]) => '<option value="' + v + '">' + l + "</option>").join("");
    if (opts.some(([v]) => v === current)) sel.value = current;
  }

  function savePayoutMethods() {
    if (!state.session) return;
    const payout = {
      zainCash: ($("payZain").value || "").trim(),
      mastercard: ($("payMaster").value || "").trim(),
      bankCard: ($("payBankCard").value || "").trim(),
      bankName: ($("payBankName").value || "").trim(),
      bankAccount: ($("payBankAccount").value || "").trim(),
      notes: ($("payNotes").value || "").trim()
    };
    if (!payout.zainCash && !payout.mastercard && !payout.bankCard && !payout.bankAccount) {
      $("payoutSaveMsg").textContent = "أضف وسيلة واحدة على الأقل";
      return;
    }
    state.payout = payout;
    writePayout(state.session.id, payout);
    $("payoutSaveMsg").textContent = "تم حفظ وسائل الاستلام";
    renderPayoutCards();
    updateReceiveMethodOptions();
  }

  function renderSettlements() {
    const body = $("settlementsBody");
    if (!body) return;
    const rows = state.settlements || [];
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="6">لا توجد تسويات بعد. ستظهر هنا عند إرسال الإدارة مبلغاً لك.</td></tr>';
      return;
    }
    body.innerHTML = rows.slice().reverse().map((s) => {
      const canConfirm = s.status === "مرسل";
      return "<tr>" +
        "<td>" + esc(s.settlementId) + "</td>" +
        "<td>" + fmt.money(s.amount) + "</td>" +
        "<td>" + esc(s.method) + "</td>" +
        "<td>" + esc(s.methodDetail || "—") + "</td>" +
        "<td><span class=\"badge status\">" + esc(s.status) + "</span></td>" +
        "<td>" + (canConfirm
          ? ('<button type="button" class="btn-primary btn-small" data-confirm-settlement="' + esc(s.settlementId) + '" onclick="window.__confirmClientSettlement && window.__confirmClientSettlement(\'' + String(s.settlementId).replace(/'/g,'') + '\')">تأكيد الاستلام</button>')
          : "تم") + "</td>" +
        "</tr>";
    }).join("");
  }

  function renderNotices() {
    const list = $("notifList");
    const badge = $("notifBadge");
    const notice = $("moneyNotice");
    const rows = state.notices || [];
    const unread = rows.filter((n) => !n.read).length;
    if (badge) {
      if (unread > 0) {
        badge.classList.remove("hidden");
        badge.textContent = String(unread);
      } else {
        badge.classList.add("hidden");
      }
    }
    if (notice) {
      const latest = rows.find((n) => !n.read) || rows[0];
      if (latest) {
        notice.classList.remove("hidden");
        notice.textContent = latest.title + ": " + latest.text;
      }
    }
    if (!list) return;
    if (!rows.length) {
      list.innerHTML = '<p class="muted">لا توجد إشعارات حالياً.</p>';
      return;
    }
    list.innerHTML = rows.slice().reverse().map((n) =>
      '<article class="notif-item' + (n.read ? "" : " unread") + '">' +
      "<h4>" + esc(n.title) + "</h4>" +
      "<p>" + esc(n.text) + "</p>" +
      '<div class="notif-meta">' + esc(n.time || "") + " · " + esc(n.method || "") + "</div>" +
      "</article>"
    ).join("");
  }

  function openNotifDrawer() {
    renderNotices();
    $("notifDrawer").classList.remove("hidden");
    // mark read
    state.notices = (state.notices || []).map((n) => Object.assign({}, n, { read: true }));
    if (state.session) writeNotices(state.session.id, state.notices);
    renderNotices();
  }

  function closeNotifDrawer() {
    $("notifDrawer").classList.add("hidden");
  }

  function methodDetailFor(method) {
    const p = state.payout || {};
    if (method.indexOf("زين") >= 0) return p.zainCash || "";
    if (method.indexOf("ماستر") >= 0) return p.mastercard || "";
    if (method.indexOf("بطاقة") >= 0) return p.bankCard || "";
    if (method.indexOf("حساب") >= 0 || method.indexOf("مصرف") >= 0) {
      return [p.bankName, p.bankAccount].filter(Boolean).join(" / ");
    }
    return "";
  }

  function confirmSettlement(settlementId) {
    if (!state.session) return;
    const row = (state.settlements || []).find((s) => s.settlementId === settlementId);
    if (!row || row.status !== "مرسل") return;
    row.status = "مستلم";
    row.receiveNote = "أكد المعتمد الاستلام من التطبيق";
    row.receivedAt = new Date().toISOString();
    writeSettlements(state.session.id, state.settlements);

    state.moves.push({
      date: new Date().toISOString().slice(0, 10),
      type: "استلام",
      method: row.method,
      amount: Number(row.amount || 0),
      note: "تسوية " + settlementId
    });
    writeMoves(state.session.id, state.moves);

    const notice = {
      id: "NTC-" + Date.now(),
      time: new Date().toLocaleString("en-GB"),
      title: "تم تأكيد وصول المبلغ",
      text: "تم تسجيل استلام " + fmt.money(row.amount) + " د.ع عبر " + row.method,
      amount: row.amount,
      method: row.method,
      read: false,
      settlementId: settlementId
    };
    state.notices.push(notice);
    writeNotices(state.session.id, state.notices);

    const adminRows = readAdminNotices();
    adminRows.push({
      id: "ADM-" + Date.now(),
      time: new Date().toLocaleString("en-GB"),
      clientCode: state.session.loginCode,
      clientName: state.session.name,
      dbSheet: state.session.dbSheet,
      title: "المعتمد أكد استلام المبلغ",
      text: state.session.name + " أكّد استلام " + fmt.money(row.amount) + " د.ع عبر " + row.method,
      amount: row.amount,
      method: row.method,
      settlementId: settlementId,
      read: false
    });
    writeAdminNotices(adminRows);
    renderAll();
  }

  // Demo helper used by local admin page / console: simulate admin sending money.
  window.__demoAdminSendSettlement = function(payload) {
    payload = payload || {};
    const code = String(payload.code || "").toUpperCase();
    const client = state.clients.find((c) => String(c.loginCode).toUpperCase() === code) || state.session;
    if (!client) return { success: false, message: "معتمد غير موجود" };
    const amount = Number(payload.amount || 0);
    const method = String(payload.method || "زين كاش");
    if (!(amount > 0)) return { success: false, message: "مبلغ غير صالح" };
    const payout = readPayout(client.id);
    let methodDetail = "";
    if (method.indexOf("زين") >= 0) methodDetail = payout.zainCash || "";
    else if (method.indexOf("ماستر") >= 0) methodDetail = payout.mastercard || "";
    else if (method.indexOf("بطاقة") >= 0) methodDetail = payout.bankCard || "";
    else methodDetail = [payout.bankName, payout.bankAccount].filter(Boolean).join(" / ");

    const settlementId = "STL-" + Date.now();
    const settlements = readSettlements(client.id);
    settlements.push({
      settlementId: settlementId,
      amount: amount,
      method: method,
      methodDetail: methodDetail,
      status: "مرسل",
      sendNote: payload.note || "تحويل من الإدارة",
      receiveNote: "",
      receivedAt: "",
      time: new Date().toLocaleString("en-GB")
    });
    writeSettlements(client.id, settlements);

    const notices = readNotices(client.id);
    notices.push({
      id: "NTC-" + Date.now(),
      time: new Date().toLocaleString("en-GB"),
      title: "تم إرسال مبلغ إليك",
      text: "تم تحويل " + Number(amount).toLocaleString("en-US") + " د.ع عبر " + method + (methodDetail ? " (" + methodDetail + ")" : ""),
      amount: amount,
      method: method,
      read: false,
      settlementId: settlementId
    });
    writeNotices(client.id, notices);

    const adminRows = readAdminNotices();
    adminRows.push({
      id: "ADM-" + Date.now(),
      time: new Date().toLocaleString("en-GB"),
      clientCode: client.loginCode,
      clientName: client.name,
      dbSheet: client.dbSheet,
      title: "تم تسجيل إرسال للمعتمد",
      text: "أُرسل " + Number(amount).toLocaleString("en-US") + " د.ع إلى " + client.name + " عبر " + method,
      amount: amount,
      method: method,
      settlementId: settlementId,
      read: false
    });
    writeAdminNotices(adminRows);

    if (state.session && state.session.id === client.id) {
      state.settlements = settlements;
      state.notices = notices;
      renderAll();
    }
    return { success: true, settlementId: settlementId };
  };

  async function boot() {
    const meta = await loadJson("data/clients.json");
    state.clients = meta.clients || [];
    $("loginBtn").addEventListener("click", doLogin);
    $("loginCode").addEventListener("keydown", (e) => { if (e.key === "Enter") doLogin(); });
    $("logoutBtn").addEventListener("click", doLogout);
    $("refreshBtn").addEventListener("click", async () => {
      if (state.session) await openClient(state.session);
    });
    $("receiveBtn").addEventListener("click", registerReceive);

    if ($("savePayoutBtn")) $("savePayoutBtn").addEventListener("click", savePayoutMethods);
    if ($("notifBtn")) $("notifBtn").addEventListener("click", openNotifDrawer);
    if ($("closeNotifBtn")) $("closeNotifBtn").addEventListener("click", closeNotifDrawer);
    document.addEventListener("click", (e) => {
      const btn = e.target.closest("[data-confirm-settlement]");
      if (!btn) return;
      confirmSettlement(btn.getAttribute("data-confirm-settlement"));
    });

    $("exportBtn").addEventListener("click", exportCsv);
    ["monthFilter", "typeFilter"].forEach((id) => $(id).addEventListener("change", renderAll));
    $("searchInput").addEventListener("input", renderAll);
    document.querySelectorAll(".tab").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.preventDefault();
        showTab(btn.dataset.tab);
      });
    });
    document.querySelectorAll(".tab").forEach((btn) => {
      btn.addEventListener("click", (e) => {
        e.preventDefault();
        showTab(btn.dataset.tab);
      });
    });
    document.querySelector(".tabs")?.addEventListener("click", (e) => {
      const btn = e.target.closest(".tab");
      if (!btn) return;
      e.preventDefault();
      showTab(btn.dataset.tab);
    });

    const params = new URLSearchParams(location.search);
    const demoCode = (params.get("code") || "").trim();
    if (demoCode) {
      $("loginCode").value = demoCode;
      await doLogin();
      const demoTab = (params.get("tab") || "").trim();
      if (demoTab) showTab(demoTab);
      return;
    }

    const existing = readSession();
    if (existing && state.clients.some((c) => c.id === existing.id)) {
      await openClient(existing);
    }
  }

  boot().catch((err) => {
    console.error(err);
    $("loginError").hidden = false;
    $("loginError").textContent = "تعذر تشغيل البوابة: " + err.message;
  });
})();
