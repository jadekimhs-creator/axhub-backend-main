# 신한라이프 내부망 Tool Pod 이관 준비 체크리스트

> 범위: `dat-was-lib`, `dat-was-cus`, `dat-was-sal`, `dat-was-pro`, `dat-was-sys` Tool Pod 이관
> 

## 1. 소스 및 형상관리

- [ ] 내부 Git 저장소 생성
- [ ] 대상 모듈 이관: `dat-was-lib`, `dat-was-cus`, `dat-was-sal`, `dat-was-pro`, `dat-was-sys`
- [ ] `main` 브랜치 및 필요한 커밋 이력 이관
- [ ] API Key, 비밀번호, 인증서, 개인 설정 파일은 Git에서 제외
- [ ] 내부 Git URL 기준으로 README와 CI/CD 설정 변경

## 2. 개발 및 빌드 환경

- [ ] JDK 21 설치 및 `JAVA_HOME` 설정
- [ ] Gradle Wrapper 실행 가능 여부 확인
- [ ] Docker 또는 Kubernetes 배포 환경 확인
- [ ] 내부 Git 접근 권한 확인
- [ ] 사내 Nexus 접근 권한 확인

## 3. 사내 Nexus 라이브러리 준비

인터넷 없이 빌드하려면 사내 Nexus에 다음 라이브러리 또는 Proxy Repository가 준비되어야 합니다.

- [ ] Spring Boot 3.5.11
- [ ] Spring AI 1.1.8
- [ ] MapStruct, Lombok, H2, P6Spy
- [ ] Glow Framework 관련 라이브러리
- [ ] 신한라이프 MCI/EAI 및 보안 관련 사내 라이브러리
- [ ] Docker Base Image: `eclipse-temurin:21-jre-alpine`

## 4. 환경별 설정

사용 프로필:

```text
local / dev / test / prod
```

배포 환경변수:

```text
SPRING_PROFILES_ACTIVE
AXHUB_TOOL_URL

GLOW_COMMUNICATION_MCI_HOST
GLOW_COMMUNICATION_MCI_PORT
```

- [ ] 개발계(`dev`) MCI 주소 및 포트 등록
- [ ] 테스트계(`test`) MCI 주소 및 포트 등록
- [ ] 운영계(`prod`) MCI 주소 및 포트 등록
- [ ] 운영 비밀값은 Git이 아닌 배포 환경변수 또는 Secret으로 관리

## 5. MCI 연계 협의

MCI 담당자에게 아래 정보를 요청합니다.

- [ ] 개발·테스트·운영 MCI URL과 포트
- [ ] Interface ID와 URI
- [ ] 요청·응답 전문 규격
- [ ] 필수 Header 및 인증 방식
- [ ] Connection/Read Timeout 기준
- [ ] Tool Pod → MCI 통신 ACL 허용

## 6. Tool Pod 배포 정보

| Tool Pod | 내부 Endpoint |
|---|---|
| CUS Tool Pod | `http://was-cus:8084/mcp` |
| SAL Tool Pod | `http://was-sal:8082/mcp` |
| PRO Tool Pod | `http://was-pro:8085/mcp` |
| SYS Tool Pod | `http://was-sys:8086/mcp` |

Portal 담당자에게 아래 정보를 전달합니다.

- [ ] Pod명 및 서비스명
- [ ] 포트와 MCP Endpoint
- [ ] Tool 목록 및 Tool 명칭
- [ ] 담당자 및 장애 연락처
- [ ] Health Check URL

## 7. Redis 정책

- [ ] Tool Pod에서 Redis 사용 여부 결정
- [ ] Redis를 사용할 경우 Host, Port, Password, ACL 확인
- [ ] Redis를 사용하지 않을 경우 Tool Pod 기능에 영향이 없는지 개별 Tool 기준 확인

## 8. 이관 후 검증

- [ ] 내부 Nexus만으로 `./gradlew clean build` 성공
- [ ] CUS, SAL, PRO, SYS Tool Pod 기동 성공
- [ ] 각 Tool Pod의 MCP Endpoint 연결 성공
- [ ] Tool Pod → MCI 호출 성공
- [ ] `trace-id`, `request-id` 전달 확인
- [ ] Tool 이름 중복 검증 확인
- [ ] 민감정보와 비밀값이 Git에 포함되지 않았는지 확인
