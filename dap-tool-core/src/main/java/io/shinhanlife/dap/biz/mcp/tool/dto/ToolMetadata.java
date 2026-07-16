package io.shinhanlife.dap.biz.mcp.tool.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * @package io.shinhanlife.dap.biz.mcp.tool.dto
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
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String endpoint;
    private String podUrl;
    private String integrationType;
    private String mciServiceId;


    @Builder.Default
    private Boolean visible = true;

    @Builder.Default
    private Boolean isRegistered = true;

    @Builder.Default
    private Boolean requiresApproval = false;



}