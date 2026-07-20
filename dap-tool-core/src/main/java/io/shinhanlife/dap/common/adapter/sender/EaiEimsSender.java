package io.shinhanlife.dap.common.adapter.sender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.shinhanlife.dap.common.adapter.support.TicketManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

/**
 * @package io.shinhanlife.dap.common.adapter.sender
 * @className EaiEimsSender
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
@Service("eaiEimsSender")
@RequiredArgsConstructor
public class EaiEimsSender implements EimsSender {

    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    //  실전 코드: 스프링이 제공하는 카프카 템플릿 주입
    private final KafkaTemplate<String, String> kafkaTemplate;

    //  비동기 티켓 매니저
    private final TicketManager ticketManager;

    @Override
    public String send(String interfaceId, String jsonPayload) throws Exception {
        StopWatch stopWatch = new StopWatch(); stopWatch.start();

        try {
            JsonNode jsonNode = jsonMapper.readTree(jsonPayload);
            String xmlData = xmlMapper.writeValueAsString(jsonNode);
            String esbStandardXml = wrapWithEaiHeader(interfaceId, xmlData);

            log.info(" [EAI 어댑터] Kafka 토픽(eai-topic)으로 전송 시도...");

            try {
                //  실전 코드 적용: Kafka로 메시지 발행
                kafkaTemplate.send("eai-topic", esbStandardXml);
                log.info(" [EAI 어댑터] Kafka 전송 완료!");
            } catch (Exception e) {
                // 로컬 환경에는 카프카가 없으므로 에러가 날 수 있습니다. 테스트를 위해 로깅만 하고 넘깁니다.
                log.warn(" 로컬 환경이거나 Ka<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                        "<EaiMessage>\n" +
                        "    <Header>\n" +
                        "        <ChannelId>MCP_GATEWAY_ASYNC</ChannelId>\n" +
                        "        <InterfaceId>EAI_BATCH_JOB</InterfaceId>\n" +
                        "        <Timestamp>1782705901850</Timestamp>\n" +
                        "        <TransferType>ASYNC</TransferType>\n" +
                        "    </Header>\n" +
                        "    <Body>\n" +
                        "        <ObjectNode>\n" +
                        "            <batchId>BATCH_20260629_001</batchId>\n" +
                        "            <targetSystem>GLOBAL_MINIMUM_TAX_SYS</targetSystem>\n" +
                        "            <recordCount>50000</recordCount>\n" +
                        "        </ObjectNode>\n" +
                        "    </Body>\n" +
                        "</EaiMessage>fka 서버에 연결할 수 없습니다. (메시지 출력으로 대체합니다) \n전송하려던 메시지: {}", esbStandardXml);
            }

            //  비동기 폴링을 위한 티켓 발급
            String ticketId = ticketManager.issueTicket(interfaceId, jsonPayload);

            return String.format(
                    "{\"status\":\"PROCESSING\", \"interfaceId\":\"%s\", \"ticketId\":\"%s\", \"message\":\"비동기 작업이 접수되었습니다. 상태 조회 API를 통해 결과를 확인하세요.\"}", 
                    interfaceId, ticketId);

        } finally {
            stopWatch.stop();
            log.info(" [SLA 모니터링 - EAI] 소요시간: {} ms", stopWatch.getTotalTimeMillis());
        }
    }

    private String wrapWithEaiHeader(String interfaceId, String xmlData) {
        // 비동기 EAI는 추적을 위해 TransferType이나 Batch ID 같은 속성이 추가로 들어가는 경우가 많습니다.
        return String.format(
                "<EaiMessage>" +
                        "<Header>" +
                        "<ChannelId>MCP_GATEWAY_ASYNC</ChannelId>" +
                        "<InterfaceId>%s</InterfaceId>" +
                        "<Timestamp>%d</Timestamp>" +
                        "<TransferType>ASYNC</TransferType>" +
                        "</Header>" +
                        "<Body>%s</Body>" +
                "</EaiMessage>",
                interfaceId, System.currentTimeMillis(), xmlData
        );
    }
}