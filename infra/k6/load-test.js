import http from "k6/http";
import ws from "k6/ws";
import { check, sleep, group } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";

/**
 * TecHub — k6 load test
 * -------------------------------------------------------------------------
 * Simulates SPA traffic against the API. Cookie-based auth.
 * IMPORTANT: This test MUTATES data. Run ONLY on localhost or loadtest.
 *
 * Examples:
 * k6 run infra/k6/load-test.js
 * k6 run -e BASE_URL=https://loadtest.example.com -e PROFILE=smoke infra/k6/load-test.js
 * k6 run -e PROFILE=scale infra/k6/load-test.js
 */

// --- Config (override via -e ENV=value) ---
const BASE_URL = (__ENV.BASE_URL || "https://localhost:8443").replace(/\/+$/, "");
const WS_URL = BASE_URL.replace(/^https/, "wss").replace(/^http/, "ws");
const API = `${BASE_URL}/api/v1`;

// Credentials
const CLIENT_USER = __ENV.TEST_USER || "user";
const CLIENT_PASS = __ENV.TEST_PASS || "pass";
const STAFF_USER = __ENV.STAFF_USER || "";
const STAFF_PASS = __ENV.STAFF_PASS || "";

// Enable reversible writes
const INCLUDE_WRITES = (__ENV.INCLUDE_WRITES || "false") === "true";
const PROFILE = __ENV.PROFILE || "load";
const AUTH_TOKEN_COOKIE = "AuthToken";

// Load profiles (auto-scaling optimized)
const PROFILES = {
  smoke: [{ duration: "30s", target: 5 }], // Quick sanity check
  load: [ // Baseline load
    { duration: "1m", target: 30 },
    { duration: "3m", target: 30 },
    { duration: "1m", target: 0 },
  ],
  scale: [ // Two-step staircase — provokes two distinct scale-out events
    { duration: "2m", target: 50 },
    { duration: "3m", target: 100 },
    { duration: "3m", target: 150 },
    { duration: "3m", target: 150 },
    { duration: "2m", target: 0 },
  ],
  soak: [ // Endurance test
    { duration: "2m", target: 30 },
    { duration: "18m", target: 30 },
    { duration: "2m", target: 0 },
  ],
};

const anonStages = PROFILES[PROFILE] || PROFILES.load;
const authStages = anonStages.map((s) => ({
  duration: s.duration,
  target: Math.max(1, Math.round(s.target * 0.3)), // Auth traffic: ~30%
}));

// --- Custom metrics ---
const errorRate = new Rate("errors");
const loginDuration = new Trend("login_duration", true);
const catalogDuration = new Trend("flow_catalog_duration", true);
const productPageDuration = new Trend("flow_product_page_duration", true);
const searchDuration = new Trend("flow_search_duration", true);
const wsConnectDuration = new Trend("ws_connect_duration", true);
const wsSessionMessages = new Counter("ws_messages_received");
const loginFailures = new Counter("login_failures");
const writesPerformed = new Counter("writes_performed");

// --- Dynamic scenarios ---
const scenarios = {
  anonymous: {
    executor: "ramping-vus",
    exec: "anonymousBrowsing",
    startVUs: 0,
    stages: anonStages,
    tags: { scenario: "anonymous" },
  },
};

if (CLIENT_USER && CLIENT_PASS) {
  scenarios.client = {
    executor: "ramping-vus",
    exec: "clientJourney",
    startVUs: 0,
    stages: authStages,
    tags: { scenario: "client" },
  };

  // Constant WebSocket pool
  scenarios.websocket = {
    executor: "constant-vus",
    exec: "wsSession",
    vus: PROFILE === "smoke" ? 3 : 15,
    duration: totalDuration(anonStages),
    tags: { scenario: "websocket" },
  };
}

if (STAFF_USER && STAFF_PASS) {
  scenarios.staff = {
    executor: "constant-vus",
    exec: "staffJourney",
    vus: PROFILE === "smoke" ? 2 : 8,
    duration: totalDuration(anonStages),
    tags: { scenario: "staff" },
  };
}

export const options = {
  scenarios,
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
  thresholds: {
    // Hard SLOs — the run is marked failed if any of these break.
    http_req_failed: ["rate<0.01"], // <1% transport/HTTP errors
    errors: ["rate<0.05"], // <5% app-level errors
    checks: ["rate>0.99"], // >99% assertions pass
    http_req_duration: ["p(95)<800", "p(99)<2000"],

    // Latency per scenario
    "http_req_duration{scenario:anonymous}": ["p(95)<600"],
    "http_req_duration{scenario:client}": ["p(95)<1000"],
    login_duration: ["p(95)<1200"],
    flow_catalog_duration: ["p(95)<1500"],
    flow_product_page_duration: ["p(95)<1500"],
    ws_connect_duration: ["p(95)<1500"],
  },
};

