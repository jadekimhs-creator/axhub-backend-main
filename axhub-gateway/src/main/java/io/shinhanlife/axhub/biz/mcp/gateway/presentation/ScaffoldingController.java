package io.shinhanlife.axhub.biz.mcp.gateway.presentation;

import io.shinhanlife.axhub.common.util.PodScaffolder;
import io.shinhanlife.axhub.common.util.ToolScaffolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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
            String author = req.getOrDefault("author", "System");
            String date = req.getOrDefault("date", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            
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
            String author = req.getOrDefault("author", "System");
            String date = req.getOrDefault("date", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            
            return ToolScaffolder.scaffold(baseName, interfaceId, description, group, routingType, moduleName, author, date);
        } catch (Exception e) {
            return "오류 발생: " + e.getMessage();
        }
    }
}
