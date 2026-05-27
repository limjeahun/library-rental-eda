import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';

const MEMBER_URL = __ENV.MEMBER_URL || 'http://member-service:8082';
const BOOK_URL = __ENV.BOOK_URL || 'http://book-service:8081';
const RENTAL_URL = __ENV.RENTAL_URL || 'http://rental-service:8080';
const BESTBOOK_URL = __ENV.BESTBOOK_URL || 'http://bestbook-service:8084';

const TARGET_VUS = Number(__ENV.K6_TARGET_VUS || 20);
const RAMP_UP = __ENV.K6_RAMP_UP || '30s';
const STEADY_DURATION = __ENV.K6_STEADY_DURATION || '1m';
const RAMP_DOWN = __ENV.K6_RAMP_DOWN || '30s';
const ASYNC_TIMEOUT_SECONDS = Number(__ENV.ASYNC_TIMEOUT_SECONDS || 10);
const POLL_INTERVAL_SECONDS = Number(__ENV.POLL_INTERVAL_SECONDS || 1);
const SERVICE_READY_TIMEOUT_SECONDS = Number(__ENV.SERVICE_READY_TIMEOUT_SECONDS || 120);
const VERIFY_BESTBOOK = (__ENV.VERIFY_BESTBOOK || 'true').toLowerCase() === 'true';

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const serviceReadiness = [
  ['member-service', `${MEMBER_URL}/actuator/health`],
  ['book-service', `${BOOK_URL}/actuator/health`],
  ['rental-service', `${RENTAL_URL}/actuator/health`],
  ['bestbook-service', `${BESTBOOK_URL}/actuator/health`],
];

export const eventualConsistencySuccess = new Rate('eventual_consistency_success');

export const options = {
  scenarios: {
    rental_flow: {
      executor: 'ramping-vus',
      stages: [
        { duration: RAMP_UP, target: TARGET_VUS },
        { duration: STEADY_DURATION, target: TARGET_VUS },
        { duration: RAMP_DOWN, target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1500'],
    checks: ['rate>0.95'],
    eventual_consistency_success: ['rate>0.95'],
  },
};

export function setup() {
  serviceReadiness.forEach(([name, url]) => waitForService(name, url));
}

export default function () {
  const userId = uniqueId();
  const userName = `k6-user-${userId}`;
  const itemTitle = `k6-book-${userId}`;

  const memberResponse = postJson(`${MEMBER_URL}/api/Member/`, {
    id: userId,
    name: userName,
    passWord: '1111',
    email: `${userId}@example.com`,
  }, 201, 'member create');

  check(memberResponse, {
    'created member has zero point': (response) => response.json('data.point') === 0,
  });

  const bookResponse = postJson(`${BOOK_URL}/api/book`, {
    title: itemTitle,
    description: 'k6 performance test book',
    author: 'k6',
    isbn: `isbn-${userId}`,
    publicationDate: '2026-05-28',
    source: 'SUPPLY',
    classification: 'LITERATURE',
    location: 'JEONGJA',
  }, 201, 'book create');
  const itemId = bookResponse.json('data.no');

  if (!itemId) {
    fail(`book create response did not include data.no: ${bookResponse.body}`);
  }

  postJson(`${RENTAL_URL}/api/rental-cards`, {
    userId,
    userNm: userName,
  }, 200, 'rental card create');

  postJson(`${RENTAL_URL}/api/rental-cards/rent`, {
    itemId,
    itemTitle,
    userId,
    userNm: userName,
  }, 202, 'rent item');

  eventually('book unavailable after rent', () => {
    const response = http.get(`${BOOK_URL}/api/book/${itemId}`, { tags: { name: 'book get by id' } });
    return response.status === 200 && response.json('data.bookStatus') === 'UNAVAILABLE';
  });

  eventually('member point saved after rent', () => {
    const response = http.get(`${MEMBER_URL}/api/Member/by-id/${userId}`, { tags: { name: 'member get by id' } });
    return response.status === 200 && response.json('data.point') >= 10;
  });

  if (VERIFY_BESTBOOK) {
    eventually('bestbook read model updated after rent', () => {
      const response = http.get(`${BESTBOOK_URL}/api/books`, { tags: { name: 'bestbook list' } });
      if (response.status !== 200) {
        return false;
      }

      const books = response.json('data') || [];
      return books.some((book) => book.itemNo === itemId && book.rentCount >= 1);
    });
  }

  postJson(`${RENTAL_URL}/api/rental-cards/return`, {
    itemId,
    itemTitle,
    userId,
    userNm: userName,
  }, 202, 'return item');

  eventually('book available after return', () => {
    const response = http.get(`${BOOK_URL}/api/book/${itemId}`, { tags: { name: 'book get by id' } });
    return response.status === 200 && response.json('data.bookStatus') === 'AVAILABLE';
  });

  eventually('member point saved after return', () => {
    const response = http.get(`${MEMBER_URL}/api/Member/by-id/${userId}`, { tags: { name: 'member get by id' } });
    return response.status === 200 && response.json('data.point') >= 20;
  });

  sleep(1);
}

function uniqueId() {
  return `k6-${Date.now()}-${exec.vu.idInTest}-${exec.scenario.iterationInTest}`;
}

function postJson(url, payload, expectedStatus, requestName) {
  const response = http.post(url, JSON.stringify(payload), {
    headers: JSON_HEADERS,
    tags: { name: requestName },
  });

  check(response, {
    [`${requestName} status ${expectedStatus}`]: (res) => res.status === expectedStatus,
  });

  if (response.status !== expectedStatus) {
    fail(`${requestName} failed. expected=${expectedStatus} actual=${response.status} body=${response.body}`);
  }

  return response;
}

function eventually(name, assertion) {
  const deadline = Date.now() + ASYNC_TIMEOUT_SECONDS * 1000;

  while (Date.now() < deadline) {
    if (safeAssert(assertion)) {
      eventualConsistencySuccess.add(true);
      check(true, { [`${name} eventually`]: (value) => value });
      return;
    }
    sleep(POLL_INTERVAL_SECONDS);
  }

  eventualConsistencySuccess.add(false);
  check(false, { [`${name} eventually`]: (value) => value });
}

function safeAssert(assertion) {
  try {
    return assertion();
  } catch (error) {
    return false;
  }
}

function waitForService(name, healthUrl) {
  const deadline = Date.now() + SERVICE_READY_TIMEOUT_SECONDS * 1000;

  while (Date.now() < deadline) {
    const response = http.get(healthUrl, { tags: { name: `${name} health` } });
    if (response.status === 200 && response.json('status') === 'UP') {
      return;
    }
    sleep(2);
  }

  fail(`${name} did not become healthy through ${healthUrl}`);
}
