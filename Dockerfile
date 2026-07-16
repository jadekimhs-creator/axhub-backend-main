# 1. ë¹Œë“œ í™˜ê²½ (JDK 21)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle ë˜í¼ì™€ ì†ŒìŠ¤ ë³µì‚¬
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# ê¶Œí•œ ë¶€ì—¬ ë° ë¹Œë“œ (í…ŒìŠ¤íŠ¸ ì œì™¸)
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# 2. ì‹¤í–‰ í™˜ê²½ (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# íƒ€ì„ì¡´ ì„¤ì • (í•œêµ­ ì‹œê°„)
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul

# ë¹Œë“œëœ JAR íŒŒì¼ ë³µì‚¬
COPY --from=builder /app/build/libs/*.jar app.jar

# ê¸°ë³¸ í¬íŠ¸ ë…¸ì¶œ
EXPOSE 8081

# ì»¨í…Œì´ë„ˆ ì‹¤í–‰ ì‹œ JAR ì‹¤í–‰
ENTRYPOINT ["java", "-jar", "app.jar"]

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (°³¹ß È¯°æ) ---
shinhan.integration.envrTypeCd=D
shinhan.integration.eai.url=http://10.176.32.181
shinhan.integration.internalMci.url=http://10.176.32.173
shinhan.integration.bancaMci.url=http://10.176.32.117
shinhan.integration.externalMci.url=http://10.176.32.176

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (Å×½ºÆ® È¯°æ) ---
shinhan.integration.envrTypeCd=T
shinhan.integration.eai.url=http://10.174.32.181
shinhan.integration.internalMci.url=http://10.174.32.173
shinhan.integration.bancaMci.url=http://10.174.32.117
shinhan.integration.externalMci.url=http://10.176.32.177

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (¿î¿µ È¯°æ) ---
shinhan.integration.envrTypeCd=R
shinhan.integration.eai.url=http://10.172.32.181
shinhan.integration.internalMci.url=http://10.172.32.173
shinhan.integration.bancaMci.url=http://10.172.32.117
shinhan.integration.externalMci.url=http://10.172.32.177
