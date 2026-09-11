package io.shinhanlife.glow.communication.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class GlowTrgmFieldContractTest {

    private static class StandardTrgmDto {
        @GlowTrgmField(order = 1, length = 8, decimal = 2, description = "금액", target = "body", type = "gs")
        private String amount;
    }

    @Test
    void exposesTheStandardGlowTrgmFieldMetadata() throws Exception {
        Field field = StandardTrgmDto.class.getDeclaredField("amount");
        GlowTrgmField metadata = field.getAnnotation(GlowTrgmField.class);

        assertEquals(1, metadata.order());
        assertEquals(8, metadata.length());
        assertEquals(2, metadata.decimal());
        assertEquals("금액", metadata.description());
        assertEquals("body", metadata.target());
        assertEquals("gs", metadata.type());
    }
}
