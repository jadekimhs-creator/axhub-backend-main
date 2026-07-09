package io.shinhanlife.axhub.biz.mcp.sample.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.axhub.biz.mcp.adapter.sender.EimsSender;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleMciReqDto;
import io.shinhanlife.axhub.biz.mcp.sample.dto.SampleMciResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @package io.shinhanlife.axhub.biz.mcp.sample.service
 * @className SampleMciCallService
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
public class SampleMciCallService {

    private final EimsSender eimsSender;
    private final ObjectMapper objectMapper;

    /**
     * MCI 인터페이스를 호출하는 샘플 메서드
     * 
     * @param request MCI 호출을 위한 요청 데이터
     * @return MCI 서버의 응답 데이터
     */
    public SampleMciResDto callSampleMci(SampleMciReqDto request) {
        log.info("📞 [MciCall] 시작 - Interface ID: {}", request.getInterfaceId());

        try {
            // 1. 요청 객체(DTO)를 JSON 문자열로 변환 (직렬화)
            String payload = objectMapper.writeValueAsString(request);
            log.debug("📤 [MciCall] 전송 페이로드: {}", payload);

            // 2. HTTP/2 기반의 EimsSender를 통해 실제 MCI 호출
            String responseJson = eimsSender.send(request.getInterfaceId(), payload);
            log.debug("📥 [MciCall] 수신 응답 JSON: {}", responseJson);

            // 3. 수신받은 JSON 문자열을 응답 객체(DTO)로 역직렬화 후 반환
            SampleMciResDto response = objectMapper.readValue(responseJson, SampleMciResDto.class);
            log.info("✅ [MciCall] 완료 - 응답 코드: {}", response.getResCode());
            
            return response;
            
        } catch (JsonProcessingException e) {
            log.error("❌ [MciCall] JSON 변환 중 오류 발생", e);
            throw new RuntimeException("MCI 통신 중 JSON 파싱 오류", e);
        } catch (Exception e) {
            log.error("❌ [MciCall] 통신 중 오류 발생", e);
            throw new RuntimeException("MCI 통신 실패", e);
        }
    }
}