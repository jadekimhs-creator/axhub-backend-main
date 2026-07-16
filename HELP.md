# Read Me First
The following was discovered as part of building this project:

* The original package name 'io.shinhanlife.chat-backend' is invalid and this project uses 'io.shinhanlife.chatbackend' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.3/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.3/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.3/reference/web/servlet.html)
* [Spring Session for Spring Data Redis](https://docs.spring.io/spring-session/reference/)
* [Spring Data JPA](https://docs.spring.io/spring-boot/3.5.3/reference/data/sql.html#data.sql.jpa-and-spring-data)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.


# --- 신한라이프 EAI/MCI 연계 IP 정보 (개발 환경) ---
shinhan.integration.envrTypeCd=D
shinhan.integration.eai.url=http://10.176.32.181
shinhan.integration.internalMci.url=http://10.176.32.173
shinhan.integration.bancaMci.url=http://10.176.32.117
shinhan.integration.externalMci.url=http://10.176.32.176

# --- 신한라이프 EAI/MCI 연계 IP 정보 (테스트 환경) ---
shinhan.integration.envrTypeCd=T
shinhan.integration.eai.url=http://10.174.32.181
shinhan.integration.internalMci.url=http://10.174.32.173
shinhan.integration.bancaMci.url=http://10.174.32.117
shinhan.integration.externalMci.url=http://10.176.32.177

# --- 신한라이프 EAI/MCI 연계 IP 정보 (운영 환경) ---
shinhan.integration.envrTypeCd=R
shinhan.integration.eai.url=http://10.172.32.181
shinhan.integration.internalMci.url=http://10.172.32.173
shinhan.integration.bancaMci.url=http://10.172.32.117
shinhan.integration.externalMci.url=http://10.172.32.177
