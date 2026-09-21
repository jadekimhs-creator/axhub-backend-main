package io.shinhanlife.dat.mcg.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void returnsConfigurationErrorInsteadOfOpenRouter401WhenExternalAiKeyIsMissing() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/v1/scaffold/tool-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"고객 계약을 조회한다\",\"model\":\"cohere/north-mini-code:free\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("OPENROUTER_API_KEY")));
    }

    @Test
    void acceptsAllFiveOpenRouterModelsForToolDraftGeneration() {
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
        List<String> models = List.of(
                "cohere/north-mini-code:free",
                "inclusionai/ling-3.0-flash:free",
                "openai/gpt-oss-20b:free",
                "google/gemma-4-31b-it:free",
                "nvidia/nemotron-3-nano-30b-a3b:free");

        for (String model : models) {
            assertEquals(model, ReflectionTestUtils.invokeMethod(controller, "resolveModel", model));
        }
    }

    @Test
    void acceptsShinhanInternalAiModelsIncludingGlmFlash() {
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
        List<String> internalModels = List.of("GLM-5.3-Flash", "Qwen3-Coder", "Gemma-4-31B");

        for (String model : internalModels) {
            assertEquals(model, ReflectionTestUtils.invokeMethod(controller, "resolveModel", model));
        }
    }

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
        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
                {"baseName":"DetailContractStatus","title":"계약 상태 조회","description":"고객번호로 계약 상태를 조회합니다.","categoryKey":"cmm","routingType":"MCI","httpApiName":"contract-status","functionDescription":"고객 계약의 현재 상태를 조회한다.","displayDescription":"고객 계약 상태 조회","whenToUse":"고객번호로 계약 상태 확인을 요청할 때 사용한다.","whenNotToUse":"계약 변경 또는 해지를 요청할 때는 사용하지 않는다.","ioLimits":"고객번호 한 건을 입력받아 계약 상태 한 건을 반환한다.","exampleQueries":["고객 C123의 계약 상태를 알려줘","C123 계약이 정상인지 확인해줘","고객번호 C123 계약 조회해줘"],"tags":["cmm","계약","상태","계약조회","contract","status"],"ownerOrg":"MCP_TOOL","inputFields":[{"name":"customerId","type":"String","description":"고객번호","examples":["C123"],"required":true}],"outputFields":[{"name":"resultCode","type":"String","description":"결과 코드","examples":["SUCCESS"],"required":true}]}
                """);
        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        mockMvc.perform(post("/api/v1/scaffold/tool-draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"고객번호로 계약 상태를 조회\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseName").value("DetailContractStatus"))
                .andExpect(jsonPath("$.routingType").value("MCI"))
                .andExpect(jsonPath("$.functionDescription").value("고객 계약의 현재 상태를 조회한다."))
                .andExpect(jsonPath("$.displayDescription").value("고객 계약 상태 조회"))
                .andExpect(jsonPath("$.whenToUse").value("고객번호로 계약 상태 확인을 요청할 때 사용한다."))
                .andExpect(jsonPath("$.whenNotToUse").value("계약 변경 또는 해지를 요청할 때는 사용하지 않는다."))
                .andExpect(jsonPath("$.ioLimits").value("고객번호 한 건을 입력받아 계약 상태 한 건을 반환한다."))
                .andExpect(jsonPath("$.exampleQueries[0]").value("고객 C123의 계약 상태를 알려줘"))
                .andExpect(jsonPath("$.tags[0]").value("cmm"))
                .andExpect(jsonPath("$.ownerOrg").value("MCP_TOOL"))
                .andExpect(jsonPath("$.inputFields[0].name").value("customerId"));

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("\"routingType\":\"MCI\""));
        assertTrue(promptCaptor.getValue().contains("Default to MCI"));
    }

    @Test
    void toolDraftOptimizeEndpointEnforcesV17RulesAndKeepsImmutableFields() throws Exception {
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
                {"baseName":"pro_search_variable","title":"AI가 바꾼 제목","description":"[pro] 변액보험 펀드 가이드 정보를 조회합니다. 계약번호를 모를 경우 계약조회 툴을 먼저 호출해야 합니다. 기준일자 미입력시 당일로 처리됩니다.","categoryKey":"pro","routingType":"MCI","httpApiName":"variable-fund","functionDescription":"[pro] 변액보험 펀드 가이드 조회","displayDescription":"AI가 바꾼 화면명","whenToUse":"고객이 변액보험 펀드 가이드 및 상품유형별 투자 가이드 조회를 요청할 때 사용한다.","whenNotToUse":"변액보험 펀드 변경이나 신청 등의 변경 업무에는 사용하지 않는다.","ioLimits":"상품유형코드 한 건을 입력받아 가이드 목록을 반환한다. 미입력시 기본 1년치 조회.","exampleQueries":["[NSAK0060] 펀드가이드 상품유형별 투자 가이드를 보여줘","변액보험 펀드 투자 가이드 조회해줘","상품유형별 펀드가이드 확인"],"tags":["pro","펀드가이드","투자가이드","variable","fundGuide"],"ownerOrg":"MCP_TOOL","inputFields":[{"name":"productTypeCode","type":"String","description":"상품유형코드","examples":["A01"],"required":true},{"name":"scrPageInfo","type":"String","description":"","examples":[],"required":true}],"outputFields":[{"name":"resultCode","type":"String","description":"결과 코드","examples":["SUCCESS"],"required":true}]}
                """);
        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        String originalTitle = "펀드가이드 상품유형별 투자이드 관리";
        String originalDisplayDesc = "[NSAK0060]펀드가이드 상품유형별 투자이드 관리";

        String requestBody = """
                {
                  "baseName": "pro_search_variable",
                  "title": "%s",
                  "description": "변액 펀드 조회",
                  "categoryKey": "pro",
                  "displayDescription": "%s"
                }
                """.formatted(originalTitle, originalDisplayDesc);

        mockMvc.perform(post("/api/v1/scaffold/tool-draft/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseName").value("SearchVariable"))
                .andExpect(jsonPath("$.title").value(originalTitle))
                .andExpect(jsonPath("$.displayDescription").value(originalDisplayDesc))
                .andExpect(jsonPath("$.categoryKey").value("pro"))
                .andExpect(jsonPath("$.description", org.hamcrest.Matchers.containsString("[pro]")))
                .andExpect(jsonPath("$.exampleQueries[0]", org.hamcrest.Matchers.containsString("NSAK0060")))
                .andExpect(jsonPath("$.tags[0]").value("pro"))
                .andExpect(jsonPath("$.inputFields[0].name").value("productTypeCode"))
                .andExpect(jsonPath("$.inputFields[1].name").value("scrPageInfo"))
                .andExpect(jsonPath("$.inputFields[1].required").value(false))
                .andExpect(jsonPath("$.inputFields[1].description").value("페이징 처리 객체 (생략 시 시스템 기본값 적용)"));

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("CRITICAL V17 OPTIMIZATION RULES - 6 GUIDELINE PRINCIPLES"));
        assertTrue(promptCaptor.getValue().contains("공개 이름 / Java Base Name 분리"));
        assertTrue(promptCaptor.getValue().contains("search"));
        assertTrue(promptCaptor.getValue().contains(originalDisplayDesc));
        assertTrue(promptCaptor.getValue().contains("idempotent=true"));
        assertTrue(promptCaptor.getValue().contains("Java Base Name excludes categoryKey"));
    }

    @Test
    void rejectsToolDraftsThatViolateV17NamingAndMetadataCardinality() {
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(controller,
                "validateToolDraft", validToolDraft("ProInquiryFund", List.of("질문 하나", "질문 둘", "질문 셋"),
                        List.of("pro", "펀드", "조회", "fund", "search"))));
        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(controller,
                "validateToolDraft", validToolDraft("ProSearchFund", List.of("질문 하나", "질문 둘"),
                        List.of("pro", "펀드", "조회", "fund", "search"))));
        assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(controller,
                "validateToolDraft", validToolDraft("ProSearchFund", List.of("질문 하나", "질문 둘", "질문 셋"),
                        List.of("pro", "펀드"))));
    }

    private ScaffoldingController.ToolDraft validToolDraft(String baseName, List<String> exampleQueries, List<String> tags) {
        return new ScaffoldingController.ToolDraft(baseName, "펀드 조회", "[pro] 펀드를 조회한다.", "pro", "MCI",
                "", "[pro] 펀드 조회", "[NSAK0060] 펀드 조회", "펀드를 조회할 때 사용한다.",
                "변경 요청에는 사용하지 않는다.", "정의된 입력만 허용한다.", exampleQueries, tags,
                "MCP_TOOL", List.of(), List.of());
    }

    @Test
    void scaffoldPageContainsV17OptimizationButtonsAndScript() throws Exception {
        try (var pageStream = getClass().getResourceAsStream("/static/admin/scaffold.html")) {
            String page = new String(java.util.Objects.requireNonNull(pageStream).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(page.contains("id=\"toolOptimizeButton\""));
            assertTrue(page.contains("optimizeAiToolDraft()"));
            assertTrue(page.contains("/api/v1/scaffold/tool-draft/optimize"));
        }
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
        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
    void mciAnalyzePreservesScrPageInfoAndPageInfoEvenIfAiSuggestsDifferentName() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(ChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        // Simulate AI trying to rename scrPageInfo and pageInfo
        when(responseSpec.content()).thenReturn("""
                {"mappings":[
                    {"ownerType":"ONBSZ0460_O","sourceName":"scrPageInfo","targetName":"scrollPagingInfo","include":true},
                    {"ownerType":"ONBSZ0460_O","sourceName":"pageInfo","targetName":"pagingDetails","include":true}
                ]}
                """);
        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        String source = """
                package io.shinhanlife.dat.mcc.infra.itrf.mci.onbsz.io;
                import io.shinhanlife.glow.db.dto.PageInfo;
                import io.shinhanlife.glow.db.dto.ScrPageInfo;
                public class ONBSZ0460_O {
                    @GlowTrgmField(order = 1, length = 20, description = "페이지정보")
                    private PageInfo pageInfo;
                    @GlowTrgmField(order = 2, length = 20, description = "스크롤페이지정보")
                    private ScrPageInfo scrPageInfo;
                }
                """;

        mockMvc.perform(post("/api/v1/scaffold/mci-response/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(java.util.Map.of(
                                "source", source,
                                "model", "cohere/north-mini-code:free"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappings[0].sourceName").value("pageInfo"))
                .andExpect(jsonPath("$.mappings[0].targetName").value("pageInfo"))
                .andExpect(jsonPath("$.mappings[1].sourceName").value("scrPageInfo"))
                .andExpect(jsonPath("$.mappings[1].targetName").value("scrPageInfo"));
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

        ScaffoldingController controller = new ScaffoldingController(builder, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "openRouterApiKey", "test-openrouter-key");
        var response = controller.generatePodManifestDraft(java.util.Map.of(
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

    @Test
    void validatesHttpToolDraftWithAbbreviatedHttpApiNameAndBaseName() {
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
        ScaffoldingController.ToolDraft draft = new ScaffoldingController.ToolDraft(
                "SearchEmployee", "직원 조회", "직원 정보를 조회한다", "cmm", "HTTP", "employee-search-detail",
                null, null, null, null, null, List.of(), List.of(), null, List.of(), List.of());

        ScaffoldingController.ToolDraft validated = ReflectionTestUtils.invokeMethod(controller, "validateToolDraft", draft);
        assertEquals("srch-emp", validated.httpApiName());
        assertEquals("SrchEmp", validated.baseName());
        assertTrue(validated.httpApiName().length() < 10);
    }

    @Test
    void defaultsInterfaceIdForHttpScaffoldWhenOmitted() throws Exception {
        createLibraryProject();
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
        String workspacePath = root.toString().replace("\\", "/");

        java.util.Map<String, String> req = new java.util.HashMap<>();
        req.put("baseName", "CustomerNotice");
        req.put("title", "고객 공지 조회");
        req.put("description", "공지사항을 조회한다");
        req.put("categoryKey", "cmm");
        req.put("routingType", "HTTP");
        req.put("moduleName", "dat-was-cmm");
        req.put("workspacePath", workspacePath);
        // interfaceId explicitly omitted

        String result = controller.scaffoldTool(req);
        assertTrue(result.contains("UseCase"), "Expected success message with UseCase, but got: " + result);
        Path usecaseInterface = Files.walk(root)
                .filter(p -> p.toString().endsWith("UseCase.java") && !p.toString().endsWith("UseCaseImpl.java"))
                .findFirst()
                .orElseThrow();
        String usecaseContent = Files.readString(usecaseInterface);
        assertTrue(usecaseContent.contains("mappingId = \"HTTP0000001\""), usecaseContent);
    }

    @Test
    void defaultsInterfaceIdForGroupedHttpToolWhenOmitted() throws Exception {
        createLibraryProject();
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
        String workspacePath = root.toString().replace("\\", "/");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        String payload = """
                {
                  "useCaseName": "NoticeGroup",
                  "moduleName": "dat-was-cmm",
                  "author": "tester",
                  "date": "2026.09.18",
                  "workspacePath": "%s",
                  "tools": [
                    {
                      "baseName": "NoticeItem",
                      "methodName": "searchNotice",
                      "title": "Notice item",
                      "description": "Search notice",
                      "group": "cmm",
                      "routingType": "HTTP",
                      "httpApiName": "not-item"
                    }
                  ]
                }
                """.formatted(workspacePath);

        mockMvc.perform(post("/api/v1/scaffold/tool-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Multi Tool Scaffolding Complete")));

        Path usecaseInterface = Files.walk(root)
                .filter(p -> p.toString().endsWith("NoticeGroupUseCase.java"))
                .findFirst()
                .orElseThrow();
        String usecaseContent = Files.readString(usecaseInterface);
        assertTrue(usecaseContent.contains("mappingId = \"HTTP0000001\""), usecaseContent);
    }

    @Test
    void abbreviatesHttpUseCaseAndOmitsRedundantConverterAndEnsuresYamlMci() throws Exception {
        createLibraryProject();
        ScaffoldingController controller = new ScaffoldingController(mock(ChatClient.Builder.class), new ObjectMapper());
        String workspacePath = root.toString().replace("\\", "/");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        String payload = """
                {
                  "useCaseName": "CusInquiryContractStatus",
                  "moduleName": "dat-was-cus",
                  "author": "tester",
                  "date": "2026.09.18",
                  "workspacePath": "%s",
                  "tools": [
                    {
                      "baseName": "CusInquiryContractStatus",
                      "methodName": "inquireContractStatus",
                      "title": "Inquire Contract Status",
                      "description": "Inquire contract status",
                      "group": "cus",
                      "routingType": "HTTP",
                      "httpApiName": "cus-con"
                    }
                  ]
                }
                """.formatted(workspacePath);

        mockMvc.perform(post("/api/v1/scaffold/tool-group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Multi Tool Scaffolding Complete")));

        // Verify UseCase and UseCaseImpl are abbreviated to CusCon
        boolean hasAbbrevUseCase = Files.walk(root)
                .anyMatch(p -> p.toString().endsWith("CusConUseCase.java"));
        assertTrue(hasAbbrevUseCase, "CusConUseCase.java should exist");

        boolean hasAbbrevUseCaseImpl = Files.walk(root)
                .anyMatch(p -> p.toString().endsWith("CusConUseCaseImpl.java"));
        assertTrue(hasAbbrevUseCaseImpl, "CusConUseCaseImpl.java should exist");

        // Verify unwanted CusInquiryContractStatusConverter was NOT created
        boolean hasUnwantedConverter = Files.walk(root)
                .anyMatch(p -> p.toString().endsWith("CusInquiryContractStatusConverter.java"));
        assertFalse(hasUnwantedConverter, "CusInquiryContractStatusConverter.java should not be created");

        // Verify valid CusConConverter WAS created
        boolean hasValidConverter = Files.walk(root)
                .anyMatch(p -> p.toString().endsWith("CusConConverter.java"));
        assertTrue(hasValidConverter, "CusConConverter.java should exist");

        // Verify application_local.yml was generated with both http api-list and mci
        Path localYml = Files.walk(root)
                .filter(p -> p.toString().endsWith("application_local.yml"))
                .findFirst()
                .orElseThrow();
        String ymlContent = Files.readString(localYml, StandardCharsets.UTF_8);
        assertTrue(ymlContent.contains("api-list:"), ymlContent);
        assertTrue(ymlContent.contains("- name: cus-con"), ymlContent);
        assertTrue(ymlContent.contains("mci:"), ymlContent);
        assertTrue(ymlContent.contains("host: ${GLOW_COMMUNICATION_MCI_HOST:http://localhost}"), ymlContent);
    }
}
