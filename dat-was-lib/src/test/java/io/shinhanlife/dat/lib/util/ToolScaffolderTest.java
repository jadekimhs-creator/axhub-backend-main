package io.shinhanlife.dat.lib.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.shinhanlife.dat.lib.metadata.ToolDefinition;
import io.shinhanlife.dat.lib.metadata.ToolDefinitionValidator;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ToolScaffolderTest {

    @Test
    void exposesGroupedUseCaseScaffoldApi() {
        assertDoesNotThrow(() -> ToolScaffolder.class.getMethod(
                "scaffoldUseCase", String.class, String.class, String.class, String.class, List.class));
    }

    @Test
    void generatesOneUseCaseWithTwoMcpToolMethodsAndTypedClients() throws Exception {
        String moduleName = root.resolve("dat-was-customer").toString();
        ToolScaffolder.scaffoldUseCase("Customer", moduleName, "tester", "2026.08.12", List.of(
                new ToolScaffolder.ToolMethodDefinition(
                        "CustomerGuidance", "searchGuidance", "CTMNILO00007", "Customer guidance", "Search guidance", "cmm", "MCI",
                        false, "NILD", null,
                        List.of(new ToolScaffolder.FieldDefinition("customerId", "String", "Customer ID", List.of("C001"), "", true)),
                        List.of(new ToolScaffolder.FieldDefinition("guidanceStatus", "String", "Guidance status", List.of("OPEN"), "", false)), null),
                new ToolScaffolder.ToolMethodDefinition(
                        "CustomerContract", "searchContract", "CTMCNT00001", "Customer contract", "Search contract", "cmm", "MCI",
                        false, "CNTD", null,
                        List.of(new ToolScaffolder.FieldDefinition("customerId", "String", "Customer ID", List.of("C001"), "", true)),
                        List.of(new ToolScaffolder.FieldDefinition("contractStatus", "String", "Contract status", List.of("ACTIVE"), "", false)), null)));

        Path sourceRoot = root.resolve("dat-was-customer/src/main/java/io/shinhanlife/dat/mcc");
        String useCase = Files.readString(sourceRoot.resolve("biz/cmm/usecase/CustomerUseCase.java"));
        String implementation = Files.readString(sourceRoot.resolve("biz/cmm/usecase/impl/CustomerUseCaseImpl.java"));
        String guidanceClient = Files.readString(sourceRoot.resolve("infra/itrf/mci/nild/CustomerGuidanceClient.java"));

        assertTrue(useCase.contains("CustomerGuidanceResponse searchGuidance(CustomerGuidanceRequest req)"), useCase);
        assertTrue(useCase.contains("CustomerContractResponse searchContract(CustomerContractRequest req)"), useCase);
        assertTrue(implementation.contains("private final CustomerGuidanceClient customerGuidanceClient;"), implementation);
        assertTrue(implementation.contains("customerGuidanceClient.callCustomerGuidance(request)"), implementation);
        assertTrue(guidanceClient.contains("CustomerGuidance_O callCustomerGuidance(CustomerGuidance_I request)"), guidanceClient);
    }

    @Test
    void groupedHttpToolRegistersItsGlowApiCatalogEntry() throws Exception {
        String moduleName = root.resolve("dat-was-http").toString();

        ToolScaffolder.scaffoldUseCase("Employee", moduleName, "tester", "2026.08.13", List.of(
                new ToolScaffolder.ToolMethodDefinition(
                        "EmployeeSearch", "searchEmployee", null, "Employee search", "Search employee", "smp", "HTTP",
                        false, null, "employee-search",
                        List.of(new ToolScaffolder.FieldDefinition("employeeNo", "String", "Employee number", List.of("10001"), "", true)),
                        List.of(new ToolScaffolder.FieldDefinition("employeeName", "String", "Employee name", List.of("Hong"), "", false)), null)));

        Path glowConfig = root.resolve("dat-was-http/src/main/resources/glow/application-glow-local.yml");
        assertTrue(Files.exists(glowConfig));
        assertTrue(Files.readString(glowConfig).contains("- name: employee-search"));
    }

    @Test
    void generatesExceptionSafeMciUseCaseWithoutResponseJsonOrExamplesInDescriptions() throws Exception {
        String moduleName = root.resolve("dat-was-pro").toString();
        List<ToolScaffolder.FieldDefinition> inputFields = List.of(
                new ToolScaffolder.FieldDefinition("residentNumber", "String", "주민등록번호", List.of("900101-1234567"), "^\\d{6}-\\d{7}$", true));
        List<ToolScaffolder.FieldDefinition> outputFields = List.of(
                new ToolScaffolder.FieldDefinition("customerName", "String", "고객명", List.of("홍길동"), "", true));

        ToolScaffolder.scaffoldUseCase("IndividualCustomer", moduleName, "tester", "2026.09.03", List.of(
                new ToolScaffolder.ToolMethodDefinition(
                        "IndividualCustomerDetailInquiry", "individualCustomerDetailInquiry", "ONCSA1341",
                        "개인고객상세조회", "개인 고객 상세 정보를 조회합니다.", "pro", "MCI", false, "ONCSA1341", null,
                        inputFields, outputFields, null)));

        Path sourceRoot = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc");
        String implementation = Files.readString(sourceRoot.resolve("biz/pro/usecase/impl/IndividualCustomerUseCaseImpl.java"));
        String request = Files.readString(sourceRoot.resolve("biz/pro/dto/IndividualCustomerDetailInquiryRequest.java"));
        Path mockResponse = root.resolve("dat-was-pro/src/main/resources/mock-responses/pro_individual_customer_detail_inquiry.json");

        assertTrue(implementation.contains("try {"), implementation);
        assertTrue(implementation.contains("} catch (Exception e) {"), implementation);
        assertTrue(implementation.contains("errorResponse.setResultCode(\"ERROR\")"), implementation);
        assertTrue(request.contains("@Pattern(regexp = \"^\\\\d{6}-\\\\d{7}$\")"), request);
        assertTrue(request.contains("@Schema(description = \"주민등록번호 (형식: ^\\\\d{6}-\\\\d{7}$)\", example = \"900101-1234567\""), request);
        assertFalse(request.contains("(예시:"), request);
        assertFalse(Files.exists(mockResponse), mockResponse.toString());
    }

    @Test
    void generatesEnumAndListFieldsInDtoSchemaAndMockResponse() throws Exception {
        String moduleName = root.resolve("dat-was-claim").toString();
        List<ToolScaffolder.FieldDefinition> inputFields = List.of(
                new ToolScaffolder.FieldDefinition("claimStatus", "Enum", "Claim status", List.of("OPEN"), "", true,
                        List.of("OPEN", "CLOSED"), null, List.of()),
                new ToolScaffolder.FieldDefinition("customerIds", "List", "Customer IDs", List.of("C001"), "", false,
                        List.of(), "String", List.of()));
        List<ToolScaffolder.FieldDefinition> outputFields = List.of(
                new ToolScaffolder.FieldDefinition("guidanceItems", "List", "Guidance items", List.of(), "", false,
                        List.of(), "Object", List.of(new ToolScaffolder.FieldDefinition("status", "String", "Status", List.of("OPEN"), "", true))));

        ToolScaffolder.scaffold("claim search", "CLM0001", "Claim search", "cmm", "MCI", moduleName,
                "tester", "2026.08.12", false, "CLM1", null, null, inputFields, outputFields);

        Path dtoRoot = root.resolve("dat-was-claim/src/main/java/io/shinhanlife/dat/mcc/biz/cmm/dto");
        String request = Files.readString(dtoRoot.resolve("ClaimSearchRequest.java"));
        String response = Files.readString(dtoRoot.resolve("ClaimSearchResponse.java"));
        String definition = Files.readString(root.resolve("dat-was-claim/src/main/resources/tool-definitions/cmm/cmm_claim_search.yml"));
        String mock = Files.readString(root.resolve("dat-was-claim/src/main/resources/mock-responses/cmm_claim_search.json"));

        assertTrue(request.contains("private ClaimStatus claimStatus;"), request);
        assertTrue(request.contains("private List<String> customerIds;"), request);
        assertTrue(Files.exists(dtoRoot.resolve("ClaimStatus.java")));
        assertTrue(response.contains("private List<GuidanceItemsItem> guidanceItems;"), response);
        assertTrue(response.contains("public static class GuidanceItemsItem"), response);
        assertFalse(Files.exists(dtoRoot.resolve("ClaimSearchResponseGuidanceItemsItem.java")));
        assertTrue(definition.contains("enum: [OPEN, CLOSED]"), definition);
        assertTrue(definition.contains("type: array"), definition);
        assertTrue(mock.contains("\"guidanceItems\" : [{"), mock);
    }

    @Test
    void generatesEveryToolSourceAsUtf8WithoutBrokenKoreanOrBom() throws Exception {
        String moduleName = root.resolve("dat-was-korean").toString();

        ToolScaffolder.scaffold("analysis data query", "CLYMCI00001", "분석 데이터 조회",
                "분석 데이터를 조회하기 위한 도구로 다양한 분석 결과를 제공합니다.",
                "cmm", "MCI", moduleName, "테스터", "2026.08.12",
                false, null, null, null,
                List.of(new ToolScaffolder.FieldDefinition("query", "String", "조회 조건", List.of("계약 분석"), "", true)),
                List.of(new ToolScaffolder.FieldDefinition("analysisResult", "String", "분석 결과", List.of("정상"), "", false)));

        Path sourceRoot = root.resolve("dat-was-korean/src/main/java/io/shinhanlife/dat/mcc");
        Path useCase = sourceRoot.resolve("biz/cmm/usecase/AnalysisDataQueryUseCase.java");
        Path implementation = sourceRoot.resolve("biz/cmm/usecase/impl/AnalysisDataQueryUseCaseImpl.java");

        String useCaseSource = Files.readString(useCase, StandardCharsets.UTF_8);
        String implementationSource = Files.readString(implementation, StandardCharsets.UTF_8);
        assertTrue(useCaseSource.contains("title = \"분석 데이터 조회\""), useCaseSource);
        assertTrue(useCaseSource.contains("description = \"분석 데이터를 조회하기 위한 도구로 다양한 분석 결과를 제공합니다.\""), useCaseSource);
        assertTrue(useCaseSource.contains("AX HUB 시스템 처리 클래스"), useCaseSource);
        assertTrue(useCaseSource.contains("개정이력"), useCaseSource);
        assertTrue(useCaseSource.contains("최초생성"), useCaseSource);
        assertTrue(implementationSource.contains("요청 수신"), implementationSource);
        assertTrue(implementationSource.contains("MapStruct를 이용한 자동 매핑"), implementationSource);
        assertTrue(implementationSource.contains("연동 중 오류 발생"), implementationSource);

        try (var files = Files.walk(root.resolve("dat-was-korean"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                byte[] bytes = Files.readAllBytes(file);
                assertFalse(bytes.length >= 3
                                && (bytes[0] & 0xff) == 0xef
                                && (bytes[1] & 0xff) == 0xbb
                                && (bytes[2] & 0xff) == 0xbf,
                        "UTF-8 BOM must not be generated: " + file);
                String content = Files.readString(file, StandardCharsets.UTF_8);
                assertFalse(content.contains("�"), "Invalid replacement character: " + file);
            }
        }
    }

    @Test
    void generatesV17ToolDefinitionTogetherWithToolSources() throws Exception {
        String moduleName = root.resolve("dat-was-v17-definition").toString();

        ToolScaffolder.scaffold("employee search", "HR_EMPLOYEE_SEARCH", "직원 조회",
                "사번으로 재직 중인 직원을 조회한다.", "smp", "HTTP", moduleName,
                "tester", "2026.08.12", false, null, null, null,
                List.of(new ToolScaffolder.FieldDefinition("employeeNo", "String", "조회할 사번", List.of("09860000"), "", true)),
                List.of(), "employee");

        Path definition = root.resolve("dat-was-v17-definition/src/main/resources/tool-definitions/smp/smp_employee_search.yml");
        String yaml = Files.readString(definition);

        assertTrue(yaml.contains("name: smp_employee_search"), yaml);
        assertTrue(yaml.contains("when_to_use:"), yaml);
        assertTrue(yaml.contains("example_queries:"), yaml);
        assertTrue(yaml.contains("additionalProperties: false"), yaml);
        assertTrue(yaml.contains("owner_org: \"MCP_TOOL\""), yaml);
        ToolDefinition parsed = new ObjectMapper(new YAMLFactory()).readValue(yaml, ToolDefinition.class);
        ToolDefinitionValidator.validate(parsed, definition.toString());
    }

    @Test
    void appliesV17MetadataEnteredByScaffoldUser() throws Exception {
        String moduleName = root.resolve("dat-was-v17-options").toString();
        ToolScaffolder.ToolDefinitionOptions options = new ToolScaffolder.ToolDefinitionOptions(
                "사번으로 직원을 조회한다.",
                "직원 정보 조회 요청에 사용한다.",
                "사번이 없으면 사용하지 않는다.",
                "최대 1건만 반환한다.",
                "직원 기본 정보 조회",
                List.of("사번 10001을 조회해줘", "직원 10001 소속을 알려줘", "10001 직원을 찾아줘"),
                List.of("employee", "search"), "HR_TEAM");

        ToolScaffolder.scaffold("employee search", "HR_EMPLOYEE_SEARCH", "직원 조회", "직원을 조회한다.",
                "smp", "HTTP", moduleName, "tester", "2026.08.12", false, null, null, null,
                List.of(new ToolScaffolder.FieldDefinition("employeeNo", "String", "조회할 사번", List.of("10001"), "", true)),
                List.of(), "employee", options);

        Path definition = root.resolve("dat-was-v17-options/src/main/resources/tool-definitions/smp/smp_employee_search.yml");
        ToolDefinition parsed = new ObjectMapper(new YAMLFactory()).readValue(Files.readString(definition), ToolDefinition.class);
        assertEquals("HR_TEAM", parsed.ownerOrg());
        assertEquals("10001 직원을 찾아줘", parsed.exampleQueries().get(2));
        assertEquals(2, parsed.tags().size());
        ToolDefinitionValidator.validate(parsed, definition.toString());
    }

    @TempDir
    Path root;

    @Test
    void generatesSpringAiToolAndResponseDto() throws Exception {
        String moduleName = "build/scaffold-manifest-test";

        ToolScaffolder.scaffold("claim search", "CLM0001", "청구 조회", "cmm", "HTTP", moduleName,
                "tester", "2026.08.04", true, null);

        Path root = Path.of(moduleName, "src/main/java/io/shinhanlife/dat/mcc/biz/cmm");
        String useCase = Files.readString(root.resolve("usecase/ClaimSearchUseCase.java"));
        String response = Files.readString(root.resolve("dto/ClaimSearchResponse.java"));

        assertTrue(useCase.contains("name = \"cmm_claim_search\""));
        assertTrue(useCase.contains("@GrowToolHint(register = true, categoryKey = \"cmm\", mappingId = \"CLM0001\")"));
        assertTrue(response.contains("private String resultCode;"));
        assertTrue(response.contains("private String resultMessage;"));
    }
    @Test
    void usesWasModuleNameAsToolPodPrefix() throws Exception {
        String moduleName = "build/dat-was-sal";

        ToolScaffolder.scaffold("notification send", "SMS0001", "SMS 발송", "cmm", "HTTP", moduleName,
                "tester", "2026.08.05", true, null);

        Path useCasePath = Path.of(moduleName,
                "src/main/java/io/shinhanlife/dat/mcc/biz/cmm/usecase/NotificationSendUseCase.java");
        String useCase = Files.readString(useCasePath);

        assertTrue(useCase.contains("name = \"cmm_notification_send\""));
    }

    @Test
    void createsSchemaResourcesInToolSchemaCategoryDirectory() throws Exception {
        String moduleName = root.resolve("dat-was-sample").toString();

        ToolScaffolder.scaffold("claim search", "CLM0001", "청구 조회", "cmm", "HTTP", moduleName,
                "tester", "2026.08.09", true, null,
                "classpath:tool-schemas/cmm/claim-search-resource-input-schema.json",
                "classpath:tool-schemas/cmm/claim-search-resource-output-schema.json");

        Path schemas = root.resolve("dat-was-sample/src/main/resources/tool-schemas/cmm");
        assertTrue(Files.exists(schemas.resolve("claim-search-resource-input-schema.json")));
        assertTrue(Files.exists(schemas.resolve("claim-search-resource-output-schema.json")));        String useCase = Files.readString(root.resolve("dat-was-sample/src/main/java/io/shinhanlife/dat/mcc/biz/cmm/usecase/ClaimSearchUseCase.java"));
        assertTrue(useCase.contains("@GrowToolHint(register = true, categoryKey = \"cmm\", mappingId = \"CLM0001\","));
        assertTrue(useCase.contains("inputSchemaResource  = \"classpath:tool-schemas/cmm/claim-search-resource-input-schema.json\""));
        assertTrue(useCase.contains("outputSchemaResource = \"classpath:tool-schemas/cmm/claim-search-resource-output-schema.json\""));
    }

    @Test
    void generatesRequestDtoFromDeclaredFields() throws Exception {
        String moduleName = root.resolve("dat-was-pay").toString();

        ToolScaffolder.scaffold("search hr", "SHEARCH_01", "HR 조회", "pay", "MCI", moduleName,
                "tester", "2026.08.09", true, "DFAG");

        Path requestPath = root.resolve("dat-was-pay/src/main/java/io/shinhanlife/dat/mcc/biz/pay/dto/SearchHrRequest.java");
        String request = Files.readString(requestPath);

        assertTrue(request.contains("import io.swagger.v3.oas.annotations.media.Schema;"));
        assertTrue(request.contains("@Schema(description = \"Search query\", example = \"example\")"));
        assertTrue(request.contains("private String query;"));
        assertFalse(request.contains("McpToolParam"));

        Path implementationPath = root.resolve("dat-was-pay/src/main/java/io/shinhanlife/dat/mcc/biz/pay/usecase/impl/SearchHrUseCaseImpl.java");
        String implementation = Files.readString(implementationPath);
        assertTrue(implementation.contains("public SearchHrResponse execute(SearchHrRequest req)"));
        assertTrue(implementation.contains("response.setResultCode(\"SUCCESS\")"));
    }

    @Test
    void generatesMciToolFromDeclaredInputAndOutputFields() throws Exception {
        String moduleName = root.resolve("dat-was-pay").toString();
        List<ToolScaffolder.FieldDefinition> inputFields = List.of(
                new ToolScaffolder.FieldDefinition("employeeId", "String", "Employee identifier", List.of("EMP10001"), "", true),
                new ToolScaffolder.FieldDefinition("page", "Integer", "Page number", List.of("1"), "", false));
        List<ToolScaffolder.FieldDefinition> outputFields = List.of(
                new ToolScaffolder.FieldDefinition("employeeName", "String", "Employee name", List.of("Hong Gildong"), "", true));

        ToolScaffolder.scaffold("search hr", "SHEARCH_01", "HR search", "pay", "MCI", moduleName,
                "tester", "2026.08.09", true, "DFAG", null, null, inputFields, outputFields);

        Path sourceRoot = root.resolve("dat-was-pay/src/main/java/io/shinhanlife/dat/mcc");
        String request = Files.readString(sourceRoot.resolve("biz/pay/dto/SearchHrRequest.java"));
        String response = Files.readString(sourceRoot.resolve("biz/pay/dto/SearchHrResponse.java"));
        String mciRequest = Files.readString(sourceRoot.resolve("infra/itrf/mci/dfag/io/SHEARCH_01_I.java"));
        String mciResponse = Files.readString(sourceRoot.resolve("infra/itrf/mci/dfag/io/SHEARCH_01_O.java"));
        String converter = Files.readString(sourceRoot.resolve("biz/pay/converter/SearchHrConverter.java"));        String useCase = Files.readString(sourceRoot.resolve("biz/pay/usecase/SearchHrUseCase.java"));
        String implementation = Files.readString(sourceRoot.resolve("biz/pay/usecase/impl/SearchHrUseCaseImpl.java"));
        assertTrue(mciRequest.contains("package io.shinhanlife.dat.mcc.infra.itrf.mci.dfag.io;"), mciRequest);        assertTrue(useCase.contains("@GrowToolHint(register = true, categoryKey = \"pay\", mappingId = \"SHEARCH_01\")"));
        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.mci.dfag.io.SHEARCH_01_O;"));

        assertTrue(request.contains("private String employeeId;"));
        assertTrue(request.contains("private Integer page;"));
        assertFalse(request.contains("phoneNumber"));
        assertTrue(response.contains("private String employeeName;"));
        assertTrue(mciRequest.contains("private String employeeId;"));
        assertTrue(mciResponse.contains("private String employeeName;"));
        assertTrue(converter.contains("SearchHrResponse toResponse(SHEARCH_01_O mciRes);"));
    }
    @Test
    void generatesMockResponseAndUnitTestSkeletonFromOutputFields() throws Exception {
        String moduleName = root.resolve("dat-was-cus").toString();
        List<ToolScaffolder.FieldDefinition> outputFields = List.of(
                new ToolScaffolder.FieldDefinition("status", "String", "Claim status", List.of("RECEIVED"), "", true));

        ToolScaffolder.scaffold("claim search", "CLM0001", "Claim search", "cmm", "MCI", moduleName,
                "tester", "2026.08.10", true, null, null, null, List.of(), outputFields);

        Path mockResponse = root.resolve("dat-was-cus/src/main/resources/mock-responses/cmm_claim_search.json");
        Path useCaseTest = root.resolve("dat-was-cus/src/test/java/io/shinhanlife/dat/mcc/biz/cmm/usecase/ClaimSearchUseCaseTest.java");

        assertTrue(Files.exists(mockResponse));
        assertTrue(Files.readString(mockResponse).contains("\"status\" : \"RECEIVED\""));
        assertTrue(Files.exists(useCaseTest));
        assertTrue(Files.readString(useCaseTest).contains("class ClaimSearchUseCaseTest"));
    }
    @Test
    void createsWireMockMappingForHttpTool() {
        String mapping = ToolScaffolder.wireMockMappingContent("HR_EMPLOYEE_SEARCH", "smp_employee_search.json");

        assertTrue(mapping.contains("\"method\" : \"POST\""), mapping);
        assertTrue(mapping.contains("\"urlPath\" : \"/HR_EMPLOYEE_SEARCH\""), mapping);
        assertTrue(mapping.contains("\"bodyFileName\" : \"smp_employee_search.json\""), mapping);
    }


    @Test
    void generatesDtoPackageAndRemovesDuplicateResponseFields() throws Exception {
        String moduleName = root.resolve("dat-was-http").toString();
        List<ToolScaffolder.FieldDefinition> outputFields = List.of(
                new ToolScaffolder.FieldDefinition("resultCode", "String", "API result", List.of("SUCCESS"), "", true),
                new ToolScaffolder.FieldDefinition("employeeName", "String", "Employee name", List.of("Hong Gildong"), "", false),
                new ToolScaffolder.FieldDefinition("employeeName", "String", "Duplicate name", List.of("Duplicate"), "", false));

        List<ToolScaffolder.FieldDefinition> inputFields = List.of(
                new ToolScaffolder.FieldDefinition("employeeId", "String", "Employee identifier", List.of("EMP10001"), "", true));
        String resultLog = ToolScaffolder.scaffold("employee search", "HR_EMPLOYEE_SEARCH", "Employee search", "smp", "HTTP", moduleName,
                "tester", "2026.08.11", false, null, null, null, inputFields, outputFields);

        Path dtoRoot = root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/biz/smp/dto");
        String request = Files.readString(dtoRoot.resolve("EmployeeSearchRequest.java"));
        String response = Files.readString(dtoRoot.resolve("EmployeeSearchResponse.java"));

        assertTrue(request.contains("package io.shinhanlife.dat.mcc.biz.smp.dto;"), request);
        assertTrue(response.contains("package io.shinhanlife.dat.mcc.biz.smp.dto;"), response);
        assertTrue(response.indexOf("private String resultCode;") == response.lastIndexOf("private String resultCode;"), response);
        assertTrue(response.indexOf("private String employeeName;") == response.lastIndexOf("private String employeeName;"), response);
        String converter = Files.readString(root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/biz/smp/converter/EmployeeSearchConverter.java"));
        String httpRequest = Files.readString(root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/http/employee_search/io/EmployeeSearchHttpRequest.java"));
        String httpResponse = Files.readString(root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/http/employee_search/io/EmployeeSearchHttpResponse.java"));
        String httpClient = Files.readString(root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/infra/itrf/http/employee_search/EmployeeSearchClient.java"));
        assertFalse(converter.contains("phoneNumber"), converter);
        assertTrue(converter.contains("infra.itrf.http.employee_search.io.EmployeeSearchHttpRequest"), converter);
        assertFalse(converter.contains("io.shinhanlife.dat.mcc.io.shinhanlife.dat.mcc"), converter);
        assertTrue(converter.contains("// @Mapping(source = \"sourceField\", target = \"targetField\")"), converter);
        assertTrue(httpRequest.contains("private String employeeId;"), httpRequest);
        assertTrue(httpResponse.contains("private String employeeName;"), httpResponse);
        assertTrue(httpClient.contains("http.call(API_NAME, request, responseType)"), httpClient);
        assertFalse(Files.exists(root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/biz/smp/legacy")));
        String implementation = Files.readString(root.resolve("dat-was-http/src/main/java/io/shinhanlife/dat/mcc/biz/smp/usecase/impl/EmployeeSearchUseCaseImpl.java"));
        assertTrue(implementation.contains("public EmployeeSearchResponse execute(EmployeeSearchRequest req)"), implementation);
        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.http.employee_search.EmployeeSearchClient;"), implementation);
        assertTrue(implementation.contains("private final EmployeeSearchClient employeeSearchClient;"), implementation);
        assertTrue(implementation.contains("employeeSearchClient.call(httpRequest, EmployeeSearchHttpResponse.class)"), implementation);
        assertFalse(implementation.contains("AxhubHttpComponent"), implementation);
        assertFalse(implementation.contains("executeLegacy(\"HTTP\""), implementation);
        Path wireMockResponse = root.resolve("mci-mock/__files/smp_employee_search.json");
        Path wireMockMapping = root.resolve("mci-mock/mappings/smp_employee_search.json");
        assertTrue(Files.exists(wireMockResponse), wireMockResponse.toString());
        assertTrue(Files.exists(wireMockMapping), wireMockMapping.toString());
        assertTrue(Files.readString(wireMockMapping).contains("\"urlPath\" : \"/HR_EMPLOYEE_SEARCH\""));
        Path localConfig = root.resolve("src/main/resources/glow/application-glow-local.yml");
        assertTrue(Files.exists(localConfig), localConfig.toString());
        assertTrue(Files.readString(localConfig).contains("name: employee-search"));
        assertTrue(Files.readString(localConfig).contains("url: ${AXHUB_EMPLOYEE_SEARCH_HTTP_URL:/api/mock/http/smp_employee_search}"));
        Path podMockResponse = root.resolve("dat-was-http/src/main/resources/mock-responses/smp_employee_search.json");
        assertTrue(Files.exists(podMockResponse), podMockResponse.toString());
        assertTrue(resultLog.contains("Tip: HTTP Tool은 WireMock 실행 후 생성된 mapping URL로 호출을 확인하세요."), resultLog);

        ToolScaffolder.scaffold("employee search", "HR_EMPLOYEE_SEARCH", "Employee search", "smp", "HTTP", moduleName,
                "tester", "2026.08.11", false, null, null, null, inputFields, outputFields);
        long apiNameCount = Files.readAllLines(localConfig).stream()
                .filter(line -> line.trim().equals("- name: employee-search"))
                .count();
        assertEquals(1, apiNameCount);
    }
    @Test
    void generatesSeparateToolTitleAndDescription() throws Exception {
        String moduleName = root.resolve("dat-was-title").toString();

        ToolScaffolder.scaffold("employee search", "HR_EMPLOYEE_SEARCH", "직원 정보 조회",
                "사번을 입력받아 재직 중인 직원의 기본 정보를 조회한다.", "smp", "HTTP", moduleName,
                "tester", "2026.08.11", false, null, null, null, List.of(), List.of());

        Path useCasePath = root.resolve("dat-was-title/src/main/java/io/shinhanlife/dat/mcc/biz/smp/usecase/EmployeeSearchUseCase.java");
        String useCase = Files.readString(useCasePath);

        assertTrue(useCase.contains("title = \"직원 정보 조회\""), useCase);
        assertTrue(useCase.contains("description = \"사번을 입력받아 재직 중인 직원의 기본 정보를 조회한다.\""), useCase);
    }
    @Test
    void generatesHttpToolUsingConfiguredApiNameAndConfiguredUrlOnly() throws Exception {
        String moduleName = root.resolve("dat-was-http-api-name").toString();

        ToolScaffolder.scaffold("employee search", "HR_EMPLOYEE_SEARCH", "직원 정보 조회",
                "사번으로 직원을 조회한다.", "smp", "HTTP", moduleName, "tester", "2026.08.11",
                false, null, null, null, List.of(), List.of(), "employee");

        Path implementationPath = root.resolve("dat-was-http-api-name/src/main/java/io/shinhanlife/dat/mcc/biz/smp/usecase/impl/EmployeeSearchUseCaseImpl.java");
        String implementation = Files.readString(implementationPath);

        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.http.employee.EmployeeClient;"), implementation);
        assertTrue(implementation.contains("employeeClient.call(httpRequest, EmployeeSearchHttpResponse.class)"), implementation);
        assertFalse(implementation.contains("AxhubHttpDomain"), implementation);
        assertFalse(implementation.contains("\"/HR_EMPLOYEE_SEARCH\""), implementation);
    }

    @Test
    void addsHttpApiEntryOnItsOwnYamlLineBeforeMciConfiguration() throws Exception {
        String moduleName = root.resolve("dat-was-http-yaml").toString();
        Path localConfig = root.resolve("src/main/resources/glow/application-glow-local.yml");
        Files.createDirectories(localConfig.getParent());
        Files.writeString(localConfig, """
                glow:
                  communication:
                    http:
                      api-list:
                        - name: memo
                          biz-pod: false
                    mci:
                      host: localhost
                """);

        ToolScaffolder.scaffold("insurance claim processor", "CLAIM0000001", "Insurance claim", "Insurance claim", "ins", "HTTP", moduleName,
                "tester", "2026.08.11", false, null, null, null, List.of(), List.of(), "insurance");

        String yaml = Files.readString(localConfig);
        assertFalse(yaml.contains("biz-pod: false        - name"), yaml);
        assertTrue(yaml.contains("      - name: insurance"), yaml);
        assertTrue(yaml.indexOf("      - name: insurance") < yaml.indexOf("    mci:"), yaml);
    }

    @Test
    void generatesObjectListAsNestedInnerClassWithoutSeparateItemSource() throws Exception {
        String moduleName = root.resolve("dat-was-inner-list").toString();
        List<ToolScaffolder.FieldDefinition> fields = List.of(
                new ToolScaffolder.FieldDefinition("data", "List", "activity data", List.of(), "", false,
                        List.of(), "Object", List.of(
                        new ToolScaffolder.FieldDefinition("date", "String", "date", List.of("2026-08-13"), "", true),
                        new ToolScaffolder.FieldDefinition("users", "Integer", "users", List.of("10"), "", false))));

        ToolScaffolder.scaffold("ga activity status", "GA001", "GA status", "GA status", "ana", "HTTP",
                moduleName, "tester", "2026.08.13", false, null, null, null, List.of(), fields);

        Path dtoDir = root.resolve("dat-was-inner-list/src/main/java/io/shinhanlife/dat/mcc/biz/ana/dto");
        String response = Files.readString(dtoDir.resolve("GaActivityStatusResponse.java"));
        assertTrue(response.contains("private List<DataItem> data;"), response);
        assertTrue(response.contains("public static class DataItem"), response);
        assertTrue(response.contains("private String date;"), response);
        assertFalse(Files.exists(dtoDir.resolve("GaActivityStatusResponseDataItem.java")));
    }

    @Test
    void groupedUseCaseSupportsHttpAndMciTools() throws Exception {
        String moduleName = root.resolve("dat-was-mixed").toString();
        ToolScaffolder.scaffoldUseCase("Customer", moduleName, "tester", "2026.08.13", List.of(
                new ToolScaffolder.ToolMethodDefinition("CustomerProfile", "getProfile", "CUST001", "Profile", "profile", "cmm", "MCI",
                        false, "CSTM", null, List.of(), List.of(), null),
                new ToolScaffolder.ToolMethodDefinition("CustomerNotice", "getNotice", "NOTICE001", "Notice", "notice", "cmm", "HTTP",
                        false, null, "customer-notice", List.of(), List.of(), null)));

        Path sourceRoot = root.resolve("dat-was-mixed/src/main/java/io/shinhanlife/dat/mcc");
        String impl = Files.readString(sourceRoot.resolve("biz/cmm/usecase/impl/CustomerUseCaseImpl.java"));
        assertTrue(impl.contains("getProfile(CustomerProfileRequest req)"), impl);
        assertTrue(impl.contains("getNotice(CustomerNoticeRequest req)"), impl);
        assertTrue(Files.exists(sourceRoot.resolve("infra/itrf/http/customer_notice/CustomerNoticeClient.java")));
    }

    @Test
    void addsToolMethodToExistingUseCaseInsteadOfOverwritingIt() throws Exception {
        String moduleName = root.resolve("dat-was-existing").toString();
        ToolScaffolder.scaffoldUseCase("Customer", moduleName, "tester", "2026.08.13", List.of(
                new ToolScaffolder.ToolMethodDefinition("CustomerProfile", "getProfile", "CUST001", "Profile", "profile", "cmm", "MCI",
                        false, "CSTM", null, List.of(), List.of(), new ToolScaffolder.ToolDefinitionOptions(
                                null, null, null, null, null, List.of(), List.of(), null, 12000L, 5))));
        ToolScaffolder.scaffoldUseCase("Customer", moduleName, "tester", "2026.08.13", List.of(
                new ToolScaffolder.ToolMethodDefinition("CustomerNotice", "getNotice", "NOTICE001", "Notice", "notice", "cmm", "HTTP",
                        false, null, "customer-notice", List.of(), List.of(), null)));

        Path sourceRoot = root.resolve("dat-was-existing/src/main/java/io/shinhanlife/dat/mcc");
        String useCase = Files.readString(sourceRoot.resolve("biz/cmm/usecase/CustomerUseCase.java"));
        String impl = Files.readString(sourceRoot.resolve("biz/cmm/usecase/impl/CustomerUseCaseImpl.java"));
        assertTrue(useCase.contains("getProfile(CustomerProfileRequest req)"), useCase);
        assertTrue(useCase.contains("timeoutMillis = 12000L"), useCase);
        assertTrue(useCase.contains("retryMaxAttempts = 5"), useCase);
        assertTrue(useCase.contains("getNotice(CustomerNoticeRequest req)"), useCase);
        assertTrue(impl.contains("private final MciCstmClient mciCstmClient;"), impl);
        assertTrue(impl.contains("private final CustomerNoticeClient customerNoticeClient;"), impl);
    }

    @Test
    void appendsMciToolWithSystemPrefixClientAndNullSafeResponseHandling() throws Exception {
        String moduleName = root.resolve("dat-was-pro").toString();
        ToolScaffolder.scaffoldUseCase("IndividualCustomer", moduleName, "tester", "2026.09.03", List.of(
                new ToolScaffolder.ToolMethodDefinition("Initial", "initial", null, "초기 도구", "초기 도구", "pro", "HTTP",
                        false, null, "initial", List.of(), List.of(), null)));

        ToolScaffolder.scaffoldUseCase("IndividualCustomer", moduleName, "tester", "2026.09.03", List.of(
                new ToolScaffolder.ToolMethodDefinition("IndividualCustomerDetailInquiry", "inquiry", "LCHITP00001",
                        "개인 고객 상세 조회", "개인 고객 상세 정보를 조회합니다.", "pro", "MCI", false,
                        "ONCSG1341", null, List.of(), List.of(), null)));

        Path implementationPath = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/biz/pro/usecase/impl/IndividualCustomerUseCaseImpl.java");
        String implementation = Files.readString(implementationPath);

        assertTrue(implementation.contains("import io.shinhanlife.dat.mcc.infra.itrf.mci.ncs.g.MciNcsgClient;"), implementation);
        assertTrue(implementation.contains("private final MciNcsgClient mciNcsgClient;"), implementation);
        assertFalse(implementation.contains("MciOncsg1341Client"), implementation);
        assertTrue(implementation.contains("Transfer<ONCSG1341_O> transfer"), implementation);
        assertTrue(implementation.contains("transfer == null || transfer.getBody() == null"), implementation);
        assertTrue(implementation.contains("setResultCode(\"ERROR\")"), implementation);
        assertFalse(implementation.contains("LCHITP00001\", null, request, ONCSG1341_O.class).getBody()"), implementation);
    }

    @Test
    void generatesAbbreviatedMciSourcesAndTargetSystemConverterPackage() throws Exception {
        String moduleName = root.resolve("dat-was-pro").toString();

        ToolScaffolder.scaffold("individual customer detail inquiry", "LCHITP00001",
                "개인 고객 상세 조회", "개인 고객 상세 정보를 조회합니다.",
                "pro", "MCI", moduleName, "tester", "2026.09.10", false,
                "ONBTA2380", null, null, List.of(), List.of(), null, null);

        Path sourceRoot = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/biz/pro");
        Path converter = sourceRoot.resolve("converter/nbt/a/ONBTA2380Converter.java");
        assertTrue(Files.exists(converter));
        assertFalse(Files.exists(sourceRoot.resolve("converter/IndiCustomConverter.java")));
        assertFalse(Files.exists(sourceRoot.resolve("converter/nbt/a/IndiCustomConverter.java")));
        assertTrue(Files.exists(sourceRoot.resolve("dto/IndiCustomRequest.java")));
        assertTrue(Files.exists(sourceRoot.resolve("dto/IndiCustomResponse.java")));
        assertTrue(Files.exists(sourceRoot.resolve("usecase/IndiCustomUseCase.java")));
        assertTrue(Files.exists(sourceRoot.resolve("usecase/impl/IndiCustomUseCaseImpl.java")));

        String converterSource = Files.readString(converter);
        String useCaseSource = Files.readString(sourceRoot.resolve("usecase/IndiCustomUseCase.java"));
        String implementationSource = Files.readString(sourceRoot.resolve("usecase/impl/IndiCustomUseCaseImpl.java"));
        assertTrue(converterSource.contains("package io.shinhanlife.dat.mcc.biz.pro.converter.nbt.a;"), converterSource);
        assertTrue(converterSource.contains("public interface ONBTA2380Converter {"), converterSource);
        assertTrue(implementationSource.contains("import io.shinhanlife.dat.mcc.biz.pro.converter.nbt.a.ONBTA2380Converter;"), implementationSource);
        assertTrue(implementationSource.contains("private final ONBTA2380Converter converter;"), implementationSource);
        assertTrue(useCaseSource.contains("name = \"pro_individual_inquiry\""), useCaseSource);
    }

    @Test
    void generatesAbbreviatedMciSourcesAndTargetSystemConverterInGroupMode() throws Exception {
        String moduleName = root.resolve("dat-was-pro").toString();

        ToolScaffolder.scaffoldUseCase("IndividualCustomerDetailInquiry", moduleName, "tester", "2026.09.10", List.of(
                new ToolScaffolder.ToolMethodDefinition("IndividualCustomerDetailInquiry", "inquiry", "LCHITP00001",
                        "개인 고객 상세 조회", "개인 고객 상세 정보를 조회합니다.", "pro", "MCI", false,
                        "ONBTA2380", null, List.of(), List.of(), null)));

        Path sourceRoot = root.resolve("dat-was-pro/src/main/java/io/shinhanlife/dat/mcc/biz/pro");
        Path converter = sourceRoot.resolve("converter/nbt/a/ONBTA2380Converter.java");
        assertTrue(Files.exists(converter));
        assertFalse(Files.exists(sourceRoot.resolve("converter/IndiCustomConverter.java")));
        assertTrue(Files.exists(sourceRoot.resolve("dto/IndiCustomRequest.java")));
        assertTrue(Files.exists(sourceRoot.resolve("dto/IndiCustomResponse.java")));
        assertTrue(Files.exists(sourceRoot.resolve("usecase/IndiCustomUseCase.java")));
        assertTrue(Files.exists(sourceRoot.resolve("usecase/impl/IndiCustomUseCaseImpl.java")));

        String converterSource = Files.readString(converter);
        String useCaseSource = Files.readString(sourceRoot.resolve("usecase/IndiCustomUseCase.java"));
        String implementationSource = Files.readString(sourceRoot.resolve("usecase/impl/IndiCustomUseCaseImpl.java"));
        assertTrue(converterSource.contains("package io.shinhanlife.dat.mcc.biz.pro.converter.nbt.a;"), converterSource);
        assertTrue(converterSource.contains("public interface ONBTA2380Converter {"), converterSource);
        assertTrue(implementationSource.contains("import io.shinhanlife.dat.mcc.biz.pro.converter.nbt.a.ONBTA2380Converter;"), implementationSource);
        assertTrue(implementationSource.contains("private final ONBTA2380Converter converter;"), implementationSource);
        assertTrue(useCaseSource.contains("name = \"pro_individual_inquiry\""), useCaseSource);
    }
}
