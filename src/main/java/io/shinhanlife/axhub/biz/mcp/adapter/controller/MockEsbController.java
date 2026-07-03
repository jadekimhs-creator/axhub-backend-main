// [MockEsbController.java] - 로컬 테스트용 가짜 ESB 서버
package io.shinhanlife.axhub.biz.mcp.adapter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mock/esb")
public class MockEsbController {

    // MCI (RestClient)가 이 주소로 실제 HTTP 요청을 보냅니다.
    @PostMapping(value = "/api", consumes = MediaType.APPLICATION_XML_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public String mockMciResponse(@RequestBody String requestXml) {
        log.info("[가짜 ESB 서버] MCI 실시간 요청 수신 완료!\n받은 데이터: {}", requestXml);

        // ESB 서버가 응답하는 것처럼 가짜 XML 전문을 리턴합니다.
        return "<EsbMessage><Header><Status>SUCCESS</Status></Header><Body><Message>정상 조회되었습니다.</Message></Body></EsbMessage>";
    }
}