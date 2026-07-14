FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY axhub-common axhub-common
COPY axhub-tool-core axhub-tool-core
COPY axhub-tool-other axhub-tool-other
RUN chmod +x gradlew
RUN ./gradlew :axhub-tool-other:build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul
COPY --from=builder /app/axhub-tool-other/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
