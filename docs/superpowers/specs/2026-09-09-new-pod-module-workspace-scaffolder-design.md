# Pod Module New Workspace Scaffolder 설계

## 목표

기존 Pod Module Scaffold(`PodScaffolder`, `/api/v1/scaffold/pod`, 기존 화면)을 변경하지 않는다.
Scaffold 화면에 별도 `Pod Module New` 기능을 추가하여, 사용자가 입력한 Pod 모듈명을 기준으로 독립 Tool Pod 프로젝트를 `C:\eGovFrameDev-4.3.1-64bit\workspace` 아래에 생성한다.

## 생성 구조

`dat-was-payment` 입력 시 다음 독립 프로젝트를 생성한다.

```text
C:\eGovFrameDev-4.3.1-64bit\workspace\dat-was-payment\
  settings.gradle
  build.gradle
  README.md
  Dockerfile
  docker-compose.yml
  k8s\
  src\main\java\...
  src\main\resources\...
```

입력 모듈명은 저장소 루트명, Gradle 루트 프로젝트명, Spring application name에 동일하게 사용한다.
생성 프로젝트는 `../dat-lib-datmt`를 Gradle composite build로 포함하고, `io.shinhanlife:dat-lib-datmt:0.0.1-SNAPSHOT` 좌표를 `:dat-was-lib`로 치환한다.

## 새 생성 경로

- 신규 `NewPodProjectScaffolder`가 독립 프로젝트 파일을 생성한다.
- 신규 Controller endpoint가 module name과 사용자가 입력한 서비스 포트를 전달한다.
- Scaffold 화면에는 기존 Pod Module 탭과 별개인 `Pod Module New` 영역을 둔다.
- workspace 경로는 사용자 입력값을 사용하며 기본값은 `C:\eGovFrameDev-4.3.1-64bit\workspace`이다.
- 존재하는 프로젝트 경로에는 생성하지 않고 명확한 오류를 반환한다.

## 기존 기능 보호

다음 기존 파일과 동작은 변경하지 않는다.

- `PodScaffolder`
- `/api/v1/scaffold/pod`
- 기존 Pod Module 화면과 기존 Pod 모듈

## 생성 파일의 주요 설정

- Spring Boot 3.5.11, Java 21
- 공통 라이브러리 composite build 연결
- `server.port`는 사용자가 입력한 서비스 포트를 기본값으로 사용하고, 환경변수 `PORT`로 덮어쓸 수 있다.
- Docker Compose 서비스명은 모듈명의 `dat-was-` 접두사를 `was-`로 치환한다.
- Tool manifest는 새 Pod의 라우팅 함수만 포함한다.
- README는 공통 라이브러리 위치, 실행 방법, 입력 포트 설정을 안내한다.

## 검증

1. 신규 Scaffolder 단위 테스트로 생성 파일, composite build 설정, 기존 경로 거부를 검증한다.
2. Controller 테스트로 신규 endpoint의 정상 생성과 중복 경로 오류를 검증한다.
3. 임시 workspace에 생성한 Pod의 `compileJava`를 실행해 `../dat-lib-datmt` 의존성 해석을 확인한다.
4. 기존 `PodScaffolder` 관련 테스트를 실행해 기존 생성 경로가 바뀌지 않았음을 확인한다.
