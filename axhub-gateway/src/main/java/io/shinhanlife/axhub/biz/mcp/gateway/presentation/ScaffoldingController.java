package io.shinhanlife.axhub.biz.mcp.gateway.presentation;

import io.shinhanlife.axhub.common.util.PodScaffolder;
import io.shinhanlife.axhub.common.util.ToolScaffolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.File;

@RestController
@RequestMapping("/api/v1/scaffold")
public class ScaffoldingController {

    @PostMapping("/pod")
    public String scaffoldPod(@RequestBody Map<String, String> req) {
        try {
            String moduleName = req.getOrDefault("moduleName", "axhub-tool-other");
            if (!moduleName.startsWith("axhub-tool-")) moduleName = "axhub-tool-" + moduleName;
            String port = req.getOrDefault("port", "8085");
            String shortName = moduleName.replace("axhub-tool-", "").replace("-", "");
            String author = req.get("author");
            if (author == null || author.trim().isEmpty()) author = System.getProperty("user.name");
            String date = req.get("date");
            if (date == null || date.trim().isEmpty()) date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));
            
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
            String group = req.getOrDefault("group", "COMMON");
            String routingType = req.getOrDefault("routingType", "HTTP");
            String moduleName = req.getOrDefault("moduleName", "axhub-tool-other");
            String author = req.get("author");
            if (author == null || author.trim().isEmpty()) author = System.getProperty("user.name");
            String date = req.get("date");
            if (date == null || date.trim().isEmpty()) date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"));
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
            String domainGroup = req.get("domainGroup");
            String description = req.get("description");
            boolean register = Boolean.parseBoolean(req.getOrDefault("register", "true"));
            Boolean requiresApproval = req.containsKey("requiresApproval") ? Boolean.parseBoolean(req.get("requiresApproval")) : null;
            
            io.shinhanlife.axhub.common.util.ToolSourceUpdater.updateToolSource(toolName, domainGroup, description, register, requiresApproval);
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
            File[] files = dir.listFiles(f -> f.isDirectory() && f.getName().startsWith("axhub-tool-") && !f.getName().equals("axhub-tool-core"));
            
            if (files == null) return List.of("axhub-tool-other");
            
            return Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return List.of("axhub-tool-other");
        }
    }
}