// --- Setup: Fetch shared real IDs ---
export function setup() {
  // Safety guard: Localhost or loadtest only
  const host = BASE_URL.replace(/^https?:\/\//, "").split("/")[0].split(":")[0].toLowerCase();
  const allowed = host === "localhost" || host === "127.0.0.1" || host.indexOf("loadtest.") === 0;
  if (!allowed) {
    throw new Error(
        `Refusing host '${host}'. Test mutates data. Set BASE_URL to localhost or loadtest.`
    );
  }

  const categoryIds = [];
  const productIds = [];

  const cats = http.get(`${API}/categories/list`, { tags: { name: "GET /categories/list" } });
  if (cats.status === 200) {
    try { JSON.parse(cats.body).forEach((c) => c && c.id && categoryIds.push(c.id)); } catch (_) {}
  }

  const prods = http.get(`${API}/products/filter?page=0&size=30`, { tags: { name: "GET /products/filter" } });
  if (prods.status === 200) {
    try {
      const items = JSON.parse(prods.body).items || [];
      items.forEach((p) => p && p.id && productIds.push(p.id));
    } catch (_) {}
  }

  if (productIds.length === 0) console.warn("setup: No products found. Detail flows will be skipped.");
  console.log(`setup: ${categoryIds.length} categories, ${productIds.length} products at ${BASE_URL}`);

  return { categoryIds, productIds };
}

// --- Scenario: Anonymous browsing ---
export function anonymousBrowsing(data) {
  group("home", () => {
    const start = Date.now();
    // Parallel landing page load
    const responses = http.batch([
      ["GET", `${API}/categories/list`, null, tag("GET /categories/list")],
      ["GET", `${API}/products/recommendations?page=0&size=8`, null, tag("GET /products/recommendations")],
      ["GET", `${API}/products/filter?page=0&size=12`, null, tag("GET /products/filter")],
    ]);
    catalogDuration.add(Date.now() - start);
    responses.forEach((r) => recordOk(r));
  });

  sleep(rnd(1, 3));

  group("search", () => {
    const terms = ["", "pro", "max", "kit", "set", "negro"];
    const term = pick(terms);
    const start = Date.now();
    const res = http.get(`${API}/products/filter?query=${encodeURIComponent(term)}&page=0&size=12`, tag("GET /products/filter"));
    searchDuration.add(Date.now() - start);
    recordOk(res);
  });

  sleep(rnd(1, 2));

  // Category landing
  if (data.categoryIds.length) {
    group("category", () => {
      const cid = pick(data.categoryIds);
      const responses = http.batch([
        ["GET", `${API}/products/category/${cid}/top-sales?page=0&size=10`, null, tag("GET /products/category/:id/top-sales")],
        ["GET", `${API}/products/category/${cid}/metrics`, null, tag("GET /products/category/:id/metrics")],
      ]);
      responses.forEach((r) => recordOk(r));
    });
    sleep(rnd(1, 2));
  }

  // Product detail
  if (data.productIds.length) {
    group("product_detail", () => {
      const pid = pick(data.productIds);
      const start = Date.now();
      const responses = http.batch([
        ["GET", `${API}/products/${pid}`, null, tag("GET /products/:id")],
        ["GET", `${API}/products/stock/${pid}`, null, tag("GET /products/stock/:id")],
        ["GET", `${API}/reviews/product?productId=${pid}&page=0&size=5`, null, tag("GET /reviews/product")],
        ["GET", `${API}/reviews/product/stats?productId=${pid}`, null, tag("GET /reviews/product/stats")],
      ]);
      productPageDuration.add(Date.now() - start);
      responses.forEach((r) => recordOk(r));
    });
  }

  sleep(rnd(2, 5));
}

// --- Scenario: Authenticated client ---
export function clientJourney(data) {
  if (!login(CLIENT_USER, CLIENT_PASS)) {
    sleep(2);
    return;
  }

  group("client_session", () => {
    recordOk(http.get(`${API}/users/session`, tag("GET /users/session")), "session");
    recordOk(http.get(`${API}/users/me`, tag("GET /users/me")), "me");
  });

  sleep(rnd(1, 2));

  group("client_browse", () => {
    recordOk(http.get(`${API}/products/filter?page=0&size=12`, tag("GET /products/filter")));
    recordOk(http.get(`${API}/products/favourites?page=0&size=10`, tag("GET /products/favourites")));
  });

  sleep(rnd(1, 2));

  group("client_cart_orders", () => {
    recordOk(http.get(`${API}/orders/cart/summary`, tag("GET /orders/cart/summary")));
    recordOk(http.get(`${API}/orders/cart?page=0&size=10`, tag("GET /orders/cart")));
    recordOk(http.get(`${API}/orders?page=0&size=10`, tag("GET /orders")));
  });

  // Optional write: favourites
  if (INCLUDE_WRITES && data.productIds.length) {
    group("client_write_favourite", () => {
      const pid = pick(data.productIds);
      const add = http.post(`${API}/products/favourites/${pid}`, null, tag("POST /products/favourites/:id"));
      if (recordOk(add, "fav_add")) writesPerformed.add(1);
      sleep(0.5);
      const del = http.del(`${API}/products/favourites/${pid}`, null, tag("DELETE /products/favourites/:id"));
      recordOk(del, "fav_del");
    });
  }

  sleep(rnd(2, 4));
}

// --- Scenario: Staff dashboard ---
export function staffJourney() {
  if (!login(STAFF_USER, STAFF_PASS)) {
    sleep(2);
    return;
  }

  group("staff_dashboard", () => {
    recordOk(http.get(`${API}/users/me`, tag("GET /users/me")), "me");
    recordOk(http.get(`${API}/orders/?page=0&size=15`, tag("GET /orders/ (staff)")), "orders");
    recordOk(http.get(`${API}/stats/orders`, tag("GET /stats/orders")), "stats");
  });

  sleep(rnd(3, 6));
}

// --- Scenario: WebSockets ---
export function wsSession() {
  if (!login(CLIENT_USER, CLIENT_PASS)) {
    sleep(2);
    return;
  }

  // Extract AuthToken for handshake
  const jar = http.cookieJar();
  const cookies = jar.cookiesForURL(BASE_URL);
  const tokenArr = cookies[AUTH_TOKEN_COOKIE];
  if (!tokenArr || !tokenArr.length) {
    errorRate.add(1);
    return;
  }

  const start = Date.now();
  const res = ws.connect(
      `${WS_URL}/api/v1/ws/notifications`,
      { headers: { Cookie: `${AUTH_TOKEN_COOKIE}=${tokenArr[0]}` } },
      function (socket) {
        socket.on("open", () => wsConnectDuration.add(Date.now() - start));
        socket.on("message", () => wsSessionMessages.add(1));

        // Keep connection open for 10s
        socket.setTimeout(() => socket.close(), 10000);
      }
  );

  check(res, { "ws handshake 101": (r) => r && r.status === 101 }) || errorRate.add(1);
}

// --- Helpers ---
function login(username, password) {
  const start = Date.now();
  const res = http.post(`${API}/auth/login`, JSON.stringify({ username, password }), {
    headers: { "Content-Type": "application/json" },
    ...tag("POST /auth/login"),
  });
  loginDuration.add(Date.now() - start);

  // Validate 200 OK and AuthToken cookie
  const ok = check(res, {
    "login status 200": (r) => r.status === 200,
    "login set AuthToken cookie": (r) => r.cookies && r.cookies[AUTH_TOKEN_COOKIE] && r.cookies[AUTH_TOKEN_COOKIE].length > 0,
  });

  if (!ok) {
    errorRate.add(1);
    loginFailures.add(1);
    return false;
  }
  errorRate.add(0);
  return true;
}

// Tag to group metrics by endpoint
function tag(name) {
  return { tags: { name } };
}

// Validate OK response and track errors
function recordOk(res, label) {
  const ok = check(res, { "status < 400": (r) => r.status > 0 && r.status < 400 }, label ? { step: label } : undefined);
  errorRate.add(!ok);
  return ok;
}

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function rnd(min, max) {
  return Math.random() * (max - min) + min;
}

// Sum stage durations
function totalDuration(stages) {
  let seconds = 0;
  for (const s of stages) seconds += parseDuration(s.duration);
  return `${seconds}s`;
}

function parseDuration(d) {
  let total = 0;
  const re = /(\d+)(h|m|s)/g;
  let m;
  while ((m = re.exec(d)) !== null) {
    const n = parseInt(m[1], 10);
    total += m[2] === "h" ? n * 3600 : m[2] === "m" ? n * 60 : n;
  }
  return total || 0;
}