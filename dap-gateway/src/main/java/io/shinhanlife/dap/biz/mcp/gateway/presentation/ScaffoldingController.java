package io.shinhanlife.dap.biz.mcp.gateway.presentation;


/**
 * @package io.shinhanlife.dap.biz.mcp.gateway.presentation
 * @className ScaffoldingController
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
import io.shinhanlife.dap.common.util.PodScaffolder;
import io.shinhanlife.dap.common.util.ToolScaffolder;
import io.shinhanlife.dap.common.util.ToolSourceUpdater;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scaffold")
public class ScaffoldingController {

    @PostMapping("/pod")
    public String scaffoldPod(@RequestBody Map<String, String> req) {
        try {
            String moduleName = req.getOrDefault("moduleName", "dap-tool-other");
            if (!moduleName.startsWith("dap-tool-")) moduleName = "dap-tool-" + moduleName;
            String port = req.getOrDefault("port", "8085");
            String shortName = moduleName.replace("dap-tool-", "").replace("-", "");
            String author = req.get("author");
            if (author == null || author.trim().isEmpty()) author = System.getProperty("user.name");
            String date = req.get("date");
            if (date == null || date.trim().isEmpty()) date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            
            return PodScaffolder.scaffoldPod(moduleName, port, shortName, author, date);
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }

    @PostMapping("/tool")
    public String scaffoldTool(@RequestBody Map<String, String> req) {
        try {
            String baseName = req.get("baseName");
            String interfaceId = req.get("interfaceId");
            String description = req.get("description");
            String group = req.getOrDefault("categoryKey", req.getOrDefault("group", "COMMON"));
            String routingType = req.getOrDefault("routingType", "HTTP");
            String moduleName = req.getOrDefault("moduleName", "dap-tool-other");
            String author = req.get("author");
            if (author == null || author.trim().isEmpty()) author = System.getProperty("user.name");
            String date = req.get("date");
            if (date == null || date.trim().isEmpty()) date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            boolean register = Boolean.parseBoolean(req.getOrDefault("register", "true"));
            
            return ToolScaffolder.scaffold(baseName, interfaceId, description, group, routingType, moduleName, author, date, register);
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
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
    public List<String> listModules() {
        try {
            String sourceDir = System.getenv("AXHUB_SOURCE_DIR");
            if (sourceDir == null) sourceDir = System.getProperty("user.dir");
            
            File dir = new File(sourceDir);
            File[] files = dir.listFiles(f -> f.isDirectory() && f.getName().startsWith("dap-tool-") && !f.getName().equals("dap-tool-core"));
            
            if (files == null) return List.of("dap-tool-other");
            
            return Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return List.of("dap-tool-other");
        }
    }
}
