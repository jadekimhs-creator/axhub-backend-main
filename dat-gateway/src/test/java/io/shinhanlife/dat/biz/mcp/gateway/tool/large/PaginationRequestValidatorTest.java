package io.shinhanlife.dat.mcg.tool.large;


/**
 * @package io.shinhanlife.dat.mcg.tool.large
 * @className PaginationRequestValidatorTest
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.shinhanlife.dat.mcg.config.McpGatewayProperties;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.util.Map;
import io.shinhanlife.dat.mcg.resilience.ToolExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaginationRequestValidatorTest {

    private PaginationRequestValidator validator;
    private ObjectMapper json;
    private McpGatewayProperties properties;

    @BeforeEach
    void setUp() {
        properties = new McpGatewayProperties();
        properties.setDefaultPageSize(100);
        properties.setMaxPageSize(500);
        json = new ObjectMapper();
        validator = new PaginationRequestValidator(properties, json);
    }

    @Test
    void shouldSetDefaultPageSizeIfMissing() {
        ObjectNode arguments = json.createObjectNode();
        ObjectNode normalized = validator.normalize(paginationTool(), arguments);
        
        assertEquals(100, normalized.get("pageSize").asInt());
    }

    @Test
    void shouldKeepValidPageSize() {
        ObjectNode arguments = json.createObjectNode();
        arguments.put("pageSize", 200);
        ObjectNode normalized = validator.normalize(paginationTool(), arguments);
        
        assertEquals(200, normalized.get("pageSize").asInt());
    }

    @Test
    void shouldThrowIfPageSizeTooLarge() {
        ObjectNode arguments = json.createObjectNode();
        arguments.put("pageSize", 600);
        
        ToolExecutionException exception = assertThrows(ToolExecutionException.class, () -> {
            validator.normalize(paginationTool(), arguments);
        });
        
        assertTrue(exception.getMessage().contains("PAGE_SIZE_EXCEEDED"));
    }

    @Test
    void shouldThrowIfCursorTooLong() {
        ObjectNode arguments = json.createObjectNode();
        arguments.put("cursor", "a".repeat(3000));
        
        ToolExecutionException exception = assertThrows(ToolExecutionException.class, () -> {
            validator.normalize(paginationTool(), arguments);
        });
        
        assertTrue(exception.getMessage().contains("INVALID_CURSOR"));
    }
    private ToolMetadata paginationTool() {
        return ToolMetadata.builder()
                .parametersSchema(Map.of("properties", Map.of("pageSize", Map.of())))
                .build();
    }
}
