import http from "k6/http";
import ws from "k6/ws";
import { check, sleep, group } from "k6";
import { Rate, Trend, Counter } from "k6/metrics";

/**
 * Frict — k6 load test
 * -------------------------------------------------------------------------
 * Drives the public API the same way the Angular SPA does. Authentication is
 * 100% cookie-based (HttpOnly `AuthToken` set by POST /auth/login); k6 keeps a
 * per-VU cookie jar and replays it automatically on later HTTP requests, so we
 * never read tokens from the body or send `Authorization` headers. The
 * WebSocket handshake is the only place we have to inject the cookie by hand.
 *
 * The test is split into traffic-shaped scenarios so the summary tells you
 * which kind of usage is hurting and which endpoint is the bottleneck.
 *
 * This test MUTATES data (product views, WS presence, optional writes), so it
 * runs ONLY against the disposable load-test clone ("loadtest.<domain>") or
 * localhost — NEVER production. A hard guard in setup() aborts otherwise.
 * BASE_URL defaults to localhost; CI passes https://loadtest.<domain>.
 *
 * Run examples:
 *   k6 run infra/k6/load-test.js                       # defaults to http://localhost:8080
 *   k6 run -e BASE_URL=https://loadtest.example.com -e PROFILE=smoke infra/k6/load-test.js
 *   k6 run -e PROFILE=scale infra/k6/load-test.js      # provoke a measured ECS scale-out
 *   k6 run -e STAFF_USER=manager -e STAFF_PASS=managerpass infra/k6/load-test.js
 *   k6 run -e INCLUDE_WRITES=true infra/k6/load-test.js  # add reversible writes
 */

// ---------------------------------------------------------------------------
// Configuration (all overridable via -e ENV=value)
// ---------------------------------------------------------------------------
// No hardcoded domain: defaults to localhost for ad-hoc local runs; CI always
// passes BASE_URL=https://loadtest.<domain> (derived from the prod apex secret).
const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
const WS_URL = BASE_URL.replace(/^https/, "wss").replace(/^http/, "ws");
const API = `${BASE_URL}/api/v1`;

// Client (USER role). Prod seeds user/pass; override via secrets in CI.
const CLIENT_USER = __ENV.TEST_USER || "user";
const CLIENT_PASS = __ENV.TEST_PASS || "pass";

// Staff (MANAGER/ADMIN). Opt-in: dashboards are heavy, only run when provided.
const STAFF_USER = __ENV.STAFF_USER || "";
const STAFF_PASS = __ENV.STAFF_PASS || "";

// Reversible writes only (favourite add/remove). Never enable against prod
// unless you accept mutating the seeded user's state.
const INCLUDE_WRITES = (__ENV.INCLUDE_WRITES || "false") === "true";

const PROFILE = __ENV.PROFILE || "load";
const AUTH_TOKEN_COOKIE = "AuthToken";

// Load shapes for an AUTO-SCALING target (ECS Fargate, 2–8 tasks, CPU target
// tracking). The goal is NOT to saturate — the platform scales out before that —
// but to (a) verify SLOs at baseline and (b) provoke a *measured* scale-out and
// confirm the new tasks absorb the load and latency recovers, without running a
// large fleet for long (cost control). Anonymous traffic is the bulk; auth/WS
// ride on top at a fraction of the volume.
const PROFILES = {
  // Sanity check: does the script + environment respond at all.
  smoke: [{ duration: "30s", target: 5 }],
  // Expected steady load — should be served by the minimum capacity (2 tasks).
  load: [
    { duration: "1m", target: 30 },
    { duration: "3m", target: 30 },
    { duration: "1m", target: 0 },
  ],
  // Measured scale-out: climb past the CPU target so ECS adds tasks, hold long
  // enough for them to start + register + absorb load, then ramp down. Modest
  // ceiling on purpose — tune `target` up if a scale-out isn't triggered.
  scale: [
    { duration: "2m", target: 40 },
    { duration: "3m", target: 100 },
    { duration: "4m", target: 100 },
    { duration: "2m", target: 0 },
  ],
  // Endurance at steady load (leaks / drift over time), kept short to save cost.
  soak: [
    { duration: "2m", target: 30 },
    { duration: "18m", target: 30 },
    { duration: "2m", target: 0 },
  ],
};
const anonStages = PROFILES[PROFILE] || PROFILES.load;
// Authenticated traffic at ~30% of anonymous volume.
const authStages = anonStages.map((s) => ({
  duration: s.duration,
  target: Math.max(1, Math.round(s.target * 0.3)),
}));

