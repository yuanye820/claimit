const STORAGE = {
  token: "claimit.token",
  listId: "claimit.listId",
};

const DEFAULT_API_BASE =
  window.location.hostname === "localhost" ||
  window.location.hostname === "127.0.0.1"
    ? "http://127.0.0.1:8000"
    : "/claimit-api";

const state = {
  token: localStorage.getItem(STORAGE.token) || "",
  apiBase: DEFAULT_API_BASE,
  listId: Number(localStorage.getItem(STORAGE.listId)) || null,
};

const els = {
  authCard: document.getElementById("auth-card"),
  appArea: document.getElementById("app-area"),
  email: document.getElementById("email"),
  password: document.getElementById("password"),
  displayName: document.getElementById("display-name"),
  authStatus: document.getElementById("auth-status"),
  listName: document.getElementById("list-name"),
  listId: document.getElementById("list-id"),
  createList: document.getElementById("create-list"),
  loadItems: document.getElementById("load-items"),
  itemName: document.getElementById("item-name"),
  itemTotal: document.getElementById("item-total"),
  addItem: document.getElementById("add-item"),
  items: document.getElementById("items"),
  itemsStatus: document.getElementById("items-status"),
  register: document.getElementById("register"),
  login: document.getElementById("login"),
  logout: document.getElementById("logout"),
};

function save() {
  localStorage.setItem(STORAGE.token, state.token);
  localStorage.setItem(STORAGE.listId, state.listId ? String(state.listId) : "");
}

function showAuth(message = "") {
  els.authCard.hidden = false;
  els.appArea.hidden = true;
  if (message) {
    setStatus(els.authStatus, message);
  }
}

function showApp(message = "") {
  els.authCard.hidden = true;
  els.appArea.hidden = false;
  if (message) {
    setStatus(els.itemsStatus, message);
  }
}

function setStatus(element, text, kind = "") {
  element.textContent = text;
  element.className = `status ${kind}`.trim();
}

function describe(error) {
  if (error.status === 401) {
    return "Not signed in or token expired. Please log in again.";
  }
  if (error.status === 409) {
    return "Conflict: someone else claimed the last unit.";
  }
  const detail = error.data && error.data.detail ? `: ${error.data.detail}` : "";
  return `${error.message}${detail}`;
}

async function api(path, options = {}) {
  const base = state.apiBase.replace(/\/+$/, "");
  const headers = { Accept: "application/json", ...(options.headers || {}) };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }

  let body = options.body;
  if (body !== undefined && body !== null) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(body);
  }

  const response = await fetch(base + path, { ...options, headers, body });
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    const error = new Error(`HTTP ${response.status}`);
    error.status = response.status;
    error.data = data;
    throw error;
  }
  return data;
}

async function register() {
  const email = els.email.value.trim();
  const password = els.password.value;
  const displayName = els.displayName.value.trim() || email;
  if (!email || !password) {
    setStatus(els.authStatus, "Email and password are required.", "warn");
    return;
  }
  try {
    const data = await api("/auth/register", {
      method: "POST",
      body: { email, password, display_name: displayName },
    });
    state.token = data.access_token;
    save();
    els.password.value = "";
    showApp("Registered and signed in.");
  } catch (error) {
    setStatus(els.authStatus, describe(error), "warn");
  }
}

async function login() {
  const email = els.email.value.trim();
  const password = els.password.value;
  if (!email || !password) {
    setStatus(els.authStatus, "Email and password are required.", "warn");
    return;
  }
  try {
    const data = await api("/auth/login", {
      method: "POST",
      body: { email, password },
    });
    state.token = data.access_token;
    save();
    els.password.value = "";
    showApp("Signed in. Create or load a list.");
  } catch (error) {
    setStatus(els.authStatus, describe(error), "warn");
  }
}

async function createList() {
  if (!state.token) {
    showAuth("Log in first.");
    return;
  }
  const name = els.listName.value.trim();
  if (!name) {
    setStatus(els.itemsStatus, "List name is required.", "warn");
    return;
  }
  try {
    const data = await api("/lists", { method: "POST", body: { name } });
    state.listId = data.id;
    save();
    els.listId.value = String(data.id);
    setStatus(els.itemsStatus, `List ${data.id} created (code ${data.code}).`, "ok");
  } catch (error) {
    setStatus(els.itemsStatus, describe(error), "warn");
  }
}

function currentListId() {
  const value = Number(els.listId.value);
  if (Number.isInteger(value) && value > 0) {
    return value;
  }
  return state.listId;
}

