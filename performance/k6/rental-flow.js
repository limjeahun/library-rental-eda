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
const memberSamples = buildMemberSamples();
const bookSamples = buildBookSamples();
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
  const scenarioData = createScenarioData();
  const userId = scenarioData.userId;
  const userName = scenarioData.userName;
  const itemTitle = scenarioData.itemTitle;

  const memberResponse = postJson(`${MEMBER_URL}/api/Member/`, {
    id: userId,
    name: userName,
    passWord: '1111',
    email: scenarioData.email,
  }, 201, 'member create');

  check(memberResponse, {
    'created member has zero point': (response) => response.json('data.point') === 0,
  });

  const bookResponse = postJson(`${BOOK_URL}/api/book`, {
    title: itemTitle,
    description: scenarioData.description,
    author: scenarioData.author,
    isbn: scenarioData.isbn,
    publicationDate: scenarioData.publicationDate,
    source: 'SUPPLY',
    classification: scenarioData.classification,
    location: scenarioData.location,
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

function buildMemberSamples() {
  const familyNames = [
    { roman: 'kim', name: '김' },
    { roman: 'lee', name: '이' },
    { roman: 'park', name: '박' },
    { roman: 'choi', name: '최' },
    { roman: 'jung', name: '정' },
    { roman: 'kang', name: '강' },
    { roman: 'yoon', name: '윤' },
    { roman: 'jang', name: '장' },
    { roman: 'lim', name: '임' },
    { roman: 'han', name: '한' },
    { roman: 'oh', name: '오' },
    { roman: 'shin', name: '신' },
    { roman: 'seo', name: '서' },
    { roman: 'moon', name: '문' },
    { roman: 'baek', name: '백' },
    { roman: 'cho', name: '조' },
    { roman: 'kwon', name: '권' },
    { roman: 'hwang', name: '황' },
    { roman: 'ahn', name: '안' },
    { roman: 'song', name: '송' },
    { roman: 'ryu', name: '류' },
    { roman: 'hong', name: '홍' },
    { roman: 'jeon', name: '전' },
    { roman: 'ko', name: '고' },
    { roman: 'nam', name: '남' },
  ];
  const givenNames = [
    { roman: 'minjun', name: '민준' },
    { roman: 'seoyeon', name: '서연' },
    { roman: 'jiho', name: '지호' },
    { roman: 'hayoon', name: '하윤' },
    { roman: 'junseo', name: '준서' },
    { roman: 'jiwoo', name: '지우' },
    { roman: 'doyoon', name: '도윤' },
    { roman: 'sua', name: '수아' },
    { roman: 'seojun', name: '서준' },
    { roman: 'chaewon', name: '채원' },
    { roman: 'yuna', name: '유나' },
    { roman: 'hyunwoo', name: '현우' },
    { roman: 'jiwon', name: '지원' },
    { roman: 'eunwoo', name: '은우' },
    { roman: 'sihyun', name: '시현' },
    { roman: 'yeeun', name: '예은' },
    { roman: 'dahyun', name: '다현' },
    { roman: 'jimin', name: '지민' },
    { roman: 'seoha', name: '서하' },
    { roman: 'harin', name: '하린' },
    { roman: 'taeyang', name: '태양' },
    { roman: 'gaeun', name: '가은' },
    { roman: 'siwoo', name: '시우' },
    { roman: 'yejun', name: '예준' },
    { roman: 'arin', name: '아린' },
    { roman: 'doyoung', name: '도영' },
    { roman: 'minseo', name: '민서' },
    { roman: 'joowon', name: '주원' },
    { roman: 'seoah', name: '서아' },
    { roman: 'eunseo', name: '은서' },
    { roman: 'taeho', name: '태호' },
    { roman: 'yujin', name: '유진' },
    { roman: 'hajun', name: '하준' },
    { roman: 'soyeon', name: '소연' },
    { roman: 'yeonwoo', name: '연우' },
    { roman: 'nayeon', name: '나연' },
    { roman: 'dabin', name: '다빈' },
    { roman: 'seunghyun', name: '승현' },
    { roman: 'hyeri', name: '혜리' },
    { roman: 'minjae', name: '민재' },
    { roman: 'hyejin', name: '혜진' },
    { roman: 'sumin', name: '수민' },
    { roman: 'geonwoo', name: '건우' },
    { roman: 'seungmin', name: '승민' },
    { roman: 'yebin', name: '예빈' },
    { roman: 'jaeyoon', name: '재윤' },
    { roman: 'hyeonseo', name: '현서' },
    { roman: 'junwoo', name: '준우' },
    { roman: 'chaerin', name: '채린' },
    { roman: 'seojin', name: '서진' },
    { roman: 'doha', name: '도하' },
    { roman: 'haneul', name: '하늘' },
    { roman: 'yewon', name: '예원' },
    { roman: 'juhyun', name: '주현' },
    { roman: 'taemin', name: '태민' },
    { roman: 'seungwoo', name: '승우' },
    { roman: 'sora', name: '소라' },
    { roman: 'dahee', name: '다희' },
    { roman: 'hyunseo', name: '현서' },
    { roman: 'jaeho', name: '재호' },
    { roman: 'yiseo', name: '이서' },
    { roman: 'sion', name: '시온' },
    { roman: 'jiyeon', name: '지연' },
    { roman: 'eunchae', name: '은채' },
    { roman: 'woojin', name: '우진' },
    { roman: 'seoyul', name: '서율' },
    { roman: 'jiyul', name: '지율' },
    { roman: 'haeun', name: '하은' },
    { roman: 'raon', name: '라온' },
    { roman: 'taejoon', name: '태준' },
    { roman: 'yul', name: '율' },
    { roman: 'sarang', name: '사랑' },
    { roman: 'eunsol', name: '은솔' },
    { roman: 'jisu', name: '지수' },
    { roman: 'yeri', name: '예리' },
    { roman: 'minho', name: '민호' },
    { roman: 'sebin', name: '세빈' },
    { roman: 'hyemin', name: '혜민' },
    { roman: 'kyungmin', name: '경민' },
    { roman: 'yoonjae', name: '윤재' },
    { roman: 'seungah', name: '승아' },
  ];
  const samples = [];

  for (const givenName of givenNames) {
    for (const familyName of familyNames) {
      samples.push({
        idPrefix: `${givenName.roman}.${familyName.roman}`,
        name: `${familyName.name}${givenName.name}`,
      });
    }
  }

  return samples;
}

function buildBookSamples() {
  const groups = [
    {
      classification: 'COMPUTER',
      authors: ['정서윤', '김도현', '박민재', '오준영', '신가람', '한지우', '강태오', '윤서진'],
      titles: [
        '분산 시스템 설계 노트',
        'Kafka Streams 실전 패턴',
        '도메인 주도 설계 입문',
        '클린 아키텍처 워크북',
        '마이크로서비스 운영 일지',
        '이벤트 기반 아키텍처 실무',
        'Spring Boot 관찰 가능성',
        '데이터베이스 트랜잭션 가이드',
        'Redis 캐시 설계',
        'Kubernetes 배포 전략',
        'Java 성능 튜닝 기록',
        'REST API 설계 원칙',
        '메시지 큐 패턴 모음',
        '테스트 자동화 실전',
        '클라우드 네이티브 운영',
        'SQL 인덱스 설계법',
        '대규모 트래픽 처리 노트',
        '객체지향 리팩터링',
        '서비스 장애 대응 매뉴얼',
        '실전 모니터링 시스템',
        '보안 인증 구현 가이드',
        '컨테이너 네트워크 이해',
        '헥사고날 아키텍처 연습',
        'JVM 메모리 분석',
        '로그 수집 파이프라인',
        'CI/CD 파이프라인 구축',
        '시스템 설계 인터뷰 노트',
        '데이터 모델링 실무',
        '운영자를 위한 Kafka',
        '실용주의 Java 개발',
        '이벤트 소싱 시작하기',
        '장애 없는 배포 전략',
        'API 게이트웨이 패턴',
        'Spring Security 레시피',
        'MongoDB 운영 노트',
        'MariaDB 성능 분석',
        'Prometheus와 Grafana',
        '부하 테스트와 병목 분석',
        '실전 SRE 핸드북',
        '레거시 코드 개선법',
      ],
    },
    {
      classification: 'LITERATURE',
      authors: ['이하늘', '최유진', '한지우', '서하린', '문소라', '강하람', '윤다은', '정아린'],
      titles: [
        '도시의 문장들',
        '밤의 도서관 산책',
        '작은 서점의 계절',
        '여름 끝의 편지',
        '바람이 머무는 골목',
        '오래된 우체통',
        '비 오는 오후의 기록',
        '겨울 강가의 약속',
        '달빛 아래 편지',
        '아침을 기다리는 사람들',
        '소설가의 작은 방',
        '느린 기차와 창문',
        '종이배가 닿는 곳',
        '푸른 지붕의 집',
        '별을 세는 밤',
        '낯선 도시의 일기',
        '어느 봄날의 대화',
        '사라진 지도',
        '정류장에 남은 노래',
        '오후 세 시의 고백',
        '책갈피 속의 바다',
        '고요한 안부',
        '끝나지 않은 산책',
        '빛나는 먼지들',
        '서랍 속의 계절',
        '강 건너 편의 집',
        '이름 없는 편지',
        '꽃이 피는 골목',
        '어제의 온도',
        '바다로 가는 길',
        '가을의 문장 수집',
        '오후의 그림자',
        '눈 내리는 서점',
        '오래된 약속들',
        '멀리서 온 엽서',
        '새벽의 문장',
        '여름비의 기억',
        '흰 벽의 방',
        '작은 불빛 하나',
        '끝의 시작',
      ],
    },
    {
      classification: 'ARTS',
      authors: ['윤서진', '강태오', '문소라', '백유림', '오지안', '신해원', '장서우', '김라온'],
      titles: [
        '현대 회화의 이해',
        '건축과 빛의 기록',
        '음악으로 읽는 하루',
        '사진가의 시선',
        '색채와 감정의 지도',
        '도시 공간의 미학',
        '영화 장면 분석',
        '디자인 스튜디오 노트',
        '조각의 시간',
        '무대 위의 언어',
        '재즈를 듣는 밤',
        '미술관 산책법',
        '일상의 타이포그래피',
        '공예의 손길',
        '클래식 음악 입문',
        '포스터 디자인 역사',
        '한국화의 오늘',
        '건축 드로잉 연습',
        '카메라와 빛',
        '그림을 읽는 법',
        '공연 예술 노트',
        '컬러 팔레트 수업',
        '생활 속 디자인',
        '영화 음악의 장면들',
        '이미지와 서사',
        '현대 사진 비평',
        '공간을 그리는 사람들',
        '소리의 풍경',
        '시각 문화 읽기',
        '전시 기획 입문',
        '손으로 만드는 하루',
        '도시 건축 스케치',
        '무용과 몸의 언어',
        '그림자의 조형',
        '아트북 제작 노트',
        '창작자를 위한 색감',
        '박물관의 밤',
        '미디어 아트 입문',
        '캘리그래피 연습장',
        '아름다움의 기준',
      ],
    },
  ];
  const samples = [];

  for (const group of groups) {
    for (let index = 0; index < group.titles.length; index += 1) {
      samples.push({
        title: group.titles[index],
        author: group.authors[index % group.authors.length],
        classification: group.classification,
      });
    }
  }

  return samples;
}

function createScenarioData() {
  const seed = exec.vu.idInTest * 100000 + exec.scenario.iterationInTest;
  const suffix = uniqueId();
  const member = pick(memberSamples, seed);
  const book = pick(bookSamples, seed + 3);

  return {
    userId: `${member.idPrefix}.${suffix}`,
    userName: member.name,
    email: `${member.idPrefix}.${suffix}@library.test`,
    itemTitle: `${book.title} ${shortCode(suffix)}`,
    description: `${book.title} 소장용 테스트 도서`,
    author: book.author,
    isbn: `979-${numericCode(seed, 10)}`,
    publicationDate: publicationDate(seed),
    classification: book.classification,
    location: seed % 2 === 0 ? 'JEONGJA' : 'PANGYO',
  };
}

function pick(values, seed) {
  return values[Math.abs(seed) % values.length];
}

function shortCode(value) {
  return value.replace(/[^a-zA-Z0-9]/g, '').slice(-8).toUpperCase();
}

function numericCode(seed, length) {
  let value = String(Math.abs(seed * 2654435761)).replace(/\D/g, '');
  while (value.length < length) {
    value += value;
  }
  return value.slice(0, length);
}

function publicationDate(seed) {
  const year = 2018 + (Math.abs(seed) % 8);
  const month = String((Math.abs(seed) % 12) + 1).padStart(2, '0');
  const day = String((Math.abs(seed) % 28) + 1).padStart(2, '0');
  return `${year}-${month}-${day}`;
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
