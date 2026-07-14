package io.shinhanlife.axhub.biz.mcp.gateway.dto;

import lombok.Getter;
import lombok.Setter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Tool(Agent)의 명세 및 라우팅 정보를 담고 있는 메타데이터 클래스
 * Redis 레지스트리에 저장되며, Planner와 Router 간의 통신 객체(Plan)로 사용됩니다.
 */
/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.dto
 * @className ToolMetadata
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  김형식    최초생성
 * 
 * </pre>
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ToolMetadata {

    // 1. Tool 기본 정보
    private String uid;               // UUID 형식의 고유 식별자
    private String semver;            // 버전 (예: 1.0.0)
    private String displayName;              // 사람이 읽는 라벨 (1-128자)
    private String name;       // MCP 서브툴 명칭 (64자 이하, 예: CustomerSearchTool)
    private String description;       // 툴의 목적 및 설명 (LLM 프롬프트에 활용 가능)

    // 2. 파라미터 스키마 (JSON Schema 형태의 Map)
    private Map<String, Object> parametersSchema;

    // 2-0. 프론트엔드 UI용 함수별 프롬프트 매핑 (추가됨)
    private Map<String, String> actionPrompts;

    // 2-1. 도메인 부서 그룹명 (category_key, 슬러그 형식)
    private String categoryKey;

    // 2-2. 툴 처리 엔드포인트 URI 경로 (예: /api/tool/customer-info)
    private String endpoint;

    // 2-3. Pod 실행 URL (독립적인 Microservice 라우팅용, 예: http://localhost:8082)
    private String podUrl;

    // 2-4. 가시성 여부
    @Builder.Default
    private Boolean visible = true;

    // 2-5. Redis 등록 여부 (UI 표출용)
    @Builder.Default
    private Boolean isRegistered = true;

    // 2-6. HITL 승인 필요 여부
    @Builder.Default
    private Boolean requiresApproval = false;



    // 3. 연동 아키텍처 구분 (DIRECT / MCI_EAI)
    private String integrationType;   // 연동 타입: "DIRECT" 또는 "MCI_EAI"

    // 4. 레거시(MCI/EAI) 연동 시 필수 정보 (integrationType이 "MCI_EAI"일 때 사용)
    private String mciServiceId;      // MCI/EAI 호출을 위한 서비스 ID (예: CRM_001, LICO_992)

    // 5. 인프라 상태 정보 (DIRECT 연동 시 사용)
    private Long lastHeartbeat;       // Redis TTL 갱신용 마지막 하트비트 타임스탬프

    // 6. 동적 서킷 브레이커 & 속도 제어 설정 (Registry 기반)
    private Integer failureRateThreshold; // 서킷 브레이커 동작 기준 실패율 (%)
    private Integer slidingWindowSize;    // 서킷 브레이커 에러율 계산 표본 요청 수
    private Integer rateLimitForPeriod;   // 속도 제어: 1초당 허용 최대 요청 수

    // 7. Gateway 코어 제어용 설정 필드 추가 (재시도, 타임아웃, 오퍼레이션 타입)
    @Builder.Default
    private OperationType operationType = OperationType.READ;
    
    @Builder.Default
    private Boolean retryEnabled = true;

    @Builder.Default
    private Integer circuitBreakerFailureThreshold = 0;

    @Builder.Default
    private Long circuitBreakerOpenMillis = 0L;

    @Builder.Default
    private Long timeoutMillis = 0L;

    // --- Guardrail 호환성을 위한 메서드 추가 ---
    public java.util.Set<String> allowedArguments() {
        if (parametersSchema == null || !parametersSchema.containsKey("properties")) return java.util.Set.of();
        return ((Map<String, Object>) parametersSchema.get("properties")).keySet();
    }

    public java.util.Set<String> requiredArguments() {
        if (parametersSchema == null || !parametersSchema.containsKey("required")) return java.util.Set.of();
        return new java.util.HashSet<>((java.util.List<String>) parametersSchema.get("required"));
    }
}