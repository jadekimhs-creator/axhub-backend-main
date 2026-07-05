# 1. 빌드 환경 (JDK 21)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle 래퍼와 소스 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# 권한 부여 및 빌드 (테스트 제외)
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# 2. 실행 환경 (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 기본 포트 노출
EXPOSE 8081

# 컨테이너 실행 시 JAR 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
