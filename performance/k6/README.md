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

Compose의 k6 서비스는 Prometheus remote write 출력으로 실행됩니다. 테스트가 시작되면 `http://localhost:3000`의 Grafana에서 `도서관 대여 EDA / k6 Performance` 대시보드를 열어 k6 자체 지표를 볼 수 있습니다.
Kafka lag, 비동기 상태 반영 시간, DB connection pressure, JVM/컨테이너 리소스는 `도서관 대여 EDA / Performance Observability` 대시보드에서 함께 확인합니다.

## 시나리오

`rental-flow.js`는 `setup()`에서 도서를 먼저 등록하고, 각 iteration마다 고유한 회원을 만든 뒤 미리 등록된 도서 중 하나를 랜덤하게 대여/반납합니다. 기본 등록 도서는 300권이고 `K6_TARGET_VUS`가 더 크면 등록 도서 수를 VU 수 이상으로 자동 보정합니다. 회원 이름은 2,000명 규모의 한국어 샘플 풀에서 생성합니다.

1. 회원 등록
2. 대여카드 생성
3. 미리 등록된 도서 중 랜덤 도서 선택
4. 도서 대여 요청
5. Kafka consumer 처리 후 도서 상태, 회원 포인트, 인기 도서 read model 확인
6. 도서 반납 요청
7. Kafka consumer 처리 후 도서 상태와 회원 포인트 확인

대여 요청이 접수된 뒤 book-service에서 이미 대여 중인 도서로 판정되면 rental-service가 대여를 보상 취소할 수 있습니다. 이 상태에서 반납 요청이 들어가 `400 / 대여 중인 도서가 아닙니다.`가 반환되면 일반 return 실패와 섞지 않고 `return_after_rent_compensation_attempts`와 `return_after_rent_compensation_rate`로 별도 집계합니다.

## 설정

환경 변수로 부하와 비동기 대기 시간을 조정할 수 있습니다.

```powershell
$env:K6_TARGET_VUS="50"
$env:K6_RAMP_UP="1m"
$env:K6_STEADY_DURATION="3m"
$env:K6_RAMP_DOWN="1m"
$env:ASYNC_TIMEOUT_SECONDS="15"
$env:SERVICE_READY_TIMEOUT_SECONDS="180"
$env:PRE_REGISTERED_BOOK_COUNT="300"
docker compose --profile perf run --rm k6
```

`PRE_REGISTERED_BOOK_COUNT`는 선등록 도서 수입니다. 실제 등록 수는 `max(PRE_REGISTERED_BOOK_COUNT, K6_TARGET_VUS)`로 보정됩니다.

인기 도서 read model 조회는 데이터가 많아지면 비용이 커질 수 있습니다. 순수 대여/반납 부하만 보고 싶을 때는 끌 수 있습니다.

```powershell
$env:VERIFY_BESTBOOK="false"
docker compose --profile perf run --rm k6
```

## 관측

k6 결과와 함께 Grafana에서 Kafka consumer lag, Spring Boot HTTP/JVM metric, 서비스 로그를 같이 확인합니다.

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:19090`
- Kafka Exporter: `http://localhost:9308/metrics`
- k6 Dashboard: Grafana `도서관 대여 EDA / k6 Performance`
- 통합 성능 관측 Dashboard: Grafana `도서관 대여 EDA / Performance Observability`

주요 확인 항목은 다음과 같습니다.

- Kafka consumer lag: `kafka_consumergroup_lag`
- 비동기 상태 반영 시간: `k6_*_duration_p95`, `k6_*_duration_p99`
- 대여 보상 후 반납 시도: `k6_return_after_rent_compensation_attempts_total`, `k6_return_after_rent_compensation_rate`
- MariaDB connection pressure: `hikaricp_connections_active`, `hikaricp_connections_pending`, `hikaricp_connections_timeout_total`
- MongoDB connection pressure: `mongodb_driver_pool_checkedout`, `mongodb_driver_pool_waitqueuesize`
- JVM 리소스: `process_cpu_usage`, `jvm_memory_used_bytes`, `jvm_gc_pause_seconds_count`
- 컨테이너 리소스: `container_cpu_usage_seconds_total`, `container_memory_working_set_bytes`
