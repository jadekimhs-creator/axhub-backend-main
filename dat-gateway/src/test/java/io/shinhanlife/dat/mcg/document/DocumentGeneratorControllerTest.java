package io.shinhanlife.dat.mcg.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shinhanlife.dat.mcc.dto.ToolMetadata;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DocumentGeneratorControllerTest {

    @Mock
    private DocumentGeneratorService service;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentGeneratorController(service)).build();
    }

    @Test
    void downloadsGeneratedProgramWorkbook() throws Exception {
        byte[] workbook = "xlsx-content".getBytes(StandardCharsets.UTF_8);
        when(service.generateProgram(any())).thenReturn(new GeneratedDocument(
                "고객정보 조회 Tool_프로그램정의서_v1.0_20260811.xlsx", workbook));
        DocumentGenerationRequest request = new DocumentGenerationRequest(
                ToolMetadata.builder().uid("tool-uid").name("oth.cmm.customer.detail").build(),
                "1.0", true, true, true);

        mockMvc.perform(post("/mcp/api/v1/admin/documents/program")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(workbook));
    }
}
