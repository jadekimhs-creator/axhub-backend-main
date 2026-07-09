package io.shinhanlife.axhub.biz.mcp.adapter.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @package io.shinhanlife.axhub.biz.mcp.adapter.connector
 * @className ThirdPartySecurityConnector
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
@Service
@RequiredArgsConstructor
public class ThirdPartySecurityConnector {

    private final ObjectMapper jsonMapper;

    /**
     * 3rd Party 보안 모듈(DRM, BM 등)과 연동하기 위한 전용 메서드입니다.
     */
    public String executeSecurityModule(String interfaceId, Map<String, Object> data) throws Exception {
        log.info("🔐 [3rd Party Security] 보안 모듈 연동 시작 - Interface: {}", interfaceId);

        // 보안 모듈 통신을 위한 특수 페이로드 조립 (예시)
        // 실제로는 RestClient나 WebClient를 통해 보안 VM의 전용 엔드포인트로 호출합니다.
        
        String resultJson;
        
        if (interfaceId.startsWith("DRM_")) {
            log.info(" [DRM 처리] 내부 문서 암/복호화 모듈과 통신 중...");
            resultJson = jsonMapper.writeValueAsString(Map.of(
                "status", "SUCCESS",
                "module", "DRM",
                "message", "문서 보안 처리가 완료되었습니다.",
                "interfaceId", interfaceId,
                "data", data != null ? data : Map.of()
            ));
        } else if (interfaceId.startsWith("BM_")) {
            log.info("👆 [BM 처리] 바이오 인증 모듈과 통신 중...");
            resultJson = jsonMapper.writeValueAsString(Map.of(
                "status", "SUCCESS",
                "module", "Bio-Metric",
                "message", "바이오 인증이 완료되었습니다.",
                "interfaceId", interfaceId,
                "data", data != null ? data : Map.of()
            ));
        } else {
            log.warn(" 알 수 없는 보안 모듈 연동 요청: {}", interfaceId);
            resultJson = jsonMapper.writeValueAsString(Map.of(
                "status", "UNKNOWN_MODULE",
                "message", "알 수 없는 보안 모듈 인터페이스입니다."
            ));
        }

        log.info(" [3rd Party Security] 보안 모듈 처리 완료");
        return resultJson;
    }
}