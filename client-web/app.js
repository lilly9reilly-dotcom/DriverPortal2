(() => {
  const state = {
    clients: [],
    index: {},
    session: null,
    db: null,
    receipts: [],
  };

  const $ = (id) => document.getElementById(id);

  const fmt = {
    int(n) {
      return Number(n || 0).toLocaleString("en-US", { maximumFractionDigits: 0 });
    },
    qty(n) {
      return Number(n || 0).toLocaleString("en-US", { maximumFractionDigits: 3 });
    },
    money(n) {
      return Number(n || 0).toLocaleString("en-US", { maximumFractionDigits: 0 });
    },
  };

  async function loadJson(path) {
    const res = await fetch(path);
    if (!res.ok) throw new Error("تعذر تحميل " + path);
    return res.json();
  }

  function saveSession(session) {
    localStorage.setItem("clientPortalSession", JSON.stringify(session));
  }

  function readSession() {
    try {
      return JSON.parse(localStorage.getItem("clientPortalSession") || "null");
    } catch {
      return null;
    }
  }

  function clearSession() {
    localStorage.removeItem("clientPortalSession");
  }

  function fillClientSelect() {
    const select = $("clientSelect");
    select.innerHTML = state.clients
      .map((c) => `<option value="${c.id}">${c.name} — ${c.kind}</option>`)
      .join("");
    const uni = state.clients.find((c) => c.name.includes("يونيغاز"));
    if (uni) select.value = uni.id;
    updateHint();
  }

  function updateHint() {
    const id = $("clientSelect").value;
    const client = state.clients.find((c) => c.id === id);
    if (!client) return;
    $("loginHint").textContent =
      `وضع المعاينة: رمز الدخول لـ «${client.name}» هو ${client.loginCode} — السيارات: ${client.cars.join("، ")}`;
  }

  async function login() {
    $("loginError").hidden = true;
    const id = $("clientSelect").value;
    const code = ($("loginCode").value || "").trim();
    const client = state.clients.find((c) => c.id === id);
    if (!client) {
      $("loginError").hidden = false;
      $("loginError").textContent = "اختر العميل";
      return;
    }
    if (code !== client.loginCode) {
      $("loginError").hidden = false;
      $("loginError").textContent = "رمز الدخول غير صحيح";
      return;
    }
    await openClient(client);
  }

  async function openClient(client) {
    const db = await loadJson(`data/${client.dbSheet}.json`);
    state.session = client;
    state.db = db;
    state.receipts = db.receipts || [];
    saveSession(client);
    $("loginView").classList.add("hidden");
    $("appView").classList.remove("hidden");
    $("clientTitle").textContent = client.name;
    $("clientMeta").textContent = `${client.kind} · ${client.dbSheet} · ${client.cars.length} سيارات`;
    $("dbName").textContent = client.dbSheet;
    $("aboutDb").textContent = client.dbSheet;
    fillMonths();
    renderCars();
    applyFilters();
    showTab("home");
  }

  function fillMonths() {
    const months = Array.from(new Set(state.receipts.map((r) => r.month).filter(Boolean))).sort().reverse();
    const sel = $("monthFilter");
    sel.innerHTML = `<option value="all">كل الأشهر</option>` + months.map((m) => `<option value="${m}">${m}</option>`).join("");
  }

  function filteredReceipts() {
    const month = $("monthFilter").value;
    const period = $("periodFilter").value;
    const type = $("typeFilter").value;
    const q = ($("searchInput").value || "").trim();

    return state.receipts.filter((r) => {
      if (month !== "all" && r.month !== month) return false;
      if (period === "1-15" && r.period !== "1-15") return false;
      if (period === "16-end" && !(String(r.period || "").includes("16") || String(r.period || "").includes("نهاية") || String(r.period || "").includes("end"))) {
        // experiment uses values like 16-end / 16-نهاية / 16-
        if (!String(r.period || "").startsWith("16")) return false;
      }
      if (type !== "all") {
        const movement = String(r.movement || "");
        const dest = String(r.destination || "");
        const isFactory = movement.includes("معمل") || dest.includes("معمل");
        if (type === "معمل" && !isFactory) return false;
        if (type === "محطة" && isFactory) return false;
      }
      if (q) {
        const blob = `${r.docNumber} ${r.driverName} ${r.carNumber} ${r.destination}`;
        if (!blob.includes(q)) return false;
      }
      return true;
    });
  }

  function applyFilters() {
    const rows = filteredReceipts();
    let qty = 0, amount = 0, gas = 0, station = 0, factory = 0;
    rows.forEach((r) => {
      qty += Number(r.quantity || 0);
      amount += Number(r.amount || 0);
      gas += Number(r.gasValue || 0);
      const isFactory = String(r.movement || "").includes("معمل") || String(r.destination || "").includes("معمل");
      if (isFactory) factory += 1; else station += 1;
    });
    $("statCount").textContent = fmt.int(rows.length);
    $("statQty").textContent = fmt.qty(qty);
    $("statAmount").textContent = fmt.money(amount);
    $("statGas").textContent = fmt.money(gas);
    $("statStation").textContent = fmt.int(station);
    $("statFactory").textContent = fmt.int(factory);
    renderReceipts(rows);
  }

  function renderReceipts(rows) {
    const body = $("receiptsBody");
    if (!rows.length) {
      body.innerHTML = `<tr><td colspan="8">لا توجد وصولات ضمن التصفية الحالية</td></tr>`;
      return;
    }
    body.innerHTML = rows.slice(0, 500).map((r) => {
      const isFactory = String(r.movement || "").includes("معمل") || String(r.destination || "").includes("معمل");
      const badge = isFactory
        ? `<span class="badge factory">معمل</span>`
        : `<span class="badge station">محطة</span>`;
      return `<tr>
        <td>${escapeHtml(r.docNumber)}</td>
        <td>${escapeHtml(r.carNumber)}</td>
        <td>${escapeHtml(r.driverName)}</td>
        <td>${escapeHtml(r.destination)}</td>
        <td>${badge}</td>
        <td>${fmt.qty(r.quantity)}</td>
        <td>${fmt.money(r.amount)}</td>
        <td>${escapeHtml(r.unloadDate)}</td>
      </tr>`;
    }).join("");
  }

  function renderCars() {
    const cars = state.session?.cars || [];
    const counts = {};
    state.receipts.forEach((r) => {
      const c = String(r.carNumber || "");
      if (!c) return;
      counts[c] = (counts[c] || 0) + 1;
    });
    $("carsGrid").innerHTML = cars.map((car) => `
      <div class="car-item">
        <strong>${escapeHtml(car)}</strong>
        <span>${fmt.int(counts[car] || 0)} وصل</span>
      </div>
    `).join("");
  }

  function showTab(name) {
    document.querySelectorAll(".tab").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.tab === name);
    });
    document.querySelectorAll(".tab-panel").forEach((panel) => {
      panel.classList.toggle("hidden", panel.id !== `tab-${name}`);
    });
  }

  function logout() {
    clearSession();
    state.session = null;
    state.db = null;
    state.receipts = [];
    $("appView").classList.add("hidden");
    $("loginView").classList.remove("hidden");
    $("loginCode").value = "";
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  }

  async function boot() {
    const meta = await loadJson("data/clients.json");
    state.clients = meta.clients || [];
    state.index = await loadJson("data/index.json");
    fillClientSelect();
    $("clientSelect").addEventListener("change", updateHint);
    $("loginBtn").addEventListener("click", login);
    $("loginCode").addEventListener("keydown", (e) => {
      if (e.key === "Enter") login();
    });
    $("logoutBtn").addEventListener("click", logout);
    $("refreshBtn").addEventListener("click", async () => {
      if (state.session) await openClient(state.session);
    });
    ["monthFilter", "periodFilter", "typeFilter"].forEach((id) => {
      $(id).addEventListener("change", applyFilters);
    });
    $("searchInput").addEventListener("input", applyFilters);
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
