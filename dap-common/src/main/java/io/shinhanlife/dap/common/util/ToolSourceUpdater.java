package io.shinhanlife.dap.common.util;


/**
 * @package io.shinhanlife.dap.common.util
 * @className ToolSourceUpdater
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolSourceUpdater {

    public static void updateToolSource(String toolName, String domainGroup, String description, boolean register, Boolean requiresApproval) throws Exception {
        // 1. Find all *Service.java files in axhub-tool-* directories
        String envSourceDir = System.getenv("AXHUB_SOURCE_DIR");
        Path rootDir = envSourceDir != null ? Paths.get(envSourceDir) : Paths.get(".");
        
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(rootDir)) {
            javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith("Service.java"))
                .filter(p -> p.toString().contains("axhub-tool-"))
                .collect(Collectors.toList());
        }

        Path targetFile = null;
        String content = null;

        // 2. Find the specific file for the tool
        String functionName = toolName;
        if (toolName.contains("_")) {
            functionName = toolName.substring(toolName.indexOf("_") + 1);
        }

        Pattern namePattern = Pattern.compile("@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(toolName) + "\"", Pattern.DOTALL);
        Pattern namePattern2 = Pattern.compile("@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(functionName) + "\"", Pattern.DOTALL);

        for (Path path : javaFiles) {
            String text = Files.readString(path);
            if (namePattern.matcher(text).find()) {
                targetFile = path;
                content = text;
                break;
            } else if (namePattern2.matcher(text).find()) {
                targetFile = path;
                content = text;
                toolName = functionName; // Use baseName for subsequent replacements
                break;
            }
        }

        if (targetFile == null) {
            throw new Exception("소스 코드를 찾을 수 없습니다: " + toolName);
        }

        // 3. Update @McpTool group
        if (domainGroup != null && !domainGroup.trim().isEmpty()) {
            Pattern groupPattern = Pattern.compile("(@McpTool\\s*\\([^)]*group\\s*=\\s*\")([^\"]+)(\")", Pattern.DOTALL);
            Matcher groupMatcher = groupPattern.matcher(content);
            if (groupMatcher.find()) {
                content = groupMatcher.replaceFirst("$1" + domainGroup + "$3");
            }
        }

        // 4. Update @McpFunction description
        if (description != null) {
            Pattern funcPattern = Pattern.compile("(@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(toolName) + "\"[^)]*description\\s*=\\s*\")([^\"]+)(\")", Pattern.DOTALL);
            Matcher funcMatcher = funcPattern.matcher(content);
            if (funcMatcher.find()) {
                content = funcMatcher.replaceFirst("$1" + description.replace("\\", "\\\\").replace("$", "\\\\$") + "$3");
            }
        }

        // 5. Update register flag
        Pattern regPattern = Pattern.compile("(@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(toolName) + "\"[^)]*register\\s*=\\s*)(true|false)([^a-zA-Z0-9])", Pattern.DOTALL);
        Matcher regMatcher = regPattern.matcher(content);
        if (regMatcher.find()) {
            content = regMatcher.replaceFirst("$1" + register + "$3");
        } else {
            Pattern addRegPattern = Pattern.compile("(@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(toolName) + "\")", Pattern.DOTALL);
            Matcher addRegMatcher = addRegPattern.matcher(content);
            if (addRegMatcher.find()) {
                content = addRegMatcher.replaceFirst("$1, register = " + register);
            }
        }

        // 5.5 Update requiresApproval flag
        if (requiresApproval != null) {
            Pattern appPattern = Pattern.compile("(@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(toolName) + "\"[^)]*requiresApproval\\s*=\\s*)(true|false)([^a-zA-Z0-9])", Pattern.DOTALL);
            Matcher appMatcher = appPattern.matcher(content);
            if (appMatcher.find()) {
                content = appMatcher.replaceFirst("$1" + requiresApproval + "$3");
            } else {
                Pattern addAppPattern = Pattern.compile("(@McpFunction\\s*\\([^)]*name\\s*=\\s*\"" + Pattern.quote(toolName) + "\")", Pattern.DOTALL);
                Matcher addAppMatcher = addAppPattern.matcher(content);
                if (addAppMatcher.find()) {
                    content = addAppMatcher.replaceFirst("$1, requiresApproval = " + requiresApproval);
                }
            }
        }

        // 6. Write back to file
        Files.writeString(targetFile, content);
    }
}
