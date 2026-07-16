package io.shinhanlife.dap.biz.mcp.tool.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [Glow Framework 통신 환경 설정 클래스]
 * application-glow-local.yml 의 'glow.communication' 하위 설정값들을 
 * 자바 객체(Bean)로 매핑하여 제공합니다.
 */
/**
 * @package io.shinhanlife.dap.biz.mcp.tool.config
 * @className GlowCommunicationProperties
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
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "glow.communication")
public class GlowCommunicationProperties {

    private Common common = new Common();
    private Http http = new Http();
    private Mci mci = new Mci();
    private ExtMci extmci = new ExtMci();
    private Eai eai = new Eai();
    private Websocket websocket = new Websocket();

    @Getter @Setter
    public static class Common {
        private String envType; // 대내표준 헤더의 환경 타입정보 (D, T, P)
    }

    @Getter @Setter
    public static class Http {
        private int connectionTimeout; // 연결 타임아웃 시간 (초 단위)
        private int readTimeout;       // 읽기 타임아웃 시간 (초 단위)
    }

    @Getter @Setter
    public static class Mci {
        private String host;
        private int port;
        private String uri;
        private String receiveUri;
        private int connectionTimeout;
        private int readTimeout;
        private String encoding;
    }

    @Getter @Setter
    public static class ExtMci {
        private String host;
        private int port;
        private String uri;
        private String jsonUri;
        private String receiveUri;
        private int connectionTimeout;
        private int readTimeout;
        private String encoding;
    }

    @Getter @Setter
    public static class Eai {
        private String host;
        private int port;
    }

    @Getter @Setter
    public static class Websocket {
        private String endpoint;
        private String allowedOrigins;
    }
}