async function addItem() {
  if (!state.token) {
    showAuth("Log in first.");
    return;
  }
  const listId = currentListId();
  if (!listId) {
    setStatus(els.itemsStatus, "Create a list or enter a list ID first.", "warn");
    return;
  }
  const name = els.itemName.value.trim();
  const total = Number(els.itemTotal.value);
  if (!name || !Number.isInteger(total) || total <= 0) {
    setStatus(els.itemsStatus, "Item name and total > 0 are required.", "warn");
    return;
  }
  try {
    const data = await api(`/lists/${listId}/items`, {
      method: "POST",
      body: { name, total },
    });
    await loadItems(`Item ${data.id} added.`);
  } catch (error) {
    setStatus(els.itemsStatus, describe(error), "warn");
  }
}

function renderItems(items) {
  els.items.replaceChildren();
  if (!items.length) {
    const empty = document.createElement("p");
    empty.textContent = "No items yet.";
    els.items.append(empty);
    return;
  }

  for (const item of items) {
    const card = document.createElement("article");
    card.className = "item";

    const title = document.createElement("h3");
    title.textContent = item.name;

    const meta = document.createElement("p");
    meta.textContent = `id ${item.id} · remaining ${item.remaining} / ${item.total}`;

    const badge = document.createElement("span");
    badge.className = "badge";
    badge.textContent = item.remaining > 0
      ? `${item.remaining} available`
      : "fully claimed";

    const actions = document.createElement("div");
    actions.className = "row";

    const claim = document.createElement("button");
    claim.textContent = "Claim 1";
    claim.addEventListener("click", () => claimItem(item.id));

    const release = document.createElement("button");
    release.textContent = "Release 1";
    release.className = "secondary";
    release.addEventListener("click", () => releaseItem(item.id));

    const history = document.createElement("div");
    history.className = "history";

    const historyButton = document.createElement("button");
    historyButton.textContent = "History";
    historyButton.className = "secondary";
    historyButton.addEventListener("click", () => showHistory(item.id, history));

    actions.append(claim, release, historyButton);
    card.append(title, meta, badge, actions, history);
    els.items.append(card);
  }
}

async function showHistory(itemId, container) {
  try {
    const events = await api(`/items/${itemId}/claims`);
    container.replaceChildren();
    if (!events.length) {
      const empty = document.createElement("p");
      empty.textContent = "No claim events yet.";
      container.append(empty);
      return;
    }
    const list = document.createElement("ul");
    for (const event of events) {
      const entry = document.createElement("li");
      const when = event.created_at
        ? new Date(event.created_at).toLocaleString()
        : "";
      entry.textContent = `${event.display_name}: ${
        event.delta > 0 ? "+" : ""
      }${event.delta}${when ? ` · ${when}` : ""}`;
      list.append(entry);
    }
    container.append(list);
  } catch (error) {
    container.textContent = describe(error);
  }
}

async function loadItems(successMessage) {
  if (!state.token) {
    showAuth("Log in first.");
    return;
  }
  const listId = currentListId();
  if (!listId) {
    setStatus(els.itemsStatus, "Create a list or enter a list ID first.", "warn");
    return;
  }
  try {
    const items = await api(`/lists/${listId}/items`);
    renderItems(items);
    setStatus(
      els.itemsStatus,
      successMessage || `Loaded ${items.length} item(s).`,
      successMessage ? "ok" : "",
    );
  } catch (error) {
    setStatus(els.itemsStatus, describe(error), "warn");
  }
}

async function claimItem(itemId) {
  try {
    await api(`/items/${itemId}/claims`, {
      method: "POST",
      body: { quantity: 1 },
    });
    await loadItems(`Claimed item ${itemId}.`);
  } catch (error) {
    if (error.status === 409) {
      await loadItems("Conflict: someone else claimed the last unit.");
    } else {
      setStatus(els.itemsStatus, describe(error), "warn");
    }
  }
}

async function releaseItem(itemId) {
  try {
    await api(`/items/${itemId}/releases`, {
      method: "POST",
      body: { quantity: 1 },
    });
    await loadItems(`Released item ${itemId}.`);
  } catch (error) {
    if (error.status === 409) {
      await loadItems("You have no claim to release.");
    } else {
      setStatus(els.itemsStatus, describe(error), "warn");
    }
  }
}

function logout() {
  state.token = "";
  save();
  els.items.replaceChildren();
  showAuth("Signed out.");
}

if (state.listId) {
  els.listId.value = String(state.listId);
}
if (state.token) {
  showApp("Signed in. Create or load a list.");
} else {
  showAuth("Sign in to continue.");
}

els.register.addEventListener("click", register);
els.login.addEventListener("click", login);
els.logout.addEventListener("click", logout);
els.createList.addEventListener("click", createList);
els.loadItems.addEventListener("click", () => loadItems());
els.addItem.addEventListener("click", addItem);
