import os

root_build_gradle = """plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.5' apply false
    id 'io.spring.dependency-management' version '1.1.6' apply false
}

allprojects {
    group = 'io.shinhanlife'
    version = '0.0.1-SNAPSHOT'
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        sourceCompatibility = '21'
    }

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
        }
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok:1.18.32'
        annotationProcessor 'org.projectlombok:lombok:1.18.32'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }

    tasks.withType(JavaCompile) {
        options.compilerArgs << '-parameters'
        options.compilerArgs << '-Amapstruct.defaultComponentModel=spring'
    }
}
"""

with open("build.gradle", "w", encoding="utf-8") as f:
    f.write(root_build_gradle)

# axhub-common
common_build_gradle = """dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.1'
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
"""
with open("axhub-common/build.gradle", "w", encoding="utf-8") as f:
    f.write(common_build_gradle)

# axhub-tool-core
core_build_gradle = """dependencies {
    api project(':axhub-common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Resilience4j
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-core:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-ratelimiter'
    implementation 'io.github.resilience4j:resilience4j-retry:2.2.0'
    implementation 'org.springframework.boot:spring-boot-starter-aop:3.3.0'
    
    // MCP required
    implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.1'
}
"""
with open("axhub-tool-core/build.gradle", "w", encoding="utf-8") as f:
    f.write(core_build_gradle)

# gateway
gateway_build_gradle = """plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation project(':axhub-common')
    
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // MyBatis & DB
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3'
    runtimeOnly 'com.h2database:h2'
    implementation 'p6spy:p6spy:3.9.1'
    
    // MapStruct
    implementation "org.mapstruct:mapstruct:1.5.5.Final"
    annotationProcessor "org.projectlombok:lombok-mapstruct-binding:0.2.0"
    annotationProcessor "org.mapstruct:mapstruct-processor:1.5.5.Final"

    implementation 'com.networknt:json-schema-validator:1.4.0'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'
}
"""
with open("axhub-gateway/build.gradle", "w", encoding="utf-8") as f:
    f.write(gateway_build_gradle)

# tool-sms, email, other
for tool in ["axhub-tool-sms", "axhub-tool-email", "axhub-tool-other"]:
    tool_build_gradle = """plugins {
    id 'org.springframework.boot'
}

dependencies {
    implementation project(':axhub-tool-core')
}
"""
    with open(f"{tool}/build.gradle", "w", encoding="utf-8") as f:
        f.write(tool_build_gradle)

print("Gradle files generated.")
