# k6 Performance Tests

Docker Compose의 `perf` profile로 k6 컨테이너를 실행해 REST API 기준 EDA 흐름을 부하 테스트합니다.

## 실행

전체 스택을 먼저 올립니다.

```powershell
docker compose up -d --build
```

k6 시나리오를 실행합니다.

```powershell
docker compose --profile perf run --rm k6
```

## 시나리오

`rental-flow.js`는 각 iteration마다 고유한 회원과 도서를 만들고 다음 흐름을 검증합니다.

1. 회원 등록
2. 도서 등록
3. 대여카드 생성
4. 도서 대여 요청
5. Kafka consumer 처리 후 도서 상태, 회원 포인트, 인기 도서 read model 확인
6. 도서 반납 요청
7. Kafka consumer 처리 후 도서 상태와 회원 포인트 확인

## 설정

환경 변수로 부하와 비동기 대기 시간을 조정할 수 있습니다.

```powershell
$env:K6_TARGET_VUS="50"
$env:K6_RAMP_UP="1m"
$env:K6_STEADY_DURATION="3m"
$env:K6_RAMP_DOWN="1m"
$env:ASYNC_TIMEOUT_SECONDS="15"
$env:SERVICE_READY_TIMEOUT_SECONDS="180"
docker compose --profile perf run --rm k6
```

인기 도서 read model 조회는 데이터가 많아지면 비용이 커질 수 있습니다. 순수 대여/반납 부하만 보고 싶을 때는 끌 수 있습니다.

```powershell
$env:VERIFY_BESTBOOK="false"
docker compose --profile perf run --rm k6
```

## 관측

k6 결과와 함께 Grafana에서 Kafka consumer lag, Spring Boot HTTP/JVM metric, 서비스 로그를 같이 확인합니다.

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Kafka Exporter: `http://localhost:9308/metrics`
