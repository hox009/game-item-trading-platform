// k6 load test for the Game Item Trading Platform.
// Simulates the core trading path through the gateway to measure throughput
// and business error rates.
//
// Usage:
//   1. Bring up the stack with Docker Compose and seed a user + items.
//   2. Install k6: https://k6.io/docs/get-started/installation/
//   3. Run:  k6 run scripts/loadtest/order-flow.js
//
// Tune GATEWAY, VUS and DURATION via environment variables:
//   k6 run -e GATEWAY=http://localhost:8080 -e VUS=200 -e DURATION=1m scripts/loadtest/order-flow.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('business_errors');

const GATEWAY = __ENV.GATEWAY || 'http://localhost:8080';
const VUS = parseInt(__ENV.VUS || '200', 10);
const DURATION = __ENV.DURATION || '1m';

export const options = {
  scenarios: {
    trading: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: VUS },   // ramp up
        { duration: DURATION, target: VUS }, // sustain peak load
        { duration: '10s', target: 0 },      // ramp down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(99)<200'],   // P99 latency < 200ms
    http_req_failed: ['rate<0.001'],    // Fewer than 0.1% failed requests
    business_errors: ['rate<0.01'],
  },
};

// Log in once per VU to obtain a JWT, then reuse it.
function login() {
  const res = http.post(`${GATEWAY}/api/users/login`, JSON.stringify({
    username: __ENV.USERNAME || 'loadtest',
    password: __ENV.PASSWORD || 'secret123',
  }), { headers: { 'Content-Type': 'application/json' } });
  const body = res.json();
  return body && body.data ? body.data.token : null;
}

export function setup() {
  const token = login();
  return { token };
}

export default function (data) {
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // 1. Browse the catalog (read-heavy, hits Redis cache).
  const browse = http.get(`${GATEWAY}/api/items?page=0&size=20`);
  check(browse, { 'browse 200': (r) => r.status === 200 });

  // 2. View a specific item detail.
  const detail = http.get(`${GATEWAY}/api/items/1`);
  check(detail, { 'detail ok': (r) => r.status === 200 });

  // 3. Place an order (write path: freeze stock via Kafka-backed flow).
  const order = http.post(`${GATEWAY}/api/orders`, JSON.stringify({
    itemId: 1, skuId: 1, quantity: 1,
  }), { headers: authHeaders });
  const ok = check(order, { 'order accepted': (r) => r.status === 200 });
  errorRate.add(!ok);

  sleep(1);
}
