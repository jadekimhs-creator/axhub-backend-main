import os

modules = {
    "axhub-gateway": 8081,
    "axhub-tool-sms": 8082,
    "axhub-tool-email": 8083,
    "axhub-tool-other": 8084
}

# 1. Create Dockerfiles
dockerfile_template = """FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY build/libs/{module}-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
"""

for module in modules:
    with open(f"{module}/Dockerfile", "w") as f:
        f.write(dockerfile_template.format(module=module))

# 2. Create application.properties for tool modules
for module, port in modules.items():
    if module == "axhub-gateway":
        continue
        
    res_dir = f"{module}/src/main/resources"
    os.makedirs(res_dir, exist_ok=True)
    with open(f"{res_dir}/application.properties", "w") as f:
        f.write(f"server.port={port}\n")
        f.write("spring.application.name=" + module + "\n")

# 3. Create docker-compose.yml
compose_content = """version: '3.8'

services:
  redis:
    image: redis:latest
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  gateway:
    build: 
      context: ./axhub-gateway
    ports:
      - "8081:8081"
    depends_on:
      redis:
        condition: service_healthy
    environment:
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379

  tool-sms:
    build: 
      context: ./axhub-tool-sms
    ports:
      - "8082:8082"
    depends_on:
      - redis

  tool-email:
    build: 
      context: ./axhub-tool-email
    ports:
      - "8083:8083"
    depends_on:
      - redis

  tool-other:
    build: 
      context: ./axhub-tool-other
    ports:
      - "8084:8084"
    depends_on:
      - redis
"""

with open("docker-compose.yml", "w") as f:
    f.write(compose_content)

print("Docker configurations generated.")
