package io.shinhanlife.dap.mcc.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.WeatherReq;
import io.shinhanlife.dap.mcc.dto.WeatherRes;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @package io.shinhanlife.dap.mcc.service
 * @className WeatherToolService
 * @description AX HUB 시스템 처리 클래스
 * @author 김형식
 * @create 2026.07.14
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.07.14  김형식    최초생성
 * 
 * </pre>
 */
@Slf4j
@Service
@McpTool(
    routingType = "DIRECT",
    categoryKey = "common"
)
public class WeatherToolService extends AbstractMcpToolService {

    private final RestClient restClient;

    public WeatherToolService() {
        this.restClient = RestClient.create();
    }

    @McpFunction(displayName = "날씨 조회 툴", name = "get_current_weather",
        description = "특정 도시의 실시간 날씨(기온, 풍속 등)를 오픈 기상 API를 통해 조회합니다.",
        prompt = "서울 날씨 알려줘, 부산 기온 알려줘 등 실시간 기상 조회",
        mappingId = "WEATHER_001"
    )
    public WeatherRes execute(WeatherReq req) {
        String city = req.city() != null ? req.city().trim() : "서울";
        
        // 지역별 위경도 매핑 (간단한 예시)
        double lat = 37.566;
        double lon = 126.978;
        
        if (city.contains("부산")) {
            lat = 35.179;
            lon = 129.075;
        } else if (city.contains("제주")) {
            lat = 33.499;
            lon = 126.531;
        } else if (city.contains("인천")) {
            lat = 37.456;
            lon = 126.705;
        }

        try {
            String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true", lat, lon);
            
            log.info("[WeatherTool] 날씨 조회 요청 URL: {}", url);
            
            String responseStr = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode response = mapper.readTree(responseStr);

            if (response != null && response.has("current_weather")) {
                JsonNode current = response.get("current_weather");
                double temp = current.path("temperature").asDouble();
                double windSpeed = current.path("windspeed").asDouble();
                String time = current.path("time").asText();
                
                int weatherCode = current.path("weathercode").asInt();
                String summary = parseWeatherCode(weatherCode);

                String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                return new WeatherRes(city, temp, windSpeed, formattedTime, summary);
            }
        } catch (Exception e) {
            log.error("[WeatherTool] 날씨 API 연동 실패: {}", e.getMessage());
            return new WeatherRes(city, 0.0, 0.0, "", "날씨 정보를 불러오는데 실패했습니다.");
        }
        
        return new WeatherRes(city, 0.0, 0.0, "", "알 수 없는 응답입니다.");
    }

    private String parseWeatherCode(int code) {
        if (code == 0) return "맑음 (Clear)";
        if (code >= 1 && code <= 3) return "구름조금/흐림 (Cloudy)";
        if (code >= 45 && code <= 48) return "안개 (Fog)";
        if (code >= 51 && code <= 67) return "비/이슬비 (Rain)";
        if (code >= 71 && code <= 77) return "눈 (Snow)";
        if (code >= 95) return "뇌우/천둥번개 (Thunderstorm)";
        return "알 수 없음 (Unknown)";
    }
}
