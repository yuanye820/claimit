const STORAGE = {
  token: "claimit.token",
  apiBase: "claimit.apiBase",
  listId: "claimit.listId",
};

const state = {
  token: localStorage.getItem(STORAGE.token) || "",
  apiBase: localStorage.getItem(STORAGE.apiBase) || "http://127.0.0.1:8000",
  listId: Number(localStorage.getItem(STORAGE.listId)) || null,
};

const els = {
  apiBase: document.getElementById("api-base"),
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
  localStorage.setItem(STORAGE.apiBase, state.apiBase);
  localStorage.setItem(STORAGE.listId, state.listId ? String(state.listId) : "");
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

function syncApiBase() {
  const value = els.apiBase.value.trim() || "http://127.0.0.1:8000";
  state.apiBase = value;
  localStorage.setItem(STORAGE.apiBase, value);
}

async function register() {
  syncApiBase();
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
    setStatus(els.authStatus, "Registered and signed in.", "ok");
  } catch (error) {
    setStatus(els.authStatus, describe(error), "warn");
  }
}

async function login() {
  syncApiBase();
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
    setStatus(els.authStatus, "Signed in.", "ok");
  } catch (error) {
    setStatus(els.authStatus, describe(error), "warn");
  }
}

async function createList() {
  syncApiBase();
  if (!state.token) {
    setStatus(els.itemsStatus, "Log in first.", "warn");
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
  syncApiBase();
  if (!state.token) {
    setStatus(els.itemsStatus, "Log in first.", "warn");
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

    actions.append(claim, release);
    card.append(title, meta, badge, actions);
    els.items.append(card);
  }
}

async function loadItems(successMessage) {
  syncApiBase();
  if (!state.token) {
    setStatus(els.itemsStatus, "Log in first.", "warn");
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
  setStatus(els.authStatus, "Signed out.");
}

els.apiBase.value = state.apiBase;
if (state.listId) {
  els.listId.value = String(state.listId);
}
if (state.token) {
  setStatus(els.authStatus, "Signed in.");
}

els.register.addEventListener("click", register);
els.login.addEventListener("click", login);
els.logout.addEventListener("click", logout);
els.createList.addEventListener("click", createList);
els.loadItems.addEventListener("click", () => loadItems());
els.addItem.addEventListener("click", addItem);
