import http from "k6/http";
import ws from "k6/ws";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "https://app.example.com";
const WS_URL = BASE_URL.replace("https://", "wss://").replace("http://", "ws://");
const TEST_USER = __ENV.TEST_USER || "loadtest";
const TEST_PASS = __ENV.TEST_PASS || "loadtest123";

const errorRate = new Rate("errors");
const loginDuration = new Trend("login_duration", true);

export const options = {
  stages: [
    { duration: "30s", target: 20 },
    { duration: "2m", target: 100 },
    { duration: "2m", target: 100 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<2000"],
    errors: ["rate<0.1"],
  },
};

function login() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: TEST_USER, password: TEST_PASS }),
    { headers: { "Content-Type": "application/json" } }
  );

  loginDuration.add(res.timings.duration);

  const ok = check(res, {
    "login status 200": (r) => r.status === 200,
    "login has token": (r) => {
      try {
        return JSON.parse(r.body).token !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (!ok) {
    errorRate.add(1);
    return null;
  }

  errorRate.add(0);
  return JSON.parse(res.body).token;
}

function browseProducts(token) {
  const headers = { Authorization: `Bearer ${token}` };

  const products = http.get(`${BASE_URL}/api/v1/products/?page=0&size=10`, {
    headers,
  });

  check(products, {
    "products status 200": (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(0.5);

  const specs = http.get(`${BASE_URL}/api/v1/products/specs`, { headers });
  check(specs, {
    "specs status 200": (r) => r.status === 200,
  }) || errorRate.add(1);

  sleep(0.5);

  const recommendations = http.get(
    `${BASE_URL}/api/v1/products/recommendations?page=0&size=8`,
    { headers }
  );
  check(recommendations, {
    "recommendations status 200": (r) => r.status === 200,
  }) || errorRate.add(1);
}

function browseTrucks(token) {
  const headers = { Authorization: `Bearer ${token}` };

  const trucks = http.get(`${BASE_URL}/api/v1/trucks/?page=0&size=10`, {
    headers,
  });
  check(trucks, {
    "trucks status 200": (r) => r.status === 200,
  }) || errorRate.add(1);
}

function openWebSocket(token) {
  const url = `${WS_URL}/api/v1/ws/notifications`;
  const res = ws.connect(url, { headers: { Cookie: `token=${token}` } }, function (socket) {
    socket.on("open", () => {});
    socket.on("message", () => {});
    socket.setTimeout(() => socket.close(), 5000);
  });

  check(res, {
    "ws status 101": (r) => r && r.status === 101,
  }) || errorRate.add(1);
}

export default function () {
  const token = login();
  if (!token) {
    sleep(1);
    return;
  }

  browseProducts(token);
  sleep(1);

  browseTrucks(token);
  sleep(1);

  openWebSocket(token);
  sleep(1);
}
