package io.shinhanlife.dap.common.adapter.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dap.common.adapter.exception.MciCommunicationException;
import io.shinhanlife.dap.common.adapter.sender.EimsSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @package io.shinhanlife.dap.common.adapter.support
 * @className MciTemplate
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
public class MciTemplate {

    private final EimsSender httpEimsSender;
    private final ObjectMapper objectMapper;

    /**
     * MCI 인터페이스를 호출하고 결과를 지정된 타입으로 반환합니다.
     *
     * @param interfaceId 호출할 MCI 인터페이스 ID
     * @param requestDto 요청 데이터 객체
     * @param responseType 응답을 매핑할 클래스 타입
     * @param <T> 요청 객체 타입
     * @param <R> 응답 객체 타입
     * @return 매핑된 응답 객체
     * @throws MciCommunicationException 통신 또는 파싱 실패 시 예외 발생
     */
    public <T, R> R call(String interfaceId, T requestDto, Class<R> responseType) {
        log.info(" [MciTemplate] 시작 - Interface ID: {}", interfaceId);

        try {
            String payload = objectMapper.writeValueAsString(requestDto);
            log.debug(" [MciTemplate] 전송 페이로드: {}", payload);

            String responseJson = httpEimsSender.send(interfaceId, payload);
            log.debug(" [MciTemplate] 수신 응답 JSON: {}", responseJson);

            R response = objectMapper.readValue(responseJson, responseType);
            log.info(" [MciTemplate] 완료 - Interface ID: {}", interfaceId);

            return response;

        } catch (JsonProcessingException e) {
            log.error(" [MciTemplate] JSON 변환 중 오류 발생", e);
            throw new MciCommunicationException("MCI 통신 중 JSON 파싱 오류", e);
        } catch (Exception e) {
            log.error(" [MciTemplate] 통신 중 오류 발생", e);
            throw new MciCommunicationException("MCI 통신 실패", e);
        }
    }
}
