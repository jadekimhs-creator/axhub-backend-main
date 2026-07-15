package io.shinhanlife.axhub.biz.mcp.gateway.service;

import io.shinhanlife.axhub.common.mcp.security.SecurityProperties;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.config.GatewayFallbackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.gateway.service
 * @className ToolPlanner
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
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolPlanner {

    private final RedisRegistryService redisRegistryService;
    private final SecurityProperties securityProperties;
    private final GatewayFallbackProperties fallbackProperties;

    /**
     * 요청(payload)을 분석하여 실행해야 할 Tool의 계획을 생성합니다.
     */
    public Object createPlan(Map<String, Object> payload, String tenantId) {
        log.info(" [Planner] 요청 분석 및 실행 계획 수립 시작");

        // 1. 요청에서 호출하려는 툴 이름 추출 (JSON-RPC params.name)
        Map<String, Object> params = (Map<String, Object>) payload.get("params");
        String toolName = params != null ? (String) params.get("name") : null;

        if (toolName == null || toolName.isEmpty()) {
            throw new IllegalArgumentException("요청에 toolName이 포함되어 있지 않습니다.");
        }

        // 2. RedisRegistry에서 해당 툴의 메타데이터 조회
        // (실제로는 이 메타데이터가 실행 계획의 핵심이 됩니다)
        var toolMetadata = redisRegistryService.getToolByName(toolName);

        if (toolMetadata == null) {
            log.warn(" [Planner] 등록되지 않은 툴 요청: {}. Fallback 라우팅 규칙을 확인합니다.", toolName);

            String fallbackPodUrl = fallbackProperties.getDefaultUrl();
            if (fallbackProperties.getRoutes() != null) {
                for (Map.Entry<String, String> entry : fallbackProperties.getRoutes().entrySet()) {
                    if (toolName.contains(entry.getKey())) {
                        fallbackPodUrl = entry.getValue();
                        break;
                    }
                }
            }

            if (fallbackPodUrl == null || fallbackPodUrl.isEmpty()) {
                log.error(" [Planner] Fallback 라우팅 대상이 아닙니다. 툴: {}", toolName);
                throw new RuntimeException("해당 툴(" + toolName + ")이 레지스트리에 존재하지 않습니다.");
            }

            log.info(" [Planner] Fallback 라우팅 매칭됨: {} -> {}", toolName, fallbackPodUrl);

            toolMetadata = ToolMetadata.builder()
                .uid(toolName)
                .name(toolName)
                .integrationType("DIRECT")
                .podUrl(fallbackPodUrl)
                .build();
        }

        // 2-1. [신규] 도메인 그룹핑 기반 권한 검증
        if (tenantId != null && toolMetadata.getCategoryKey() != null) {
            String normalizedTenantId = tenantId.toLowerCase();
            List<String> allowedDomains = securityProperties.getTenantDomains().get(normalizedTenantId);
            
            // 만약 대소문자 변환 후에도 없으면 원래 값으로 한 번 더 시도 (하위 호환성)
            if (allowedDomains == null) {
                allowedDomains = securityProperties.getTenantDomains().get(tenantId);
            }

            if (allowedDomains == null || 
                (!allowedDomains.contains("ALL") && !allowedDomains.contains(toolMetadata.getCategoryKey()))) {
                log.warn(" [Planner] 권한 거부 - Tenant: {}, Request Domain: {}", tenantId, toolMetadata.getCategoryKey());
                throw new SecurityException("해당 도메인(" + toolMetadata.getCategoryKey() + ")의 툴을 실행할 권한이 없습니다.");
            }
        }

        log.info(" [Planner] 툴 '{}'에 대한 실행 계획 수립 완료", toolName);

        // 3. 수립된 계획(툴 메타데이터) 반환
        return toolMetadata;
    }
}