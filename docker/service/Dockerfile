FROM eclipse-temurin:21-jdk AS build

ARG SERVICE_NAME
WORKDIR /workspace

COPY . .

RUN chmod +x ./gradlew && ./gradlew --no-daemon ":${SERVICE_NAME}:bootJar"

FROM eclipse-temurin:21-jre

ARG SERVICE_NAME
ENV JAVA_OPTS=""
WORKDIR /app

COPY --from=build /workspace/${SERVICE_NAME}/build/libs/*.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
