package io.shinhanlife.axhub.common.mcp.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  [다중 테넌트 설정 매핑 클래스]
 * application-local.properties 파일에 정의된 mcp.security.api-keys.* 설정들을
 * Map 자료구조로 자동 바인딩(주입) 받기 위한 설정 클래스입니다.
 */
/**
 * @package io.shinhanlife.axhub.common.mcp.security
 * @className SecurityProperties
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
@Data
@Component
@ConfigurationProperties(prefix = "mcp.security")
public class SecurityProperties {
    // API Key를 Key로, Tenant ID를 Value로 가지는 맵 
    private Map<String, String> apiKeys = new HashMap<>();

    // Tenant ID를 Key로, 허용된 도메인 그룹 목록을 Value로 가지는 맵 (ex. mcp-client-1 -> [CUSTOMER, COMMON])
    // 만약 "ALL" 이 포함되어 있다면 모든 도메인에 접근 허용
    private Map<String, List<String>> tenantDomains = new HashMap<>();
}
