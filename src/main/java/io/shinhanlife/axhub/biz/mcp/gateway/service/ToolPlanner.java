package io.shinhanlife.axhub.biz.mcp.gateway.service;

import io.shinhanlife.axhub.common.mcp.security.SecurityProperties;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolPlanner {

    private final RedisRegistryService redisRegistryService;
    private final SecurityProperties securityProperties;

    /**
     * 요청(payload)을 분석하여 실행해야 할 Tool의 계획을 생성합니다.
     */
    public Object createPlan(Map<String, Object> payload, String tenantId) {
        log.info("📝 [Planner] 요청 분석 및 실행 계획 수립 시작");

        // 1. 요청에서 호출하려는 툴 이름 추출
        String toolName = (String) payload.get("toolName");

        if (toolName == null || toolName.isEmpty()) {
            throw new IllegalArgumentException("요청에 toolName이 포함되어 있지 않습니다.");
        }

        // 2. RedisRegistry에서 해당 툴의 메타데이터 조회
        // (실제로는 이 메타데이터가 실행 계획의 핵심이 됩니다)
        var toolMetadata = redisRegistryService.getTool(toolName);

        if (toolMetadata == null) {
            log.warn(" [Planner] 등록되지 않은 툴 요청: {}", toolName);
            throw new RuntimeException("해당 툴(" + toolName + ")이 레지스트리에 존재하지 않습니다.");
        }

        // 2-1. [신규] 도메인 그룹핑 기반 권한 검증
        if (tenantId != null && toolMetadata.getDomainGroup() != null) {
            List<String> allowedDomains = securityProperties.getTenantDomains().get(tenantId);
            if (allowedDomains == null || 
                (!allowedDomains.contains("ALL") && !allowedDomains.contains(toolMetadata.getDomainGroup()))) {
                log.warn(" [Planner] 권한 거부 - Tenant: {}, Request Domain: {}", tenantId, toolMetadata.getDomainGroup());
                throw new SecurityException("해당 도메인(" + toolMetadata.getDomainGroup() + ")의 툴을 실행할 권한이 없습니다.");
            }
        }

        log.info(" [Planner] 툴 '{}'에 대한 실행 계획 수립 완료", toolName);

        // 3. 수립된 계획(툴 메타데이터) 반환
        return toolMetadata;
    }
}
