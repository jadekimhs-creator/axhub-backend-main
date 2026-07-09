import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RefactorPackage {

    static final String ROOT_DIR = ".";
    
    static final String[] DIR_RENAMES = {
        "./axhub-gateway/src/main/java/io/shinhanlife/axhub/biz/mcp/gateway/controller",
        "./axhub-gateway/src/main/java/io/shinhanlife/axhub/biz/mcp/gateway/presentation",
        "./axhub-tool-core/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/controller",
        "./axhub-tool-core/src/main/java/io/shinhanlife/axhub/biz/mcp/tool/presentation"
    };

    static final String[][] STRING_REPLACEMENTS = {
        {"io.shinhanlife.axhub.biz.mcp.gateway.controller", "io.shinhanlife.axhub.biz.mcp.gateway.presentation"},
        {"io.shinhanlife.axhub.biz.mcp.tool.controller", "io.shinhanlife.axhub.biz.mcp.tool.presentation"}
    };

    public static void main(String[] args) throws Exception {
        // 1. Rename Directories
        for (int i = 0; i < DIR_RENAMES.length; i += 2) {
            File srcDir = new File(DIR_RENAMES[i]);
            File destDir = new File(DIR_RENAMES[i+1]);
            if (srcDir.exists() && srcDir.isDirectory()) {
                boolean success = srcDir.renameTo(destDir);
                System.out.println("Renamed " + srcDir.getPath() + " to " + destDir.getPath() + " : " + success);
            } else {
                System.out.println("Source dir not found or not a directory: " + srcDir.getPath());
            }
        }

        // 2. Replace string contents in all .java files
        Files.walkFileTree(Paths.get(ROOT_DIR), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName().toString();
                if (name.equals(".git") || name.equals("build") || name.equals(".gradle") || name.equals("out") || name.equals("bin") || name.equals("scratch")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java") && !file.getFileName().toString().equals("RefactorPackage.java") && !file.getFileName().toString().equals("AddJavadoc.java")) {
                    processJavaFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Done.");
    }

    private static void processJavaFile(Path file) throws IOException {
        String content = new String(Files.readAllBytes(file), "UTF-8");
        boolean modified = false;
        
        for (String[] rep : STRING_REPLACEMENTS) {
            if (content.contains(rep[0])) {
                content = content.replace(rep[0], rep[1]);
                modified = true;
            }
        }

        if (modified) {
            Files.write(file, content.getBytes("UTF-8"));
            System.out.println("Updated imports/package in: " + file);
        }
    }
}
