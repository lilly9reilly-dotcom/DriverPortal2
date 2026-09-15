(() => {
  const cfg = window.CLIENT_PORTAL_CONFIG || { mode: "demo", apiUrl: "", allowLiveWrite: false };
  const state = {
    clients: [],
    session: null,
    db: null,
    receipts: [],
    moves: [],
    chart: null
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
      note: "تسجيل من بوابة العميل"
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
