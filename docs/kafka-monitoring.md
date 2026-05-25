# Kafka Monitoring

이 프로젝트의 로컬 Kafka와 Spring Boot 서비스는 Prometheus, Loki, Grafana로 확인할 수 있습니다.

## 구성

| 컴포넌트 | 주소 | 역할 |
| --- | --- | --- |
| Kafka JMX Exporter | `http://localhost:7071/metrics` | Kafka broker JMX/JVM 지표 노출 |
| Kafka Exporter | `http://localhost:9308/metrics` | Topic, partition, consumer group lag 지표 노출 |
| Prometheus | `http://localhost:9090` | Kafka와 Spring Boot 서비스 지표 수집 |
| Loki | `http://localhost:3100` | 컨테이너 로그 저장 및 LogQL 질의 |
| Alloy | `http://localhost:12345` | Docker 컨테이너 로그 수집 및 Loki 전달 |
| Grafana | `http://localhost:3000` | Kafka 지표와 Loki 로그 대시보드 표시 |

Grafana 기본 계정은 `admin` / `admin`입니다. 기본 UI 언어는 한국어(`ko-KR`)로 설정됩니다. `도서관 대여 EDA / 도서관 Kafka 모니터링`, `도서관 대여 EDA / 도서관 로그 모니터링` 대시보드가 자동 provision 됩니다.

## 실행

처음 실행하거나 Kafka 이미지 구성이 바뀐 경우에는 `--build`를 붙입니다.

```bash
docker compose up -d --build
```

이미 빌드가 끝난 뒤에는 일반 실행으로 충분합니다.

```bash
docker compose up -d
```

## 확인

Prometheus target 상태:

```bash
curl http://localhost:9090/api/v1/targets
```

Kafka broker JMX 지표:

```bash
curl http://localhost:7071/metrics
```

Topic/consumer lag 지표:

```bash
curl http://localhost:9308/metrics
```

Spring Boot 서비스 Prometheus 지표:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
curl http://localhost:8084/actuator/prometheus
```

Loki readiness:

```bash
curl http://localhost:3100/ready
```

Alloy UI:

```text
http://localhost:12345/graph
```

## 주요 PromQL

| 목적 | PromQL |
| --- | --- |
| Kafka JMX scrape 상태 | `up{job="kafka-jmx"}` |
| 초당 유입 메시지 | `sum(rate(kafka_server_brokertopicmetrics_messagesin_total[1m]))` |
| 초당 broker 입력 bytes | `sum(rate(kafka_server_brokertopicmetrics_bytesin_total[1m]))` |
| 초당 broker 출력 bytes | `sum(rate(kafka_server_brokertopicmetrics_bytesout_total[1m]))` |
| Consumer group lag | `sum(kafka_consumergroup_lag) by (consumergroup)` |
| Topic별 current offset | `sum(kafka_topic_partition_current_offset) by (topic)` |
| Offline partition 수 | `kafka_controller_kafkacontroller_offlinepartitionscount` |
| Under replicated partition 수 | `kafka_server_replicamanager_underreplicatedpartitions` |
| Spring Boot scrape 상태 | `up{job="spring-services"}` |
| 서비스별 HTTP 요청 수 | `sum by (application) (increase(http_server_requests_seconds_count{job="spring-services"}[5m]))` |
| 서비스별 JVM heap 사용량 | `sum by (application) (jvm_memory_used_bytes{job="spring-services", area="heap"})` |

## 주요 LogQL

| 목적 | LogQL |
| --- | --- |
| 전체 로컬 컨테이너 로그 | `{app="library-rental-eda"}` |
| 서비스별 로그 유입량 | `sum by (service_name) (rate({app="library-rental-eda"}[5m]))` |
| 경고 및 오류 로그 | `{app="library-rental-eda"} |~ "(?i)error|exception|warn"` |
| Kafka 컨테이너 로그 | `{app="library-rental-eda", service_name="kafka"}` |
| rental-service 로그 | `{app="library-rental-eda", service_name="rental-service"}` |

Alloy는 Docker socket을 읽어 컨테이너 로그를 수집합니다. 로컬 개발용 구성이라 `loki`와 `alloy`는 단일 컨테이너로 실행하고, Loki 로그 보존 기간은 7일(`168h`)입니다.
