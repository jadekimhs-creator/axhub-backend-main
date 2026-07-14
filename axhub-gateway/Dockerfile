FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY axhub-common axhub-common
COPY axhub-gateway axhub-gateway
RUN chmod +x gradlew
RUN ./gradlew :axhub-gateway:build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul
COPY --from=builder /app/axhub-gateway/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
