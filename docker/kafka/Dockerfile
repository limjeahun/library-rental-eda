ARG KAFKA_IMAGE=apache/kafka:3.9.0

FROM curlimages/curl:8.11.1 AS jmx-exporter
ARG JMX_EXPORTER_VERSION=1.5.0
RUN curl -fsSL \
    -o /tmp/jmx_prometheus_javaagent.jar \
    "https://github.com/prometheus/jmx_exporter/releases/download/${JMX_EXPORTER_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_VERSION}.jar"

FROM ${KAFKA_IMAGE}
COPY --from=jmx-exporter /tmp/jmx_prometheus_javaagent.jar /opt/jmx_exporter/jmx_prometheus_javaagent.jar
COPY kafka-jmx.yml /opt/jmx_exporter/kafka-jmx.yml
