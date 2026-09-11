package io.shinhanlife.dat.mcg.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class ScaffoldingControllerToolDraftTest {

    @TempDir
    Path root;

    @Test
    void createsStandalonePodProjectWithoutChangingExistingPodScaffolder() throws Exception {
        createLibraryProject();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String workspacePath = root.toString().replace("\\", "\\\\");

        mockMvc.perform(post("/api/v1/scaffold/pod-new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"moduleName":"dat-was-payment","port":8099,
                                 "author":"tester","workspacePath":"%s"}
                                """.formatted(workspacePath)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dat-was-payment")));

        assertTrue(Files.exists(root.resolve("dat-was-payment/settings.gradle")));
    }

    private void createLibraryProject() throws Exception {
        Path library = root.resolve("dat-lib-datmt");
        Files.createDirectories(library.resolve("gradle/wrapper"));
        Files.writeString(library.resolve("gradlew"), "#!/bin/sh\n");
        Files.writeString(library.resolve("gradlew.bat"), "@echo off\n");
        Files.writeString(library.resolve("gradle/wrapper/gradle-wrapper.jar"), "wrapper");
        Files.writeString(library.resolve("gradle/wrapper/gradle-wrapper.properties"), "distributionUrl=test\n");
    }

    @Test
    void rejectsNewPodProjectWhenTheTargetModuleAlreadyExists() throws Exception {
        Files.createDirectories(root.resolve("dat-was-payment"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String workspacePath = root.toString().replace("\\", "\\\\");

        mockMvc.perform(post("/api/v1/scaffold/pod-new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"moduleName":"dat-was-payment","port":8099,
                                 "workspacePath":"%s"}
                                """.formatted(workspacePath)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    void scaffoldPageKeepsTheNewPodFormSeparateFromTheLegacyPodForm() throws Exception {
        try (var pageStream = getClass().getResourceAsStream("/static/admin/scaffold.html")) {
            String page = new String(java.util.Objects.requireNonNull(pageStream).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(page.contains("id=\"podForm\""));
            assertTrue(page.contains("id=\"newPodForm\""));
            assertTrue(page.contains("/api/v1/scaffold/pod-new"));
            assertTrue(page.contains("Pod Module New"));
        }
    }

    @Test
    void newPodFormProvidesTheSameServerFolderPickerAsTheLegacyPodForm() throws Exception {
        try (var pageStream = getClass().getResourceAsStream("/static/admin/scaffold.html")) {
            String page = new String(java.util.Objects.requireNonNull(pageStream).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(page.contains("id=\"newPodWorkspacePathInput\""));
            assertTrue(page.contains("openServerFolderPicker('newPodWorkspacePathInput')"));
        }
    }

    @Test
    void usesRenamedDasmtWorkspaceByDefault() {
        assertTrue(ScaffoldingController.DEFAULT_WORKSPACE.endsWith("dat-was-dasmt"));
    }

    @Test
    void listsExistingUseCasesForSelectedModuleAndCategory() throws Exception {
        Path useCaseDir = root.resolve("dat-was-sample/src/main/java/io/shinhanlife/dat/mcc/biz/smp/usecase");
        Files.createDirectories(useCaseDir);
        Files.writeString(useCaseDir.resolve("EmployeeSearchUseCase.java"), "interface EmployeeSearchUseCase {}\n");
        Files.writeString(useCaseDir.resolve("Ignored.java"), "class Ignored {}\n");

        String previousUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", root.toString());
        try {
            ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
            assertEquals(java.util.List.of("EmployeeSearchUseCase"), controller.listUseCases("dat-was-sample", "smp", root.toString()));
        } finally {
            System.setProperty("user.dir", previousUserDir);
        }
    }

    @Test
    void groupedToolRequestGeneratesOneUseCase() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ScaffoldingController(builder, new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String moduleName = root.resolve("dat-was-customer").toString().replace("\\", "\\\\");
        String request = """
                {"useCaseName":"Customer","moduleName":"%s","author":"tester","date":"2026.08.12","tools":[
                  {"baseName":"CustomerGuidance","methodName":"searchGuidance","interfaceId":"CTMNILO00007","title":"Customer guidance","description":"Search guidance","group":"cmm","routingType":"MCI","register":false,"clientSystemCode":"NILD","inputFields":[{"name":"customerId","type":"String","description":"Customer ID","example":"C001","required":true}],"outputFields":[]}
                ]}
                """.formatted(moduleName);

        mockMvc.perform(post("/api/v1/scaffold/tool-group")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CustomerUseCase.java")));
    }

    @Test
    void toolDraftEndpointIsAvailable() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(ChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("""
                {"baseName":"CustomerContractStatus","title":"계약 상태 조회","description":"고객번호로 계약 상태를 조회합니다.","categoryKey":"cmm","routingType":"MCI","httpApiName":"contract-status","functionDescription":"고객 계약의 현재 상태를 조회한다.","displayDescription":"고객 계약 상태 조회","whenToUse":"고객번호로 계약 상태 확인을 요청할 때 사용한다.","whenNotToUse":"계약 변경 또는 해지를 요청할 때는 사용하지 않는다.","ioLimits":"고객번호 한 건을 입력받아 계약 상태 한 건을 반환한다.","exampleQueries":["고객 C123의 계약 상태를 알려줘","C123 계약이 정상인지 확인해줘","고객번호 C123 계약 조회해줘"],"tags":["contract","status","search"],"ownerOrg":"MCP_TOOL","inputFields":[{"name":"customerId","type":"String","description":"고객번호","examples":["C123"],"required":true}],"outputFields":[{"name":"resultCode","type":"String","description":"결과 코드","examples":["SUCCESS"],"required":true}]}
                """);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ScaffoldingController(builder, new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/v1/scaffold/tool-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"고객번호로 계약 상태를 조회\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseName").value("CustomerContractStatus"))
                .andExpect(jsonPath("$.routingType").value("MCI"))
                .andExpect(jsonPath("$.functionDescription").value("고객 계약의 현재 상태를 조회한다."))
                .andExpect(jsonPath("$.displayDescription").value("고객 계약 상태 조회"))
                .andExpect(jsonPath("$.whenToUse").value("고객번호로 계약 상태 확인을 요청할 때 사용한다."))
                .andExpect(jsonPath("$.whenNotToUse").value("계약 변경 또는 해지를 요청할 때는 사용하지 않는다."))
                .andExpect(jsonPath("$.ioLimits").value("고객번호 한 건을 입력받아 계약 상태 한 건을 반환한다."))
                .andExpect(jsonPath("$.exampleQueries[0]").value("고객 C123의 계약 상태를 알려줘"))
                .andExpect(jsonPath("$.tags[0]").value("contract"))
                .andExpect(jsonPath("$.ownerOrg").value("MCP_TOOL"))
                .andExpect(jsonPath("$.inputFields[0].name").value("customerId"));

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("\"routingType\":\"MCI\""));
        assertTrue(promptCaptor.getValue().contains("Default to MCI"));
    }

    @Test
    void mciResponseAnalyzeKeepsGlowDescriptionAndUsesAiForTheLlmFieldName() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(ChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("""
                {"mappings":[{"ownerType":"ONBSZ0460_O","sourceName":"prafNo","targetName":"employeeNumber","include":true}]}
                """);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(builder, new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String source = """
                package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;
                public class ONBSZ0460_O {
                    @GlowTrgmField(order = 1, length = 8, description = "인사번호")
                    private String prafNo;
                }
                """;
        String request = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "source", source,
                "model", "cohere/north-mini-code:free"));

        mockMvc.perform(post("/api/v1/scaffold/mci-response/analyze")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.rootClassName").value("ONBSZ0460_O"))
                .andExpect(jsonPath("$.parsed.types[0].fields[0].description").value("인사번호"))
                .andExpect(jsonPath("$.mappings[0].sourceName").value("prafNo"))
                .andExpect(jsonPath("$.mappings[0].targetName").value("employeeNumber"));

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("인사번호"));
        assertTrue(promptCaptor.getValue().contains("prafNo"));
    }

    @Test
    void mciResponseGenerateReturnsResponseAndConverterPreviews() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String source = """
                package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;
                public class ONBSZ0460_O {
                    @GlowTrgmField(order = 1, length = 8, description = "인사번호")
                    private String prafNo;
                }
                """;
        String request = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "source", source,
                "responsePackage", "io.shinhanlife.dat.mcc.biz.pro.dto",
                "responseClassName", "IndividualCustomerDetailInquiryResponse",
                "converterPackage", "io.shinhanlife.dat.mcc.biz.pro.converter",
                "converterClassName", "IndividualCustomerDetailInquiryConverter",
                "mappings", java.util.List.of(java.util.Map.of(
                        "ownerType", "ONBSZ0460_O",
                        "sourceName", "prafNo",
                        "targetName", "employeeNumber",
                        "include", true))));

        mockMvc.perform(post("/api/v1/scaffold/mci-response/generate")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseSource", org.hamcrest.Matchers.containsString(
                        "@Schema(description = \"인사번호\")")))
                .andExpect(jsonPath("$.responseSource", org.hamcrest.Matchers.containsString(
                        "private String employeeNumber;")))
                .andExpect(jsonPath("$.converterSource", org.hamcrest.Matchers.containsString(
                        "@Mapping(source = \"prafNo\", target = \"employeeNumber\")")));
    }

    @Test
    void mciRequestGenerateReturnsRequestAndReverseConverterPreviews() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String source = """
                package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;
                public class ONBSZ0460_I {
                    @GlowTrgmField(order = 1, length = 8, description = "인사번호")
                    private String prafNo;
                }
                """;
        String request = new ObjectMapper().writeValueAsString(java.util.Map.of(
                "source", source,
                "requestPackage", "io.shinhanlife.dat.mcc.biz.pro.dto",
                "requestClassName", "IndividualCustomerDetailInquiryRequest",
                "converterPackage", "io.shinhanlife.dat.mcc.biz.pro.converter",
                "converterClassName", "IndividualCustomerDetailInquiryRequestConverter",
                "mappings", java.util.List.of(java.util.Map.of(
                        "ownerType", "ONBSZ0460_I",
                        "sourceName", "prafNo",
                        "targetName", "employeeNumber",
                        "include", true))));

        mockMvc.perform(post("/api/v1/scaffold/mci-request/generate")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestSource", org.hamcrest.Matchers.containsString(
                        "private String employeeNumber;")))
                .andExpect(jsonPath("$.converterSource", org.hamcrest.Matchers.containsString(
                        "@Mapping(source = \"employeeNumber\", target = \"prafNo\")")));
    }

    @Test
    void mciRequestAnalyzeReturnsAiSuggestedRequestFieldNames() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(ChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("""
                {"mappings":[{"ownerType":"ONBSZ0460_I","sourceName":"prafNo","targetName":"employeeNumber","include":true}]}
                """);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(builder, new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String source = """
                package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;
                public class ONBSZ0460_I {
                    @GlowTrgmField(order = 1, length = 8, description = "인사번호")
                    private String prafNo;
                }
                """;

        mockMvc.perform(post("/api/v1/scaffold/mci-request/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(java.util.Map.of(
                                "source", source,
                                "model", "cohere/north-mini-code:free"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parsed.rootClassName").value("ONBSZ0460_I"))
                .andExpect(jsonPath("$.mappings[0].targetName").value("employeeNumber"));
    }

    @Test
    void podDraftUsesTheCurrentTargetModuleOptionsForConfusableServers() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(ChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("""
                mcp:
                  manifest:
                    routing-functions:
                      - name: route_to_dat-was-pro
                        server-id: dat-was-pro
                        category-key: pro
                        confusable-servers: [dat-was-hrd, dat-was-pay, dat-was-att]
                """);

        var response = new ScaffoldingController(builder, new ObjectMapper()).generatePodManifestDraft(java.util.Map.of(
                "description", "상품 업무를 처리합니다.",
                "moduleName", "dat-was-pro",
                "targetModules", "[\"dat-was-cus\",\"dat-was-hr\",\"dat-was-sal\",\"dat-was-pro\",\"dat-was-sys\"]"));

        assertEquals(org.springframework.http.HttpStatus.OK, response.getStatusCode());
        String manifest = (String) ((java.util.Map<?, ?>) response.getBody()).get("toolServiceManifest");
        List<String> confusableServers = manifest.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("- \"dat-was-"))
                .map(line -> line.substring(3, line.length() - 1))
                .toList();
        assertEquals(List.of("dat-was-cus", "dat-was-hr", "dat-was-sal", "dat-was-sys"), confusableServers);
        assertFalse(manifest.contains("dat-was-hrd"), manifest);
        assertFalse(manifest.contains("dat-was-pay"), manifest);
        assertFalse(manifest.contains("dat-was-att"), manifest);
    }
}
