package io.shinhanlife.dat.mcg.presentation;


/**
 * @package io.shinhanlife.dat.mcg.presentation
 * @className ScaffoldingController
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
import io.shinhanlife.dat.lib.util.NewPodProjectScaffolder;
import io.shinhanlife.dat.lib.util.PodScaffolder;
import io.shinhanlife.dat.lib.util.MciResponseScaffolder;
import io.shinhanlife.dat.lib.util.ToolScaffolder;
import io.shinhanlife.dat.lib.util.ToolSourceUpdater;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scaffold")
public class ScaffoldingController {

    private static final Set<String> SUPPORTED_FIELD_TYPES = Set.of(
            "String", "Integer", "Long", "Double", "Boolean", "BigDecimal", "List");
    private static final Set<String> SUPPORTED_AI_MODELS = Set.of(
            "GLM-5.3-Flash",
            "Gemma-4-31B",
            "Qwen3-Coder",
            "inclusionai/ling-3.0-flash:free",
            "openai/gpt-oss-20b:free",
            "google/gemma-4-31b-it:free",
            "nvidia/nemotron-3-nano-30b-a3b:free",
            "cohere/north-mini-code:free");
    private static final Set<String> OPENROUTER_AI_MODELS = Set.of(
            "inclusionai/ling-3.0-flash:free",
            "openai/gpt-oss-20b:free",
            "google/gemma-4-31b-it:free",
            "nvidia/nemotron-3-nano-30b-a3b:free",
            "cohere/north-mini-code:free");

    static final String DEFAULT_WORKSPACE = "C:\\eGovFrameDev-4.3.1-64bit\\workspace-egov\\dat-was-dasmt";
    static final String DEFAULT_NEW_POD_WORKSPACE = "C:\\Users\\09863406\\IdeaProjects";

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.base-url}")
    private String liteLlmBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.gemma-key}")
    private String gemmaKey;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.qwen-key}")
    private String qwenKey;

    @org.springframework.beans.factory.annotation.Value("${shinhan.ai.glm-key:sk-TqLYA8ZcjGNsmJP0VWr8CA}")
    private String glmKey;

    @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.api-key:}")
    private String openRouterApiKey;

    public ScaffoldingController(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/pod")
    public String scaffoldPod(@RequestBody Map<String, String> req) {
        try {
            String moduleName = req.getOrDefault("moduleName", "dat-was-cus");
            if (!moduleName.startsWith("dat-was-")) moduleName = "dat-was-" + moduleName;
            String port = req.getOrDefault("port", "8085");
            String shortName = moduleName.replace("dat-was-", "").replace("-", "");
            String author = req.get("author");
            if (author == null || author.trim().isEmpty()) author = System.getProperty("user.name");
            String date = req.get("date");
            if (date == null || date.trim().isEmpty()) date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            
            String defaultWorkspace = DEFAULT_WORKSPACE;
              String workspacePath = req.getOrDefault("workspacePath", defaultWorkspace);
              if (workspacePath.trim().isEmpty()) {
                  workspacePath = defaultWorkspace;
              }
              System.setProperty("AXHUB_SOURCE_DIR", workspacePath);

              return PodScaffolder.scaffoldPod(moduleName, port, shortName, author, date,
                      req.get("toolServiceManifest"), targetModules(req.get("targetModules")));
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    /**
     * Creates an independent Pod project below the shared workspace.
     * This deliberately does not use PodScaffolder, which changes the legacy AX Hub multi-module project.
     */
    @PostMapping("/pod-new")
    public ResponseEntity<?> scaffoldNewPod(@RequestBody NewPodProjectRequest request) {
        try {
            if (request == null || request.moduleName() == null || request.moduleName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Pod Module New 이름을 입력해주세요."));
            }
            if (request.port() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "서비스 포트를 입력해주세요."));
            }
            String workspacePath = request.workspacePath() == null || request.workspacePath().isBlank()
                    ? DEFAULT_NEW_POD_WORKSPACE : request.workspacePath().trim();
            Path targetDir = Path.of(workspacePath).resolve(request.moduleName().trim());
            if (Files.exists(targetDir)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Target module already exists: " + request.moduleName().trim()));
            }
            String author = request.author() == null || request.author().isBlank()
                    ? System.getProperty("user.name") : request.author().trim();
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            String result = NewPodProjectScaffolder.scaffold(
                    Path.of(workspacePath), request.moduleName().trim(), request.port(), author, date,
                    request.toolServiceManifest(), targetModules(request.targetModules()));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e)));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", safeMessage(e)));
        }
    }

    @PostMapping("/pod-draft")
    public ResponseEntity<?> generatePodManifestDraft(@RequestBody Map<String, String> req) {
        String description = req.getOrDefault("description", "").trim();
        if (description.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Pod 업무 설명을 입력해주세요."));
        try {
            String moduleName = req.getOrDefault("moduleName", "dat-was-cus").trim();
            if (!moduleName.startsWith("dat-was-")) moduleName = "dat-was-" + moduleName;
            List<String> targetModules = targetModules(req.get("targetModules"));
            String prompt = """
                    Generate only YAML for an MCP tool service manifest.
                    The root must be mcp.manifest.routing-functions with one routing function.
                    Include name, server-id, category-key, product-boundary, business-domain,
                    business-outcome, primary-entities, capabilities, select-if, reject-if,
                    confusable-servers, decision-policy, and description-serialization.
                    Except for machine identifiers and YAML keys (name, server-id, category-key), write every value in Korean.
                    The routing function name must be exactly route_to_<moduleName>, preserving hyphens (for example route_to_dat-was-cus).
                    description_serialization is the Korean tool description generated from business-outcome and decision-policy;
                    do not put JSON, MCP tags, or implementation instructions inside description_serialization.
                    confusable-servers must contain only other plausible target server IDs and must never contain the current server-id.
                    descriptions, business domains, outcomes, entities, capabilities, selection/rejection rules,
                    and decision policy must all be natural and specific Korean text.
                    Enclose every scalar string value in double quotes so Korean text containing a colon (for example "예: 급여") remains valid YAML.
                    Use valid YAML only, without Markdown fences or explanations.
                    Pod module: %s
                    Business description: %s
                    """.formatted(moduleName, description);
            String content = PodScaffolder.normalizeToolServiceManifest(
                    stripCodeFence(generateAiContent(prompt, req.get("model"))), moduleName, targetModules);
            return ResponseEntity.ok(Map.of("toolServiceManifest", content));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI Manifest 초안 생성 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/pod-new-draft")
    public ResponseEntity<?> generateNewPodManifestDraft(@RequestBody Map<String, String> req) {
        String description = req.getOrDefault("description", "").trim();
        if (description.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Pod 업무 설명을 입력해주세요."));
        try {
            String moduleName = req.getOrDefault("moduleName", "dat-was-new").trim();
            if (!moduleName.startsWith("dat-was-")) moduleName = "dat-was-" + moduleName;
            List<String> targetModules = targetModules(req.get("targetModules"));
            String prompt = """
                    Generate only YAML for an MCP tool service manifest.
                    The root must be mcp.manifest.routing-functions with one routing function.
                    Include name, server-id, category-key, product-boundary, business-domain,
                    business-outcome, primary-entities, capabilities, select-if, reject-if,
                    confusable-servers, decision-policy, and description-serialization.
                    Except for machine identifiers and YAML keys (name, server-id, category-key), write every value in Korean.
                    The routing function name must be exactly route_to_<moduleName>, preserving hyphens.
                    Use valid YAML only, without Markdown fences or explanations.
                    Pod module: %s
                    Business description: %s
                    """.formatted(moduleName, description);
            String content = NewPodProjectScaffolder.normalizeToolServiceManifest(
                    stripCodeFence(generateAiContent(prompt, req.get("model"))), moduleName, targetModules);
            return ResponseEntity.ok(Map.of("toolServiceManifest", content));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI Manifest 초안 생성 실패: " + safeMessage(e)));
        }
    }

    @GetMapping("/browse-folder")
    public String browseFolder() {
        try {
            System.setProperty("java.awt.headless", "false");
            javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
            chooser.setCurrentDirectory(new java.io.File("c:\\eGovFrameDev-4.3.1-64bit\\workspace-egov"));
            chooser.setDialogTitle("Select Workspace Folder");
            chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile().getAbsolutePath();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
        return "";
    }

    @PostMapping("/tool")
    public String scaffoldTool(@RequestBody Map<String, String> req) {
        try {
            String baseName = req.get("baseName");
            String interfaceId = req.get("interfaceId");
            String title = req.get("title");
            if (title == null || title.isBlank()) title = baseName;
            String description = req.get("description");
            String group = req.getOrDefault("categoryKey", req.getOrDefault("group", "COMMON"));
            String routingType = req.getOrDefault("routingType", "MCI");
            if ("HTTP".equalsIgnoreCase(routingType)) {
                if (interfaceId == null || interfaceId.isBlank()) {
                    interfaceId = "HTTP0000001";
                }
            }
            String moduleName = req.getOrDefault("moduleName", "dat-was-cus");
            String author = req.get("author");
            if (author == null || author.trim().isEmpty()) author = System.getProperty("user.name");
            String date = req.get("date");
            if (date == null || date.trim().isEmpty()) date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            boolean register = Boolean.parseBoolean(req.getOrDefault("register", "true"));
            String clientSystemCode = req.get("clientSystemCode");
            String httpApiName = req.get("httpApiName");
            String inputSchemaResource = req.get("inputSchemaResource");
            String outputSchemaResource = req.get("outputSchemaResource");
            List<ToolScaffolder.FieldDefinition> inputFields = parseFields(req.get("inputFields"));
            List<ToolScaffolder.FieldDefinition> outputFields = parseFields(req.get("outputFields"));
            if (inputFields.isEmpty()) {
                inputFields = List.of(new ToolScaffolder.FieldDefinition("query", "String", "Search query", List.of("example"), "", false));
            }
            ToolScaffolder.ToolDefinitionOptions definitionOptions = new ToolScaffolder.ToolDefinitionOptions(
                    req.get("functionDescription"),
                    req.get("whenToUse"),
                    req.get("whenNotToUse"),
                    req.get("ioLimits"),
                    req.get("displayDescription"),
                    parseDelimited(req.get("exampleQueries")),
                    parseDelimited(req.get("tags")),
                    req.get("ownerOrg"),
                    positiveLongOrNull(req.get("timeoutMillis")),
                    positiveIntOrNull(req.get("retryMaxAttempts")),
                    req.get("pagingMode"));

            String defaultWorkspace = DEFAULT_WORKSPACE;
            String workspacePath = req.getOrDefault("workspacePath", defaultWorkspace);
            if (workspacePath.trim().isEmpty()) {
                workspacePath = defaultWorkspace;
            }
            System.setProperty("AXHUB_SOURCE_DIR", workspacePath);

            return ToolScaffolder.scaffold(baseName, interfaceId, title, description, group, routingType,
                    moduleName, author, date, register, clientSystemCode, inputSchemaResource,
                    outputSchemaResource, inputFields, outputFields, httpApiName, definitionOptions);
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    @PostMapping("/tool-group")
    public String scaffoldToolGroup(@RequestBody ToolGroupRequest request) {
        try {
            if (request == null || request.useCaseName() == null
                    || !request.useCaseName().trim().matches("^[A-Z][A-Za-z0-9]*$")) {
                throw new IllegalArgumentException("UseCase name must be PascalCase.");
            }
            String moduleName = request.moduleName() == null || request.moduleName().isBlank()
                    ? "dat-was-cus" : request.moduleName().trim();
            String author = request.author() == null || request.author().isBlank()
                    ? System.getProperty("user.name") : request.author().trim();
            String date = request.date() == null || request.date().isBlank()
                    ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) : request.date().trim();
            String workspacePath = request.workspacePath() != null && !request.workspacePath().isBlank()
                    ? request.workspacePath().trim() : DEFAULT_WORKSPACE;
            System.setProperty("AXHUB_SOURCE_DIR", workspacePath);
            List<ToolScaffolder.ToolMethodDefinition> normalizedTools = (request.tools() == null ? List.<ToolScaffolder.ToolMethodDefinition>of() : request.tools()).stream()
                    .map(tool -> {
                        if (tool != null && "HTTP".equalsIgnoreCase(tool.routingType())) {
                            String itrfId = (tool.interfaceId() == null || tool.interfaceId().isBlank())
                                    ? "HTTP0000001" : tool.interfaceId().trim();
                            return new ToolScaffolder.ToolMethodDefinition(
                                    tool.baseName(), tool.methodName(), itrfId, tool.title(),
                                    tool.description(), tool.group(), tool.routingType(),
                                    tool.register(), tool.clientSystemCode(), tool.httpApiName(),
                                    tool.inputFields(), tool.outputFields(), tool.definitionOptions());
                        }
                        return tool;
                    }).toList();
            return ToolScaffolder.scaffoldUseCase(request.useCaseName().trim(), moduleName, author, date,
                    normalizedTools);
        } catch (Exception e) {
            return "Error: " + safeMessage(e);
        }
    }

    @GetMapping("/usecases")
    public List<String> listUseCases(@RequestParam("moduleName") String moduleName, @RequestParam("categoryKey") String categoryKey, @RequestParam(value = "workspacePath", required = false) String workspacePath) {
        if (moduleName == null || !moduleName.matches("^dat-was-[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid target module.");
        }
        if (categoryKey == null || !categoryKey.matches("^[a-z0-9]{3}$")) {
            throw new IllegalArgumentException("Invalid domain category.");
        }
        String sourceDir = (workspacePath != null && !workspacePath.isBlank()) ? workspacePath : System.getProperty("AXHUB_SOURCE_DIR");
        if (sourceDir == null || sourceDir.isBlank()) sourceDir = System.getenv("AXHUB_SOURCE_DIR");
        if (sourceDir == null || sourceDir.isBlank()) sourceDir = System.getProperty("user.dir");
        File useCaseDir = new File(sourceDir, moduleName + "/src/main/java/io/shinhanlife/dat/mcc/biz/"
                + categoryKey + "/usecase");
        File[] files = useCaseDir.listFiles(file -> file.isFile() && file.getName().endsWith("UseCase.java"));
        if (files == null) return List.of();
        return Arrays.stream(files).map(File::getName)
                .map(name -> name.substring(0, name.length() - ".java".length()))
                .sorted().toList();
    }

    @GetMapping("/categories")
    public List<String> listCategories(@RequestParam("moduleName") String moduleName, @RequestParam(value = "workspacePath", required = false) String workspacePath) {
        if (moduleName == null || !moduleName.matches("^dat-was-[a-z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid target module.");
        }
        String sourceDir = (workspacePath != null && !workspacePath.isBlank()) ? workspacePath : System.getProperty("AXHUB_SOURCE_DIR");
        if (sourceDir == null || sourceDir.isBlank()) sourceDir = System.getenv("AXHUB_SOURCE_DIR");
        if (sourceDir == null || sourceDir.isBlank()) sourceDir = System.getProperty("user.dir");
        File bizDir = new File(sourceDir, moduleName + "/src/main/java/io/shinhanlife/dat/mcc/biz");
        File[] dirs = bizDir.listFiles(File::isDirectory);
        if (dirs == null) return List.of();
        return Arrays.stream(dirs).map(File::getName)
                .filter(name -> name.matches("^[a-z0-9]{3}$"))
                .sorted().toList();
    }

    @PostMapping("/field-draft")
    public ResponseEntity<?> generateFieldDraft(@RequestBody Map<String, String> req) {
        String description = req.getOrDefault("description", "").trim();
        String target = req.getOrDefault("target", "inputFields");
        if (description.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "설명을 입력해주세요."));
        }
        if (!"inputFields".equals(target) && !"outputFields".equals(target)) {
            return ResponseEntity.badRequest().body(Map.of("error", "지원하지 않는 필드 대상입니다."));
        }

        try {
            String targetLabel = "inputFields".equals(target) ? "INPUT request DTO" : "OUTPUT response DTO";
            String prompt = """
                    Generate Java DTO fields for an MCP tool.
                    Return JSON only. Do not add Markdown, explanations, or code fences.
                    The response must have this exact shape:
                    {"fields":[{"name":"camelCaseName","type":"String","description":"short description","examples":["example1","example2"],"pattern":"^regex$","required":true,"enumValues":[],"itemType":null,"itemFields":[]}]}
                    Allowed type values: String, Integer, Long, Double, Boolean, BigDecimal, List.
                    Finite values should be enforced by populating enumValues on standard types. List fields must include itemType; use Object plus itemFields for object lists.
                    If applicable, provide a regex for pattern.
                    Generate fields only for the requested target: %s.
                    For OUTPUT fields, include resultCode and resultMessage when appropriate.
                    Keep field names valid Java camelCase identifiers. Generate at most 10 fields.
                    User description: %s
                    """.formatted(targetLabel, description);

            String response = generateAiContent(prompt, req.get("model"));
            FieldDraft draft = objectMapper.readValue(stripCodeFence(response), FieldDraft.class);
            List<ToolScaffolder.FieldDefinition> fields = validateFields(draft.fields());
            return ResponseEntity.ok(Map.of("fields", fields));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI 초안 생성 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/tool-draft")
    public ResponseEntity<?> generateToolDraft(@RequestBody Map<String, String> req) {
        String description = req.getOrDefault("description", "").trim();
        if (description.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tool 설명을 입력해주세요."));
        }

        try {
            String prompt = """
                    Generate an MCP Tool scaffold from the user request according to ShinhanLife V17 Tool Schema prerequisites and rules based on the 6 guidelines.
                    Return JSON only. Do not add Markdown, explanations, or code fences.
                    The response must have this exact shape:
                    {"baseName":"PascalCase3PartName","title":"short Korean title","description":"clear Korean LLM tool guidance","categoryKey":"cmm","routingType":"MCI","httpApiName":"simple-api-name","functionDescription":"core business function","displayDescription":"short portal description","whenToUse":"specific user requests that should select this tool","whenNotToUse":"requests or conditions that must not select this tool","ioLimits":"allowed input and output scope and limits","exampleQueries":["query 1","query 2","query 3"],"tags":["categoryKey","한국어태그1","한국어태그2","engTag1","engTag2"],"ownerOrg":"MCP_TOOL","inputFields":[{"name":"camelCaseName","type":"String","description":"short description","examples":["example1","example2"],"pattern":"^regex$","required":true,"enumValues":[],"itemType":null,"itemFields":[]}],"outputFields":[{"name":"resultCode","type":"String","description":"result code","examples":["SUCCESS"],"pattern":"","required":true,"enumValues":[],"itemType":null,"itemFields":[]}]}
                    [V17 RULES]
                    - Read-only & idempotent (destructive=false, idempotent=true for MCI; destructive=false, idempotent=false for HTTP). All tools are strictly read-only inquiry tools.
                    - 3-part naming for baseName: {categoryKey}_{action}_{target} in PascalCase (e.g. ProSearchFund, CusDetailContract, ProSearchFundHistory).
                      categoryKey must be exactly 3 lowercase letters (e.g. pro, cus, sal, cmm).
                      Action must be strictly one of: 'search' (multi-row / list / condition inquiry) or 'detail' (single-row / comprehensive detail inquiry).
                      Do not use 'inquiry', 'query', 'asst', or other arbitrary verbs. Target domains (history, consulting, return, balance, unclaimed, fund, contract) belong to target.
                      Target is a complete English entity word without abbreviations.
                    - description & functionDescription must state owning system in brackets (e.g. '[pro] ...'), hint on prerequisite input inquiries if IDs are required, and clarify system (Converter) default value and query period handling when omitted.
                    - displayDescription: Linked legacy screen name and function if known, or concise Korean function description.
                    - exampleQueries: 3 to 10 realistic Korean query patterns referencing the screen name and sub-function terms.
                    - tags: 3-tier structure (1: categoryKey, 2: 2-4 Korean primary query keywords, 3: 2-4 English secondary keywords, total 5-8 tags).
                    - routingType must be either MCI or HTTP. Default to MCI. Use HTTP only when the user explicitly requests a REST or HTTP integration. For HTTP, httpApiName must be a concise abbreviation in 'xxx-xxxx' format under 10 characters (e.g. cst-inq, emp-srch).
                    - Allowed field type values: String, Integer, Long, Double, Boolean, BigDecimal, List. Finite values should be enforced by populating enumValues. List must include itemType and object lists include itemFields.
                    - If applicable, provide a regex for pattern. Keep all field names valid Java camelCase identifiers. Generate at most 10 fields per list.
                    - If a field name is 'scrPageInfo' or 'pageInfo' (case-insensitive), DO NOT rename or convert it; keep the exact field name 'scrPageInfo' or 'pageInfo' and set required=false (system handles pagination).
                    - Do not generate interfaceId or clientSystemCode; those must come from a real integration contract.
                    User request: %s
                    """.formatted(description);

            String response = generateAiContent(prompt, req.get("model"));
            ToolDraft draft = objectMapper.readValue(stripCodeFence(response), ToolDraft.class);
            ToolDraft validatedDraft = validateToolDraft(draft);
            return ResponseEntity.ok(validatedDraft);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI Tool 초안 생성 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/tool-draft/optimize")
    public ResponseEntity<?> optimizeToolDraft(@RequestBody Map<String, Object> req) {
        String baseName = req.getOrDefault("baseName", "").toString().trim();
        String title = req.getOrDefault("title", "").toString().trim();
        String description = req.getOrDefault("description", "").toString().trim();
        String categoryKey = req.getOrDefault("categoryKey", "").toString().trim();
        String routingType = req.getOrDefault("routingType", "MCI").toString().trim();
        String httpApiName = req.getOrDefault("httpApiName", "").toString().trim();
        String functionDescription = req.getOrDefault("functionDescription", "").toString().trim();
        String displayDescription = req.getOrDefault("displayDescription", "").toString().trim();
        String whenToUse = req.getOrDefault("whenToUse", "").toString().trim();
        String whenNotToUse = req.getOrDefault("whenNotToUse", "").toString().trim();
        String ioLimits = req.getOrDefault("ioLimits", "").toString().trim();
        Object rawExampleQueries = req.get("exampleQueries");
        Object rawTags = req.get("tags");
        String ownerOrg = req.getOrDefault("ownerOrg", "MCP_TOOL").toString().trim();
        Object rawInputFields = req.get("inputFields");
        Object rawOutputFields = req.get("outputFields");
        String model = req.get("model") != null ? req.get("model").toString() : null;

        if (title.isBlank() && description.isBlank() && baseName.isBlank() && displayDescription.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "최적화할 Tool 정보를 입력해주세요."));
        }

        try {
            String inputFieldsJson = rawInputFields != null ? objectMapper.writeValueAsString(rawInputFields) : "[]";
            String outputFieldsJson = rawOutputFields != null ? objectMapper.writeValueAsString(rawOutputFields) : "[]";
            String exampleQueriesJson = rawExampleQueries != null ? objectMapper.writeValueAsString(rawExampleQueries) : "[]";
            String tagsJson = rawTags != null ? objectMapper.writeValueAsString(rawTags) : "[]";
            String effectiveCatKey = categoryKey.isBlank() ? "cmm" : categoryKey;
            String effectiveDisplayDesc = displayDescription.isBlank() ? title : displayDescription;

            String prompt = """
                    Optimize and re-evaluate this MCP Tool schema according to strict ShinhanLife V17 Tool Schema prerequisites and rules based on the 6 guidelines.
                    Return JSON only. Do not add Markdown, explanations, or code fences.
                    Response must follow this exact shape:
                    {"baseName":"PascalCase3PartName","title":%s,"description":"...","categoryKey":"%s","routingType":"%s","httpApiName":"%s","functionDescription":"...","displayDescription":%s,"whenToUse":"...","whenNotToUse":"...","ioLimits":"...","exampleQueries":["query1","query2","query3"],"tags":["%s","한국어태그1","한국어태그2","engTag1","engTag2"],"ownerOrg":"%s","inputFields":%s,"outputFields":%s}

                    [CRITICAL V17 OPTIMIZATION RULES - 6 GUIDELINE PRINCIPLES]
                    1. 궁극적 목적 (Ultimate Purpose) & Agent LLM 단독 수행:
                       - 툴 호출 판단(툴 선택, 호출 여부, 호출 순서)은 오직 Agent Builder의 LLM이 단독으로 수행한다. 시스템(Converter 포함)은 LLM의 툴 호출 판단에 관여하지 않는다.
                       - 시스템(Converter)의 역할은 LLM이 호출을 결정한 이후 파라미터 기본값, 기본 기간 세팅 등 기술적인 처리에만 한정된다.
                    2. 최상위 제약 - Read-Only 원칙:
                       - 모든 툴은 순수 조회용이다 (destructive=false, idempotent=true for MCI; destructive=false, idempotent=false for HTTP).
                       - 등록, 수정, 삭제, 변경을 수행하거나 지시하는 코드는 절대 포함하지 않는다.
                    3. 스케일 고려 (다중 MCP 등록, MCP당 툴 50개 이하 전제):
                       - LLM이 수십 개 툴 중에서 시맨틱 검색으로 정확히 탐색할 수 있도록 tags, exampleQueries 품질을 극대화한다.
                       - 업무단위 접두사(categoryKey: pro, cus, sal, cmm 등)가 1차 네임스페이스 역할을 수행한다.
                       - 유사한 툴 간의 명확한 경계 구분은 whenNotToUse를 적극 활용하여 기술한다.
                    4. Ground Truth & Immutable Fields (화면 기능의 실체화 - 절대 수정 불가):
                       - title: 사용자가 입력한 타이틀 그대로 100%% 유지 (절대 임의 변경 금지: %s).
                       - displayDescription: 연계 화면명 (고정값, 예: [NSAK0060]...) 그대로 100%% 유지 (절대 임의 변경 금지: %s).
                    5. 네이밍 규칙 2-12 ({업무단위}_{기능}_{대상} 3단 구조 -> PascalCase baseName):
                       - 1단 (업무단위): categoryKey (소문자 3자, e.g. pro, cus, sal, cmm).
                       - 2단 (기능/작업): 조회성 툴은 오직 'search' (다건/목록 조회) 또는 'detail' (단건/상세 조회) 2가지만 사용!
                         'inquiry', 'query', 'asst' 등 임의의 기능어 혼용 절대 금지.
                       - 3단 (대상/영역): 다루는 대상(컨설팅, 이력, 수익률, 펀드 등)은 모두 3단에 배치! (e.g. Variable, Fund, Contract, Fee, Disclosure, History, Consulting, Return, Balance, Unclaimed).
                         완전한 영어 단어 사용 (축약어 금지).
                       - baseName은 이 3단을 PascalCase로 결합 (예: ProSearchFund, CusDetailContract, ProSearchFundHistory, ProDetailContractReturn).
                       - routingType이 HTTP일 경우, httpApiName은 10자 미만 'xxx-xxxx' 형식의 축약 영문 소문자.
                    6. description & functionDescription 작성 지침:
                       - 연계 화면 기능의 본래 의도(Ground Truth) 유지.
                       - 소속 시스템/채널을 대괄호로 명시 (예: '[%s] ...').
                       - 선행 조회 힌트: 필수 식별값(계약번호, 고객ID, 변경신청ID 등)을 모를 경우 선행 조회 툴을 먼저 호출해야 함을 명시.
                       - 시스템(Converter) 기본값/기간 위임 명시: 파라미터가 없거나 빈 값으로 호출 시 시스템(Converter)에서 기본값 및 기본 기간(예: 당일, 최근 1년 등)을 세팅한다는 내용을 명시.
                    7. exampleQueries 설계 (3 to 10개):
                       - 연계 화면명(displayDescription: %s)의 기능 용어(하위 기능명)를 반드시 참조하여 설계.
                       - 화면명에 포함된 핵심 키워드를 반드시 활용하여 사용자가 실제 업무에서 물어볼 법한 3~10개의 현실적인 질문 패턴 작성.
                    8. tags 3중 구조 (5 to 8개 태그 유지):
                       - 1단: 업무단위 (categoryKey: '%s')
                       - 2단: 한글 주 태그 (2~4개, 사용자가 질문할 만한 핵심 한국어 키워드)
                       - 3단: 영문 보조 태그 (2~4개, 시맨틱 검색을 보조할 영문 키워드)
                       - 총 5~8개 태그 유지. 노이즈 방지.
                    9. whenToUse & whenNotToUse:
                       - whenToUse: LLM이 이 툴을 선택해야 하는 구체적인 사용자 질문/상황 명시.
                       - whenNotToUse: 유사 툴(목록 vs 상세, 타 업무 도메인)과의 경계 및 수정/신청/해지 등 CUD 작업 요청 시 호출 불가 조건 명시.
                    10. ioLimits:
                       - 허용되는 입력 범위, 반환 DTO 범위, 건수 제한, 기본값/기본기간 처리 방식 명시.
                    11. inputFields & outputFields:
                       - 필드는 최대 10개 이내로 관리.
                       - 시스템 위임 및 페이징: 'scrPageInfo' 및 'pageInfo' (대소문자 무관)는 절대 자연어로 변환하거나 필드명을 변경하지 말고 원형 유지.
                       - 페이징 객체는 LLM의 필수 입력값이 아니라 시스템에 위임하는 객체이므로 required: false로 설정하고 기본값 처리 안내.

                    [CURRENT TOOL DRAFT DATA TO OPTIMIZE]
                    BaseName: %s
                    Title: %s
                    Description: %s
                    CategoryKey: %s
                    RoutingType: %s
                    HttpApiName: %s
                    FunctionDescription: %s
                    DisplayDescription: %s
                    WhenToUse: %s
                    WhenNotToUse: %s
                    IoLimits: %s
                    ExampleQueries: %s
                    Tags: %s
                    OwnerOrg: %s
                    InputFields: %s
                    OutputFields: %s
                    """.formatted(
                    objectMapper.writeValueAsString(title),
                    effectiveCatKey,
                    routingType.isBlank() ? "MCI" : routingType,
                    httpApiName.isBlank() ? "http-api" : httpApiName,
                    objectMapper.writeValueAsString(effectiveDisplayDesc),
                    effectiveCatKey,
                    ownerOrg.isBlank() ? "MCP_TOOL" : ownerOrg,
                    inputFieldsJson,
                    outputFieldsJson,
                    objectMapper.writeValueAsString(title),
                    objectMapper.writeValueAsString(effectiveDisplayDesc),
                    effectiveCatKey,
                    objectMapper.writeValueAsString(effectiveDisplayDesc),
                    effectiveCatKey,
                    baseName,
                    title,
                    description,
                    categoryKey,
                    routingType,
                    httpApiName,
                    functionDescription,
                    displayDescription,
                    whenToUse,
                    whenNotToUse,
                    ioLimits,
                    exampleQueriesJson,
                    tagsJson,
                    ownerOrg,
                    inputFieldsJson,
                    outputFieldsJson
            );

            String response = generateAiContent(prompt, model);
            ToolDraft draft = objectMapper.readValue(stripCodeFence(response), ToolDraft.class);

            // Guarantee Rule 2: Ground Truth & Immutable fields
            String finalTitle = !title.isBlank() ? title : draft.title();
            String finalDisplayDesc = !displayDescription.isBlank() ? displayDescription : draft.displayDescription();
            String finalBaseName = normalizeBaseName(draft.baseName());

            ToolDraft finalDraft = new ToolDraft(
                    finalBaseName,
                    finalTitle,
                    draft.description(),
                    draft.categoryKey(),
                    draft.routingType(),
                    draft.httpApiName(),
                    draft.functionDescription(),
                    finalDisplayDesc,
                    draft.whenToUse(),
                    draft.whenNotToUse(),
                    draft.ioLimits(),
                    draft.exampleQueries(),
                    draft.tags(),
                    draft.ownerOrg(),
                    draft.inputFields(),
                    draft.outputFields()
            );

            ToolDraft validatedDraft = validateToolDraft(finalDraft);
            return ResponseEntity.ok(validatedDraft);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "AI Tool V17 스키마 최적화 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/mci-response/analyze")
    public ResponseEntity<?> analyzeMciResponse(@RequestBody MciResponseAnalyzeRequest request) {
        if (request == null || request.source() == null || request.source().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "XXXX_O.java 소스를 입력해주세요."));
        }
        try {
            MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(request.source());
            List<Map<String, Object>> allFields = extractFields(parsed);
            AiMciMappingDraft draft = analyzeFieldsInChunks(allFields, request.model(), false);
            List<MciResponseScaffolder.FieldMapping> mappings = normalizeAiMappings(parsed, draft);
            return ResponseEntity.ok(new MciResponseAnalyzeResponse(parsed, mappings));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "MCI Response AI 분석 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/mci-response/generate")
    public ResponseEntity<?> generateMciResponse(@RequestBody MciResponseGenerateRequest request) {
        if (request == null || request.source() == null || request.source().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "XXXX_O.java 소스를 입력해주세요."));
        }
        try {
            MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(request.source());
            MciResponseScaffolder.GeneratedSources generated = MciResponseScaffolder.generate(
                    parsed,
                    request.responsePackage(),
                    request.responseClassName(),
                    request.converterPackage(),
                    request.converterClassName(),
                    request.mappings());
            return ResponseEntity.ok(generated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "MCI Response 소스 생성 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/mci-request/analyze")
    public ResponseEntity<?> analyzeMciRequest(@RequestBody MciResponseAnalyzeRequest request) {
        if (request == null || request.source() == null || request.source().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "XXXX_I.java 소스를 입력해주세요."));
        }
        try {
            MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(request.source());
            List<Map<String, Object>> allFields = extractFields(parsed);
            AiMciMappingDraft draft = analyzeFieldsInChunks(allFields, request.model(), true);
            List<MciResponseScaffolder.FieldMapping> mappings = normalizeAiMappings(parsed, draft);
            return ResponseEntity.ok(new MciResponseAnalyzeResponse(parsed, mappings));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "MCI Request AI 분석 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/mci-request/generate")
    public ResponseEntity<?> generateMciRequest(@RequestBody MciRequestGenerateRequest request) {
        if (request == null || request.source() == null || request.source().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "XXXX_I.java 소스를 입력해주세요."));
        }
        try {
            MciResponseScaffolder.ParsedSource parsed = MciResponseScaffolder.parse(request.source());
            MciResponseScaffolder.GeneratedRequestSources generated = MciResponseScaffolder.generateRequest(
                    parsed,
                    request.requestPackage(),
                    request.requestClassName(),
                    request.converterPackage(),
                    request.converterClassName(),
                    request.mappings());
            return ResponseEntity.ok(generated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", safeMessage(e)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "MCI Request 소스 생성 실패: " + safeMessage(e)));
        }
    }

    @PostMapping("/tool/update")
    public String updateTool(@RequestBody Map<String, String> req) {
        try {
            String toolName = req.get("toolName");
            String domainGroup = req.getOrDefault("categoryKey", req.get("domainGroup"));
            String description = req.get("description");
            boolean register = Boolean.parseBoolean(req.getOrDefault("register", "true"));
            Boolean requiresApproval = req.containsKey("requiresApproval") ? Boolean.parseBoolean(req.get("requiresApproval")) : null;
            
            ToolSourceUpdater.updateToolSource(toolName, domainGroup, description, register, requiresApproval);
            return "성공";
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    @GetMapping("/modules")
    public List<String> listModules(@RequestParam(value = "workspacePath", required = false) String workspacePath) {
        try {
            String sourceDir = (workspacePath != null && !workspacePath.isBlank()) ? workspacePath : System.getProperty("AXHUB_SOURCE_DIR");
            if (sourceDir == null) sourceDir = System.getenv("AXHUB_SOURCE_DIR");
            if (sourceDir == null) sourceDir = System.getProperty("user.dir");
            
            File dir = NewPodProjectScaffolder.resolveWorkspaceRoot(Path.of(sourceDir)).toFile();
            File[] files = dir.listFiles(f -> f.isDirectory() && f.getName().startsWith("dat-was-") && !f.getName().equals("dat-was-lib"));
            
            if (files == null || files.length == 0) {
                return List.of("dat-was-cus", "dat-was-sal", "dat-was-pro", "dat-was-sys");
            }
            
            return Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return List.of("dat-was-cus", "dat-was-sal", "dat-was-pro", "dat-was-sys");
        }
    }

    private List<ToolScaffolder.FieldDefinition> parseFields(String source) throws Exception {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(source, new TypeReference<List<ToolScaffolder.FieldDefinition>>() { });
    }

    private List<MciResponseScaffolder.FieldMapping> normalizeAiMappings(
            MciResponseScaffolder.ParsedSource parsed, AiMciMappingDraft draft) {
        Map<String, AiMciFieldMapping> suggestions = new java.util.LinkedHashMap<>();
        if (draft != null && draft.mappings() != null) {
            for (AiMciFieldMapping mapping : draft.mappings()) {
                if (mapping == null || mapping.ownerType() == null || mapping.sourceName() == null) continue;
                suggestions.put(mapping.ownerType() + "#" + mapping.sourceName(), mapping);
            }
        }

        List<MciResponseScaffolder.FieldMapping> result = new java.util.ArrayList<>();
        Map<String, Set<String>> usedTargets = new java.util.HashMap<>();
        for (MciResponseScaffolder.ParsedType type : parsed.types()) {
            Set<String> used = usedTargets.computeIfAbsent(type.name(), ignored -> new LinkedHashSet<>());
            for (MciResponseScaffolder.ParsedField field : type.fields()) {
                AiMciFieldMapping suggestion = suggestions.get(type.name() + "#" + field.name());
                String targetName = suggestion == null ? field.name() : suggestion.targetName();
                targetName = targetName == null ? "" : targetName.trim();
                if ("scrPageInfo".equalsIgnoreCase(field.name())) {
                    targetName = "scrPageInfo";
                } else if ("pageInfo".equalsIgnoreCase(field.name())) {
                    targetName = "pageInfo";
                } else if (!targetName.matches("^[a-z][A-Za-z0-9]*$") || used.contains(targetName)) {
                    targetName = field.name();
                }
                if (!targetName.matches("^[a-z][A-Za-z0-9]*$") || !used.add(targetName)) {
                    throw new IllegalArgumentException(
                            "AI가 중복되거나 올바르지 않은 필드명을 생성했습니다: " + type.name() + "." + field.name());
                }
                boolean include = suggestion == null || suggestion.include() == null || suggestion.include();
                result.add(new MciResponseScaffolder.FieldMapping(
                        type.name(), field.name(), targetName, include));
            }
        }
        return List.copyOf(result);
    }

    private List<String> parseDelimited(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return Arrays.stream(source.split("[\\r\\n,]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<ToolScaffolder.FieldDefinition> validateFields(List<ToolScaffolder.FieldDefinition> source) {
        return validateFields(source, false);
    }

    private List<ToolScaffolder.FieldDefinition> validateFields(List<ToolScaffolder.FieldDefinition> source, boolean allowEmpty) {
        if (source == null || source.isEmpty()) {
            if (allowEmpty) {
                return List.of();
            }
            throw new IllegalArgumentException("AI가 필드를 생성하지 않았습니다.");
        }
        Set<String> names = new LinkedHashSet<>();
        return source.stream()
                .limit(10)
                .filter(field -> field != null && field.name() != null && !field.name().isBlank())
                .map(field -> {
                    String name = field.name().trim();
                    boolean isPaging = "scrPageInfo".equalsIgnoreCase(name) || "pageInfo".equalsIgnoreCase(name);
                    String desc = field.description() == null ? "" : field.description().trim();
                    if (isPaging && desc.isBlank()) {
                        desc = "페이징 처리 객체 (생략 시 시스템 기본값 적용)";
                    }
                    boolean required = isPaging ? false : field.required();
                    return new ToolScaffolder.FieldDefinition(
                            name,
                            field.type() == null ? "String" : field.type().trim(),
                            desc,
                            field.examples() == null ? List.of() : field.examples().stream().map(String::trim).toList(),
                            field.pattern() == null ? "" : field.pattern().trim(),
                            required,
                            field.enumValues() == null ? List.of() : field.enumValues(),
                            field.itemType(),
                            field.itemFields() == null ? List.of() : field.itemFields());
                })
                .peek(field -> {
                    if (!field.name().matches("^[A-Za-z_$][A-Za-z0-9_$]*$")) {
                        throw new IllegalArgumentException("AI가 올바르지 않은 필드명을 생성했습니다: " + field.name());
                    }
                    if (!SUPPORTED_FIELD_TYPES.contains(field.type())) {
                        throw new IllegalArgumentException("AI가 지원하지 않는 Type을 생성했습니다: " + field.type());
                    }
                    validateStructuredField(field);
                    if (!names.add(field.name())) {
                        throw new IllegalArgumentException("AI가 중복 필드명을 생성했습니다: " + field.name());
                    }
                })
                .toList();
    }

    private void validateStructuredField(ToolScaffolder.FieldDefinition field) {
        if ("List".equals(field.type()) && (field.itemType() == null || field.itemType().isBlank())) {
            throw new IllegalArgumentException("List field needs itemType: " + field.name());
        }
        if ("List".equals(field.type()) && "Object".equals(field.itemType()) && field.itemFields().isEmpty()) {
            throw new IllegalArgumentException("Object List field needs itemFields: " + field.name());
        }
    }

    private String normalizeBaseName(String name) {
        if (name == null || name.isBlank()) return "";
        String trimmed = name.trim();
        if (trimmed.contains("_") || trimmed.contains("-")) {
            String[] parts = trimmed.split("[_-]+");
            StringBuilder sb = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    sb.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        sb.append(part.substring(1));
                    }
                }
            }
            return sb.toString();
        }
        if (Character.isLowerCase(trimmed.charAt(0))) {
            return Character.toUpperCase(trimmed.charAt(0)) + (trimmed.length() > 1 ? trimmed.substring(1) : "");
        }
        return trimmed;
    }

    private ToolDraft validateToolDraft(ToolDraft draft) {
        if (draft == null) {
            throw new IllegalArgumentException("AI가 Tool 초안을 생성하지 않았습니다.");
        }
        String baseName = normalizeBaseName(draft.baseName());
        if (baseName.isBlank() || !baseName.matches("^[A-Z][A-Za-z0-9]*$")) {
            throw new IllegalArgumentException("AI가 올바르지 않은 Base Name을 생성했습니다: " + draft.baseName());
        }
        String categoryKey = draft.categoryKey() == null ? "" : draft.categoryKey().trim().toLowerCase(Locale.ROOT);
        if (!categoryKey.matches("^[a-z0-9]{3}$")) {
            throw new IllegalArgumentException("AI가 올바르지 않은 Category Key를 생성했습니다.");
        }
        String routingType = draft.routingType() == null ? "" : draft.routingType().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("HTTP", "MCI").contains(routingType)) {
            throw new IllegalArgumentException("AI가 지원하지 않는 Protocol을 생성했습니다.");
        }
        String httpApiName = draft.httpApiName() == null ? "" : draft.httpApiName().trim();
        if ("HTTP".equals(routingType)) {
            if (httpApiName.isBlank() || !httpApiName.matches("^[a-z0-9]+-[a-z0-9]+$") || httpApiName.length() >= 10) {
                httpApiName = ToolScaffolder.toAbbreviatedHttpApiName(baseName);
            }
            baseName = ToolScaffolder.toPascalCase(httpApiName);
        } else {
            if (httpApiName.isBlank()) {
                httpApiName = "http-api";
            } else if (!httpApiName.matches("^[A-Za-z0-9_-]+$")) {
                throw new IllegalArgumentException("AI가 올바르지 않은 HTTP API Name을 생성했습니다.");
            }
        }
        String title = draft.title() == null ? "" : draft.title().trim();
        String description = draft.description() == null ? "" : draft.description().trim();
        if (title.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("AI가 Tool 제목 또는 설명을 생성하지 않았습니다.");
        }
        String functionDescription = textOrDefault(draft.functionDescription(), description);
        String displayDescription = textOrDefault(draft.displayDescription(), title);
        String whenToUse = textOrDefault(draft.whenToUse(), description + " 요청을 처리할 때 사용한다.");
        String whenNotToUse = textOrDefault(draft.whenNotToUse(), "필수 입력값이 없거나 다른 업무 요청에는 사용하지 않는다.");
        String ioLimits = textOrDefault(draft.ioLimits(), "정의된 입력 필드만 허용하며 정의된 응답 DTO 범위만 반환한다.");
        List<String> exampleQueries = normalizedDraftList(draft.exampleQueries(), List.of(
                title + " 해줘", title + " 정보를 알려줘", title + " 결과를 확인해줘"));
        List<String> tags = normalizedDraftList(draft.tags(), List.of(categoryKey));
        String ownerOrg = textOrDefault(draft.ownerOrg(), "MCP_TOOL");
        return new ToolDraft(baseName, title, description, categoryKey, routingType, httpApiName,
                functionDescription, displayDescription, whenToUse, whenNotToUse, ioLimits,
                exampleQueries, tags, ownerOrg,
                validateFields(draft.inputFields(), true), validateFields(draft.outputFields(), true));
    }

    private String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private List<String> normalizedDraftList(List<String> values, List<String> fallback) {
        if (values == null) {
            return fallback;
        }
        List<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? fallback : normalized;
    }


    private List<Map<String, Object>> extractFields(MciResponseScaffolder.ParsedSource parsed) {
        return parsed.types().stream()
                .flatMap(type -> type.fields().stream().map(field -> Map.of(
                        "ownerType", (Object) type.name(),
                        "sourceName", field.name(),
                        "description", field.description(),
                        "javaType", field.type(),
                        "sensitive", field.sensitive())))
                .toList();
    }

    private AiMciMappingDraft analyzeFieldsInChunks(List<Map<String, Object>> allFields, String requestedModel, boolean isRequest) throws Exception {
        int CHUNK_SIZE = 20;
        List<List<Map<String, Object>>> chunks = new java.util.ArrayList<>();
        for (int i = 0; i < allFields.size(); i += CHUNK_SIZE) {
            chunks.add(allFields.subList(i, Math.min(i + CHUNK_SIZE, allFields.size())));
        }

        String typeStr = isRequest ? "request" : "response";
        String promptTemplate = "You rename legacy Korean financial-system " + typeStr + " fields for an MCP Tool " + typeStr + " DTO.\n" +
                "Return JSON only with this exact shape:\n" +
                "{\"mappings\":[{\"ownerType\":\"SourceOwnerClass\",\"sourceName\":\"legacyField\",\"targetName\":\"businessMeaningInEnglish\",\"include\":true}]}\n\n" +
                "Rules:\n" +
                "- Return a mapping for EVERY field in the input list, including List-type, reserved, and filler fields.\n" +
                "- targetName must be a concise, descriptive English Java camelCase identifier (e.g. 'employeeNumber', 'reservedField01').\n" +
                "- Derive the business meaning primarily from description; use sourceName only as supporting metadata.\n" +
                "- If sourceName is 'scrPageInfo' or 'pageInfo' (case-insensitive), DO NOT rename or convert it; targetName MUST remain exactly 'scrPageInfo' or 'pageInfo' respectively.\n" +
                "- For reserved/dummy/filler fields (e.g. reserved01, filler, spare), use a clean camelCase form like 'reserved01', 'filler01' as targetName and set include=true.\n" +
                "- Set include=false ONLY for fields that are explicitly obsolete or harmful to expose.\n\n" +
                "Source fields:\n" +
                "%s\n";

        List<java.util.concurrent.CompletableFuture<AiMciMappingDraft>> futures = chunks.stream()
                .map(chunk -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        String fieldsJson = objectMapper.writeValueAsString(chunk);
                        String prompt = String.format(promptTemplate, fieldsJson);
                        String aiResponse = generateAiContent(prompt, requestedModel);
                        return objectMapper.readValue(stripCodeFence(aiResponse), AiMciMappingDraft.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

        List<AiMciFieldMapping> mergedMappings = new java.util.ArrayList<>();
        for (var future : futures) {
            AiMciMappingDraft draft = future.get();
            if (draft != null && draft.mappings() != null) {
                mergedMappings.addAll(draft.mappings());
            }
        }
        return new AiMciMappingDraft(mergedMappings);
    }

    private String generateAiContent(String prompt, String requestedModel) {
        String resolvedModel = resolveModel(requestedModel);
        org.springframework.ai.chat.client.ChatClient activeChatClient;
        org.springframework.ai.chat.prompt.ChatOptions chatOptions;

        if ("Qwen3-Coder".equalsIgnoreCase(resolvedModel)
                || "Gemma-4-31B".equalsIgnoreCase(resolvedModel)
                || "GLM-5.3-Flash".equalsIgnoreCase(resolvedModel)) {
            // Use liteLlmBaseUrl from application.yml
            String apiKey;
            if ("GLM-5.3-Flash".equalsIgnoreCase(resolvedModel)) {
                apiKey = glmKey;
            } else if ("Gemma-4-31B".equalsIgnoreCase(resolvedModel)) {
                apiKey = gemmaKey;
            } else {
                apiKey = qwenKey;
            }

            org.springframework.ai.openai.api.OpenAiApi openAiApi = org.springframework.ai.openai.api.OpenAiApi.builder()
                    .baseUrl(liteLlmBaseUrl)
                    .apiKey(new org.springframework.ai.model.SimpleApiKey(apiKey))
                    .build();

            org.springframework.ai.openai.OpenAiChatModel dynamicChatModel = org.springframework.ai.openai.OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .build();

            activeChatClient = org.springframework.ai.chat.client.ChatClient.builder(dynamicChatModel).build();
            chatOptions = org.springframework.ai.openai.OpenAiChatOptions.builder().model(resolvedModel).build();
        } else if (OPENROUTER_AI_MODELS.contains(resolvedModel)) {
            requireOpenRouterApiKey();
            activeChatClient = chatClientBuilder.build();
            chatOptions = org.springframework.ai.openai.OpenAiChatOptions.builder().model(resolvedModel).build();
        } else {
            throw new IllegalArgumentException("지원하지 않는 AI 모델입니다.");
        }

        return activeChatClient.prompt()
                .user(prompt)
                .options(chatOptions)
                .call()
                .content();
    }

    private Long positiveLongOrNull(String value) {
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Integer positiveIntOrNull(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String resolveModel(String requestedModel) {
        String model = requestedModel == null || requestedModel.isBlank()
                ? "cohere/north-mini-code:free" : requestedModel.trim();
        if (!SUPPORTED_AI_MODELS.contains(model)) {
            throw new IllegalArgumentException("지원하지 않는 AI 모델입니다.");
        }
        return model;
    }

    private void requireOpenRouterApiKey() {
        if (openRouterApiKey == null || openRouterApiKey.isBlank()
                || "NOT_CONFIGURED".equals(openRouterApiKey.trim())
                || "sk-placeholder-dummy-key".equals(openRouterApiKey.trim())) {
            throw new IllegalStateException(
                    "OPENROUTER_API_KEY가 설정되지 않았습니다. Gateway 실행 환경에 실제 OpenRouter API 키를 설정한 뒤 재시작하세요.");
        }
    }

    private String stripCodeFence(String response) {
        if (response == null) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다.");
        }
        return response.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private List<String> targetModules(String rawTargetModules) throws IOException {
        if (rawTargetModules == null || rawTargetModules.isBlank()) {
            return List.of();
        }
        List<String> modules = objectMapper.readValue(rawTargetModules, new TypeReference<List<String>>() { });
        return modules.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(name -> name.matches("^dat-was-[a-z0-9-]+$"))
                .distinct()
                .toList();
    }

    private record FieldDraft(List<ToolScaffolder.FieldDefinition> fields) {
    }

    public record MciResponseAnalyzeRequest(String source, String model) {
    }

    public record MciResponseAnalyzeResponse(MciResponseScaffolder.ParsedSource parsed,
                                             List<MciResponseScaffolder.FieldMapping> mappings) {
    }

    public record MciResponseGenerateRequest(String source,
                                             String responsePackage,
                                             String responseClassName,
                                             String converterPackage,
                                             String converterClassName,
                                             List<MciResponseScaffolder.FieldMapping> mappings) {
    }

    public record MciRequestGenerateRequest(String source,
                                            String requestPackage,
                                            String requestClassName,
                                            String converterPackage,
                                            String converterClassName,
                                            List<MciResponseScaffolder.FieldMapping> mappings) {
    }

    private record AiMciMappingDraft(List<AiMciFieldMapping> mappings) {
    }

    private record AiMciFieldMapping(String ownerType, String sourceName, String targetName, Boolean include) {
    }

    public record ToolDraft(String baseName, String title, String description, String categoryKey, String routingType,
                             String httpApiName, String functionDescription, String displayDescription,
                             String whenToUse, String whenNotToUse, String ioLimits,
                             List<String> exampleQueries, List<String> tags, String ownerOrg,
                             List<ToolScaffolder.FieldDefinition> inputFields,
                             List<ToolScaffolder.FieldDefinition> outputFields) {
    }

    private record ToolGroupRequest(String useCaseName, String moduleName, String author, String date, String workspacePath,
                                    List<ToolScaffolder.ToolMethodDefinition> tools) {
    }

    private record NewPodProjectRequest(String moduleName, Integer port, String author, String workspacePath,
                                        String toolServiceManifest, String targetModules) {
    }
}
