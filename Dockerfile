# 1. 빌드 환경 (JDK 21)
# -----------------------------------------------------------------------------
# 외부 인터넷이 차단된 내부망에서는 Docker Hub 대신 사내 Container Registry의
# Java 21 이미지를 사용합니다. 인프라 담당자에게 이미지의 전체 경로와 태그를
# 받은 후 아래 FROM 행만 교체합니다.
#
# 예: FROM registry.shinhanlife.co.kr/base/openjdk:21 AS builder
#
# 이 단계는 Gradle 빌드와 Java 컴파일을 수행하므로 반드시 JDK 21 이미지여야 합니다.
# 사내 이미지가 실제로 JDK 21인지 다음 명령으로 확인합니다.
# docker run --rm <사내-JDK-이미지> java -version
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle Wrapper와 소스 파일을 복사합니다.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Wrapper 실행 권한을 부여하고 테스트를 제외한 빌드를 수행합니다.
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# 2. 실행 환경 (JRE 21)
# -----------------------------------------------------------------------------
# 실행 단계는 JRE 21 이미지가 가장 가볍지만, 사내에서 JDK 21 이미지만 제공하는
# 경우에는 동일한 JDK 21 이미지를 사용해도 정상 동작합니다.
#
# 예: FROM registry.shinhanlife.co.kr/base/openjre:21
# 예: FROM registry.shinhanlife.co.kr/base/openjdk:21
#
# 사내 제공 이미지의 기반 OS를 확인합니다.
# - Alpine 기반: 아래 apk 명령을 그대로 사용합니다.
# - Ubuntu/Debian 기반: apk 대신 apt-get update && apt-get install -y tzdata를 사용합니다.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 서울 시간대를 설정합니다.
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 빌드 단계에서 생성한 애플리케이션 JAR를 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 포트를 노출합니다.
EXPOSE 8081

# 애플리케이션을 실행합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]