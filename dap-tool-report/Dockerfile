FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew \
    && ./gradlew :dap-tool-report:bootJar -x test --no-daemon \
    && find dap-tool-report/build/libs -name '*-SNAPSHOT.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata
ENV TZ=Asia/Seoul
COPY --from=builder /workspace/app.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
