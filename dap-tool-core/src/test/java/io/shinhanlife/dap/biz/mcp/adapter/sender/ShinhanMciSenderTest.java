package io.shinhanlife.dap.biz.mcp.adapter.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dap.common.integration.mci.dto.MciRequestWrapper;
import io.shinhanlife.dap.common.integration.mci.dto.ShinhanCommonHeaderDto;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @package io.shinhanlife.dap.biz.mcp.adapter.sender
 * @className ShinhanMciSenderTest
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
class ShinhanMciSenderTest {

    @Data
    static class SampleBody {
        private String msgCd;
        private String anxMsgCt;
    }

    @Test
    void testJsonUnwrapped() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ShinhanCommonHeaderDto header = new ShinhanCommonHeaderDto();
        header.setGlbId("20211115150135679009808815NCS17007887");
        header.setPgrsSriaNo("002");
        header.setItrIfId("NCSBACO00001");

        SampleBody body = new SampleBody();
        body.setMsgCd("12345");
        body.setAnxMsgCt("Test Message");

        MciRequestWrapper<SampleBody> wrapper = new MciRequestWrapper<>();
        wrapper.setTgrmCmnnhddValu(header);
        wrapper.setBody(body);

        String json = mapper.writeValueAsString(wrapper);

        System.out.println(json);

        // 검증: body 필드가 json root 레벨에 평탄화되어 있는지 확인
        assertThat(json).contains("\"tgrmCmnnhddValu\":{");
        assertThat(json).contains("\"msgCd\":\"12345\"");
        assertThat(json).contains("\"anxMsgCt\":\"Test Message\"");
        assertThat(json).doesNotContain("\"body\":");
    }
}
