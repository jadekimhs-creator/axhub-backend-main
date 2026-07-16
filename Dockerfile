# 1. 鍮뚮뱶 ?섍꼍 (JDK 21)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle ?섑띁? ?뚯뒪 蹂듭궗
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# 沅뚰븳 遺??諛?鍮뚮뱶 (?뚯뒪???쒖쇅)
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# 2. ?ㅽ뻾 ?섍꼍 (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# ??꾩〈 ?ㅼ젙 (?쒓뎅 ?쒓컙)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# 鍮뚮뱶??JAR ?뚯씪 蹂듭궗
COPY --from=builder /app/build/libs/*.jar app.jar

# 湲곕낯 ?ы듃 ?몄텧
EXPOSE 8081

# 而⑦뀒?대꼫 ?ㅽ뻾 ??JAR ?ㅽ뻾
ENTRYPOINT ["java", "-jar", "app.jar"]
