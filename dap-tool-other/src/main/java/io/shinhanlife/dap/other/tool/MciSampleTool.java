package io.shinhanlife.dap.other.tool;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanTelegramWrapper;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanCommonHeaderDto;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanMessageDto;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanMessageDto.MsgHddvValu;
import io.shinhanlife.dap.other.dto.MyBizDataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import io.shinhanlife.dap.other.dto.MciSampleRequestDto;

/**
 * @package io.shinhanlife.dap.other.tool
 * @className MciSampleTool
 * @description AX HUB 시스템 처리 클래스 - MCI 연동 샘플 툴
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
@McpTool(routingType = "MCI", categoryKey = "common")
public class MciSampleTool {

    @McpFunction(displayName = "MCI 샘플 전송", name = "send_mci_sample", description = "MCI 표준 헤더, 메시지부, 데이터부 구조를 갖는 샘플 연동")
    public ShinhanTelegramWrapper<MyBizDataDto> sendMciSample(MciSampleRequestDto params) {
        log.info("▶ [MCI Sample Tool] 요청을 수신했습니다. 전달된 파라미터: {}", params);
        
        ShinhanTelegramWrapper<MyBizDataDto> request = new ShinhanTelegramWrapper<>();
        
        // 1. 공통 헤더부 생성
        ShinhanCommonHeaderDto header = new ShinhanCommonHeaderDto();
        header.setItrIfId(params.getInterfaceId() != null && !params.getInterfaceId().isBlank() ? params.getInterfaceId() : "dummy_header");
        header.setReqRspnScCd("R");
        header.setGlbId(UUID.randomUUID().toString());
        request.setTgrmCmnnhddValu(header);

        // 2. 메시지부 생성
        ShinhanMessageDto msg = new ShinhanMessageDto();
        MsgHddvValu msgHeader = new MsgHddvValu();
        msgHeader.setMsgTnsmTypeCd(params.getMessageType() != null && !params.getMessageType().isBlank() ? params.getMessageType() : "dummy_msg_type");
        msg.setMsgHddvValu(msgHeader);
        request.setTgrmMsdvValu(msg);

        // 3. 데이터부 생성
        MyBizDataDto bizData = new MyBizDataDto();
        bizData.setCustomerName(params.getCustomerName() != null && !params.getCustomerName().isBlank() ? params.getCustomerName() : "홍길동");
        bizData.setRemarks("정상적으로 처리되었습니다. (응답 테스트)");
        request.setTgrmDtdvValu(bizData);

        return request;
    }
}