// ---------------------------------------------------------------------------
// Custom metrics — these are what you read in the summary / dashboards
// ---------------------------------------------------------------------------
const errorRate = new Rate("errors"); // app-level failures (bad status / shape)
const loginDuration = new Trend("login_duration", true);
const catalogDuration = new Trend("flow_catalog_duration", true);
const productPageDuration = new Trend("flow_product_page_duration", true);
const searchDuration = new Trend("flow_search_duration", true);
const wsConnectDuration = new Trend("ws_connect_duration", true);
const wsSessionMessages = new Counter("ws_messages_received");
const loginFailures = new Counter("login_failures");
const writesPerformed = new Counter("writes_performed");

// ---------------------------------------------------------------------------
// Options: scenarios are assembled conditionally so the test adapts to the
// credentials / flags you pass in.
// ---------------------------------------------------------------------------
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
  // WebSocket presence: connections are expensive, hold a small constant pool.
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
  // Include p(99) so the end-of-test report and thresholds can show it.
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
  thresholds: {
    // Hard SLOs — the run is marked failed if any of these break.
    http_req_failed: ["rate<0.01"], // <1% transport/HTTP errors
    errors: ["rate<0.05"], // <5% app-level errors
    checks: ["rate>0.99"], // >99% assertions pass
    http_req_duration: ["p(95)<800", "p(99)<2000"],
    // Per-scenario latency budgets (read public traffic must stay snappy).
    "http_req_duration{scenario:anonymous}": ["p(95)<600"],
    "http_req_duration{scenario:client}": ["p(95)<1000"],
    login_duration: ["p(95)<1200"],
    flow_catalog_duration: ["p(95)<1500"],
    flow_product_page_duration: ["p(95)<1500"],
    ws_connect_duration: ["p(95)<1500"],
  },
};

