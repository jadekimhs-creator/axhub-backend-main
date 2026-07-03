# AXHUB Backend

Spring Boot 기반 AXHUB 관리자 백엔드 API 서버입니다.

---

## 환경

| 항목 | 버전 |
|------|------|
| Java | 17 |
| Spring Boot | 3.5.x |
| Build Tool | Maven |
| 주요 라이브러리 | MyBatis, Lombok, MapStruct, P6Spy |
| 데이터베이스 | H2 (in-memory, 로컬 개발용) |
| 세션 저장소 | Redis |

---

## 실행 방법

`AxHubAdminApplication.java`를 실행합니다.

```
src/main/java/io/shinhanlife/AxHubAdminApplication.java
```

- IDE: 클래스 우클릭 → **Run 'AxHubAdminApplication'**
- CLI: `./mvnw spring-boot:run`
- 기본 포트: `8080`
- H2 콘솔: `http://localhost:8080/h2-console` (로컬 환경에서만 활성화)

---

## 패키지 구조

```
io.shinhanlife
├── AxHubAdminApplication.java
│
├── axhub/
│   ├── biz/                        # 업무 도메인
│   │   ├── sm/mmg/                 # 메뉴 관리 (System Management - Menu Management)
│   │   └── so/atm/                 # 접근 권한 관리 (System Operation - Access Management)
│   │
│   ├── common/                     # 공통 모듈
│   │   ├── config/                 # Spring 설정 (CORS 등)
│   │   ├── session/                # 세션/SSO 처리 (biz와 동일한 레이어 구조)
│   │   └── util/                   # 유틸리티
│   │
│   └── sample/                     # 개발 참고용 샘플
│
└── glow/                           # Glow 프레임워크 호환 패키지 (하단 참고)
```

각 업무 패키지(`biz/**`, `common/session`, `sample`)는 아래 **5개 레이어**로 구성됩니다.

---

## 레이어 구조

```
{업무패키지}/
├── presentation/       ← HTTP 진입점
│   └── io/             ← Request / Response 객체
├── usecase/            ← 비즈니스 흐름 제어
│   └── impl/
├── dto/                ← 레이어 간 데이터 전달
├── domain/             ← 핵심 비즈니스 로직
│   ├── model/          ← 도메인 엔티티
│   ├── repository/     ← DB 접근 인터페이스 (MyBatis Mapper)
│   └── service/        ← 도메인 서비스
│       └── impl/
└── converter/          ← 객체 변환 (MapStruct)
```

### presentation

**역할**: HTTP 요청을 받아 UseCase를 호출하고 응답을 반환합니다. 비즈니스 로직을 포함하지 않습니다.

- `@RestController` 클래스
- `io/` 하위에 해당 API 전용 Request/Response 클래스를 위치시킵니다.
- Presentation이 직접 알아야 하는 타입은 `io/`의 Request/Response와 UseCase 인터페이스뿐입니다.

```
presentation/
├── SmNmg0100MController.java       # @RestController
└── io/
    ├── SmNmg0100M01RRequest.java   # 조회 요청
    └── SmNmg0100M01RResponse.java  # 조회 응답
```

---

### usecase

**역할**: 하나의 업무 흐름(시나리오)을 조율합니다. 여러 Domain Service 또는 Repository를 순서에 맞게 호출하며, 트랜잭션 경계를 정의합니다.

- 인터페이스(`UseCase`)와 구현체(`impl/UseCaseImpl`)를 분리합니다.
- Presentation → **UseCase** → Domain 방향으로만 호출합니다.
- DTO를 입출력 타입으로 사용합니다.

```java
public interface SmNmg0100MUseCase {
    List<MenuOutDto> getMenuList(MenuInDto inDto);
    void saveMenu(MenuSaveInDto inDto);
}
```

---

### dto

**역할**: 레이어 간 데이터를 운반하는 순수 데이터 객체입니다.

- 비즈니스 로직을 포함하지 않습니다.
- Presentation의 `io/` Request/Response와 구분됩니다.
  - `io/` → HTTP 스펙에 종속된 입출력
  - `dto/` → 내부 레이어 간 전달용
- Converter가 `io/ ↔ dto ↔ domain model` 간 변환을 담당합니다.

---

### domain

**역할**: 핵심 비즈니스 규칙과 DB 접근을 담당합니다.

- **`model/`**: 도메인 엔티티. DB 테이블에 대응하는 객체입니다.
- **`repository/`**: MyBatis Mapper 인터페이스. SQL은 `resources/mapper/` 하위 XML에 작성합니다.
- **`service/`**: 단일 도메인 내 재사용 가능한 비즈니스 로직. UseCase가 호출합니다.

> UseCase와 Service의 구분 기준: 여러 업무에서 재사용 가능한 단위 로직은 `domain/service`, 특정 업무 흐름의 조율은 `usecase`에 둡니다.

---

### converter

**역할**: 레이어 간 객체 변환을 전담합니다. MapStruct를 사용합니다.

- `Request → DTO`, `DTO → Model`, `Model → DTO`, `DTO → Response` 변환을 처리합니다.
- Presentation과 UseCase가 직접 매핑 코드를 작성하지 않도록 분리합니다.

```java
@Mapper(componentModel = "spring")
public interface MenuConverter {
    MenuInDto toDto(SmNmg0100M01RRequest request);
    MenuOutDto toDto(ZtMenu model);
}
```

---

## 호출 흐름

```
HTTP 요청
  └─▶ presentation (Controller)
          │  Request → DTO (Converter)
          └─▶ usecase (UseCase)
                  │  비즈니스 흐름 조율
                  └─▶ domain/service (Service)
                          │
                          └─▶ domain/repository (Mapper)
                                  │
                                  └─▶ DB (H2 / 운영 DB)
```

---

## io.shinhanlife.glow 패키지

Glow 프레임워크(사내 공통 프레임워크) 호환을 위해 **임시로** 만들어놓은 패키지입니다.  
추후 Glow 라이브러리 의존성으로 대체될 예정이며, 현재는 로컬 소스 형태로 포함되어 있습니다.

| 클래스 | 역할 |
|--------|------|
| `BaseResponse` / `ResponseUtil` | 공통 API 응답 래퍼 |
| `BaseException` / `BizException` | 공통 예외 |
| `ResponseCode` | 응답 코드 정의 |
| `GlowIndexPaging` / `PageInfo` | 페이징 |
| `GlowLogger` | 로깅 |
| `GlowMybatisMapper` | MyBatis Mapper 기반 인터페이스 |
| `GlowAppServiceId` 등 | 서비스/컨트롤러 식별 애노테이션 |
| `db/dto/AuditInfo` | 등록자/수정자 공통 필드 |
