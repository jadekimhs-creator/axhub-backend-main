package io.shinhanlife.glow.util;

import io.shinhanlife.glow.GlowMciFieldInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlowMciParserTest {

    public static class DummyMciDto {
        @GlowMciFieldInfo(order = 1, length = 10)
        private String customerId;

        @GlowMciFieldInfo(order = 2, length = 15)
        private String name;

        @GlowMciFieldInfo(order = 3, length = 3)
        private int age;

        public String getCustomerId() { return customerId; }
        public String getName() { return name; }
        public int getAge() { return age; }
    }

    @Test
    @DisplayName("고정 길이 MCI 문자열을 DTO로 파싱하는 테스트")
    public void testParseFixedLengthString() {
        // given: 고정 길이 텍스트 (총 28자리)
        // ID(10) + Name(15) + Age(3)
        String rawMciString = "CUST000001KIM SHINHAN    035";
        
        // when
        DummyMciDto result = GlowMciParser.parse(rawMciString, DummyMciDto.class);

        // then
        System.out.println("==================================================");
        System.out.println(" [원본 MCI 전문] : [" + rawMciString + "]");
        System.out.println(" [파싱된 ID (10자리)] : [" + result.getCustomerId() + "]");
        System.out.println(" [파싱된 Name (15자리)] : [" + result.getName() + "]");
        System.out.println(" [파싱된 Age (3자리)] : [" + result.getAge() + "]");
        System.out.println("==================================================");

        assertNotNull(result);
        assertEquals("CUST000001", result.getCustomerId());
        assertEquals("KIM SHINHAN", result.getName());
        assertEquals(35, result.getAge());
    }
}
