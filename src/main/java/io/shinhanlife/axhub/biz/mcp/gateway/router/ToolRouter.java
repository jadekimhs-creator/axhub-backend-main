package io.shinhanlife.axhub.biz.mcp.gateway.router;

import io.shinhanlife.axhub.biz.mcp.gateway.dto.ToolMetadata;
import io.shinhanlife.axhub.biz.mcp.gateway.registry.RedisRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRouter {

    private final RedisRegistryService redisRegistryService;

    // application.yml 등에 정의된 신한라이프 EAI 표준 게이트웨이 주소
    @Value("${shinhan.eai.gateway.url:http://eai.internal.shinhanlife.com/api/v1/invoke}")
    private String eaiGatewayUrl;

    /**
     * Planner에서 생성된 실행 계획(ToolMetadata)을 바탕으로
     * Client가 호출할 최종 타겟 정보(URL 및 필수 헤더/파라미터)를 라우팅합니다.
     *
     * @param plan ToolPlanner에서 반환한 실행 계획 (ToolMetadata)
     * @return Client에게 전달될 라우팅 정보 Map (targetUrl, integrationType, mciServiceId 등)
     */
    public Map<String, Object> route(Object plan) {
        log.info("📍 [Router] 타겟 시스템 라우팅 분석 시작");

        // 1. Planner에서 넘어온 plan을 ToolMetadata로 안전하게 캐스팅 (Java 16+ 패턴 매칭)
        if (!(plan instanceof ToolMetadata toolMetadata)) {
            log.error(" [Router] 잘못된 Plan 객체 타입 전달");
            throw new IllegalArgumentException("올바르지 않은 실행 계획(Plan) 타입입니다.");
        }

        String toolName = toolMetadata.getToolName();
        String integrationType = toolMetadata.getIntegrationType();

        Map<String, Object> routingResult = new HashMap<>();
        routingResult.put("toolName", toolName);
        routingResult.put("integrationType", integrationType);

        // 2. 분기 처리: 레거시(MCI/EAI) 연동 vs 내부 직접(DIRECT) 연동
        if ("MCI_EAI".equalsIgnoreCase(integrationType)) {
            // [A] MCI/EAI 연동 로직
            String mciServiceId = toolMetadata.getMciServiceId();
            if (mciServiceId == null || mciServiceId.isBlank()) {
                log.error(" [Router] EAI 라우팅 실패: mciServiceId가 누락되었습니다. (Tool: {})", toolName);
                throw new IllegalStateException("MCI/EAI 연동 툴의 필수값(Service ID)이 누락되었습니다.");
            }

            log.info(" [Router] MCI/EAI 연계 라우팅 - Tool: {}, Service ID: {}", toolName, mciServiceId);

            // EAI 고정 서버 주소와 Service ID를 결과에 담아 Client로 전달
            routingResult.put("targetUrl", eaiGatewayUrl);
            routingResult.put("mciServiceId", mciServiceId);

        } else {
            // [B] 내부 마이크로서비스 직접(DIRECT) 연동 로직
            log.info(" [Router] 내부 DIRECT 연계 라우팅 - Tool: {}", toolName);

            // Redis에서 현재 살아있는 해당 툴의 활성 Pod(URL) 목록 조회
            List<String> availablePods = redisRegistryService.getAvailablePods(toolName);

            if (availablePods == null || availablePods.isEmpty()) {
                log.error(" [Router] 라우팅 실패: '{}' 툴을 처리할 수 있는 활성 Pod가 없습니다.", toolName);
                throw new IllegalStateException("활성화된 대상 Tool Pod를 찾을 수 없습니다: " + toolName);
            }

            // 다중 Pod 중 하나를 선택 (Random 로드밸런싱)
            String targetUrl = selectPodLoadBalanced(availablePods);
            log.info(" [Router] DIRECT 라우팅 완료 - 선택된 타겟 URL: {}", targetUrl);

            routingResult.put("targetUrl", targetUrl);
        }

        // 3. Client 계층에서 즉시 활용할 수 있도록 라우팅 결과 반환
        return routingResult;
    }

    /**
     * 다중 인스턴스(Pod) 환경을 위한 클라이언트 사이드 로드밸런싱 로직 (랜덤 방식)
     *
     * @param availablePods 살아있는 타겟 URL 리스트
     * @return 로드밸런싱을 통해 선택된 단일 URL
     */
    private String selectPodLoadBalanced(List<String> availablePods) {
        if (availablePods.size() == 1) {
            return availablePods.get(0);
        }
        // Java ThreadLocalRandom을 사용하여 동시성 이슈 없이 난수 생성
        int randomIndex = ThreadLocalRandom.current().nextInt(availablePods.size());
        return availablePods.get(randomIndex);
    }
}
