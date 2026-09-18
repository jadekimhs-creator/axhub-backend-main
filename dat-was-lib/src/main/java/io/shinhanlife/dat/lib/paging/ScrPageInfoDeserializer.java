package io.shinhanlife.dat.lib.paging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.shinhanlife.glow.db.dto.ScrPageInfo;

import java.io.IOException;

/**
 * @package io.shinhanlife.dat.lib.paging
 * @className ScrPageInfoDeserializer
 * @description AX HUB 시스템 처리 클래스
 * @author 0986406
 * @create 2026.09.01
 * <pre>
 * ---------- 개정이력 ----------
 * 수정일      수정자    수정내용
 * ---------- -------- ---------------------------
 * 2026.09.01  0986406    최초생성
 * 
 * </pre>
 */
public class ScrPageInfoDeserializer extends JsonDeserializer<ScrPageInfo> {

    @Override
    public ScrPageInfo deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }

        // 빈 문자열("")로 넘어온 경우
        if (node.isTextual() && node.asText().isBlank()) {
            return null;
        }

        // MCI 전문 응답처럼 배열([ { ... } ]) 형태로 넘어온 경우 첫 번째 객체 꺼내기
        if (node.isArray()) {
            if (node.isEmpty()) {
                return null;
            }
            node = node.get(0);
        }

        if (node == null || node.isNull()) {
            return null;
        }

        ScrPageInfo info = new ScrPageInfo();

        if (node.hasNonNull("scrImhdNm")) {
            info.setScrImhdNm(node.get("scrImhdNm").asText());
        } else if (node.hasNonNull("scrlmhdNm")) {
            info.setScrImhdNm(node.get("scrlmhdNm").asText());
        }

        if (node.hasNonNull("scrItva")) {
            info.setScrItva(node.get("scrItva").asText());
        } else if (node.hasNonNull("scrlItva")) {
            info.setScrItva(node.get("scrlItva").asText());
        }

        if (node.hasNonNull("scrSortValu")) {
            info.setScrSortValu(node.get("scrSortValu").asText());
        }

        if (node.hasNonNull("nextDataExtYn")) {
            info.setNextDataExtYn(node.get("nextDataExtYn").asText());
        }

        if (node.hasNonNull("pageDataCc")) {
            info.setPageDataCc(node.get("pageDataCc").asInt());
        }

        return info;
    }
}