// ---------------------------------------------------------------------------
// Setup: bootstrap real category/product IDs once, share with every VU.
// ---------------------------------------------------------------------------
export function setup() {
  // Hard guard (allowlist): this test mutates data, so it may ONLY hit the
  // disposable loadtest clone ("loadtest.<domain>") or localhost. Anything else —
  // notably any production host — is refused. No hardcoded prod domain needed.
  const host = BASE_URL.replace(/^https?:\/\//, "").split("/")[0].split(":")[0].toLowerCase();
  const allowed = host === "localhost" || host === "127.0.0.1" || host.indexOf("loadtest.") === 0;
  if (!allowed) {
    throw new Error(
      `Refusing host '${host}'. This test mutates data (product views, WS presence) ` +
        `and only runs against the loadtest clone ('loadtest.<domain>') or localhost. ` +
        `Set BASE_URL accordingly.`
    );
  }

  const categoryIds = [];
  const productIds = [];

  const cats = http.get(`${API}/categories/list`, {
    tags: { name: "GET /categories/list" },
  });
  if (cats.status === 200) {
    try {
      JSON.parse(cats.body).forEach((c) => c && c.id && categoryIds.push(c.id));
    } catch (_) {}
  }

  const prods = http.get(`${API}/products/filter?page=0&size=30`, {
    tags: { name: "GET /products/filter" },
  });
  if (prods.status === 200) {
    try {
      const items = JSON.parse(prods.body).items || [];
      items.forEach((p) => p && p.id && productIds.push(p.id));
    } catch (_) {}
  }

  if (productIds.length === 0) {
    console.warn(
      "setup: no products discovered — product-detail flows will be skipped. " +
        "Check BASE_URL and that the catalog is seeded."
    );
  }
  console.log(
    `setup: discovered ${categoryIds.length} categories, ${productIds.length} products at ${BASE_URL}`
  );

  return { categoryIds, productIds };
}

// ---------------------------------------------------------------------------
// Scenario: anonymous storefront browsing (no auth) — the bulk of traffic.
// ---------------------------------------------------------------------------
export function anonymousBrowsing(data) {
  group("home", () => {
    const start = Date.now();
    // What the landing page loads in parallel.
    const responses = http.batch([
      ["GET", `${API}/categories/list`, null, tag("GET /categories/list")],
      [
        "GET",
        `${API}/products/recommendations?page=0&size=8`,
        null,
        tag("GET /products/recommendations"),
      ],
      [
        "GET",
        `${API}/products/filter?page=0&size=12`,
        null,
        tag("GET /products/filter"),
      ],
    ]);
    catalogDuration.add(Date.now() - start);
    responses.forEach((r) => recordOk(r));
  });

  sleep(rnd(1, 3));

  group("search", () => {
    const terms = ["", "pro", "max", "kit", "set", "negro"];
    const term = pick(terms);
    const start = Date.now();
    const res = http.get(
      `${API}/products/filter?query=${encodeURIComponent(term)}&page=0&size=12`,
      tag("GET /products/filter")
    );
    searchDuration.add(Date.now() - start);
    recordOk(res);
  });

  sleep(rnd(1, 2));

  // Category landing (top sales + metrics), only if we have category IDs.
  if (data.categoryIds.length) {
    group("category", () => {
      const cid = pick(data.categoryIds);
      const responses = http.batch([
        [
          "GET",
          `${API}/products/category/${cid}/top-sales?page=0&size=10`,
          null,
          tag("GET /products/category/:id/top-sales"),
        ],
        [
          "GET",
          `${API}/products/category/${cid}/metrics`,
          null,
          tag("GET /products/category/:id/metrics"),
        ],
      ]);
      responses.forEach((r) => recordOk(r));
    });
    sleep(rnd(1, 2));
  }

  // Product detail page (parallel fan-out like the real component).
  if (data.productIds.length) {
    group("product_detail", () => {
      const pid = pick(data.productIds);
      const start = Date.now();
      const responses = http.batch([
        ["GET", `${API}/products/${pid}`, null, tag("GET /products/:id")],
        [
          "GET",
          `${API}/products/stock/${pid}`,
          null,
          tag("GET /products/stock/:id"),
        ],
        [
          "GET",
          `${API}/reviews/product?productId=${pid}&page=0&size=5`,
          null,
          tag("GET /reviews/product"),
        ],
        [
          "GET",
          `${API}/reviews/product/stats?productId=${pid}`,
          null,
          tag("GET /reviews/product/stats"),
        ],
      ]);
      productPageDuration.add(Date.now() - start);
      responses.forEach((r) => recordOk(r));
    });
  }

  sleep(rnd(2, 5)); // think time between sessions
}

// ---------------------------------------------------------------------------
// Scenario: authenticated client journey (USER).
// ---------------------------------------------------------------------------
export function clientJourney(data) {
  if (!login(CLIENT_USER, CLIENT_PASS)) {
    sleep(2);
    return;
  }

  group("client_session", () => {
    recordOk(
      http.get(`${API}/users/session`, tag("GET /users/session")),
      "session"
    );
    recordOk(http.get(`${API}/users/me`, tag("GET /users/me")), "me");
  });

  sleep(rnd(1, 2));

  group("client_browse", () => {
    recordOk(
      http.get(
        `${API}/products/filter?page=0&size=12`,
        tag("GET /products/filter")
      )
    );
    recordOk(
      http.get(
        `${API}/products/favourites?page=0&size=10`,
        tag("GET /products/favourites")
      )
    );
  });

  sleep(rnd(1, 2));

  group("client_cart_orders", () => {
    recordOk(
      http.get(`${API}/orders/cart/summary`, tag("GET /orders/cart/summary"))
    );
    recordOk(
      http.get(`${API}/orders/cart?page=0&size=10`, tag("GET /orders/cart"))
    );
    recordOk(http.get(`${API}/orders?page=0&size=10`, tag("GET /orders")));
  });

  // Optional reversible write: add a product to favourites then remove it.
  if (INCLUDE_WRITES && data.productIds.length) {
    group("client_write_favourite", () => {
      const pid = pick(data.productIds);
      const add = http.post(
        `${API}/products/favourites/${pid}`,
        null,
        tag("POST /products/favourites/:id")
      );
      if (recordOk(add, "fav_add")) writesPerformed.add(1);
      sleep(0.5);
      const del = http.del(
        `${API}/products/favourites/${pid}`,
        null,
        tag("DELETE /products/favourites/:id")
      );
      recordOk(del, "fav_del");
    });
  }

  sleep(rnd(2, 4));
}

// ---------------------------------------------------------------------------
// Scenario: staff dashboard (MANAGER/ADMIN) — read-only, heavier queries.
// ---------------------------------------------------------------------------
export function staffJourney() {
  if (!login(STAFF_USER, STAFF_PASS)) {
    sleep(2);
    return;
  }

  group("staff_dashboard", () => {
    recordOk(http.get(`${API}/users/me`, tag("GET /users/me")), "me");
    recordOk(
      http.get(`${API}/orders/?page=0&size=15`, tag("GET /orders/ (staff)")),
      "orders"
    );
    recordOk(
      http.get(`${API}/stats/orders`, tag("GET /stats/orders")),
      "stats"
    );
  });

  sleep(rnd(3, 6));
}

// ---------------------------------------------------------------------------
// Scenario: WebSocket presence + notifications.
// ---------------------------------------------------------------------------
export function wsSession() {
  if (!login(CLIENT_USER, CLIENT_PASS)) {
    sleep(2);
    return;
  }

  // Cookie jar is per-VU; pull the AuthToken out for the WS handshake header.
  const jar = http.cookieJar();
  const cookies = jar.cookiesForURL(BASE_URL);
  const tokenArr = cookies[AUTH_TOKEN_COOKIE];
  if (!tokenArr || !tokenArr.length) {
    errorRate.add(1);
    return;
  }
  const token = tokenArr[0];

  const start = Date.now();
  const res = ws.connect(
    `${WS_URL}/api/v1/ws/notifications`,
    { headers: { Cookie: `${AUTH_TOKEN_COOKIE}=${token}` } },
    function (socket) {
      socket.on("open", () => {
        wsConnectDuration.add(Date.now() - start);
      });
      socket.on("message", () => {
        wsSessionMessages.add(1);
      });
      // Hold the connection open like a real browser tab (10s).
      socket.setTimeout(() => socket.close(), 10000);
    }
  );

  check(res, { "ws handshake 101": (r) => r && r.status === 101 }) ||
    errorRate.add(1);
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function login(username, password) {
  const start = Date.now();
  const res = http.post(
    `${API}/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { "Content-Type": "application/json" }, ...tag("POST /auth/login") }
  );
  loginDuration.add(Date.now() - start);

  // Success = 200 AND the AuthToken cookie was set by Set-Cookie.
  const ok = check(res, {
    "login status 200": (r) => r.status === 200,
    "login set AuthToken cookie": (r) =>
      r.cookies && r.cookies[AUTH_TOKEN_COOKIE] && r.cookies[AUTH_TOKEN_COOKIE].length > 0,
  });

  if (!ok) {
    errorRate.add(1);
    loginFailures.add(1);
    return false;
  }
  errorRate.add(0);
  return true;
}

// Build a params object carrying a low-cardinality `name` tag so k6 aggregates
// per-endpoint metrics instead of per-URL (IDs would explode the cardinality).
function tag(name) {
  return { tags: { name } };
}

// Assert a response is OK (2xx/3xx) and feed the error rate. The check name is
// stable; the per-endpoint breakdown comes from the request `name` tag.
function recordOk(res, label) {
  const ok = check(
    res,
    { "status < 400": (r) => r.status > 0 && r.status < 400 },
    label ? { step: label } : undefined
  );
  errorRate.add(!ok);
  return ok;
}

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function rnd(min, max) {
  return Math.random() * (max - min) + min;
}

// Sum k6 stage durations ("30s", "2m", "1m30s") into a single duration string.
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
