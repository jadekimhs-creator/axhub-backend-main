import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.*;

public class AddJavadoc {

    static final String ROOT_DIR = ".";
    
    static final String TEMPLATE = 
        "/**\n" +
        " * @package %s\n" +
        " * @className %s\n" +
        " * @description AX HUB ì‹œìŠ¤í…œ ì²˜ë¦¬ í´ë˜ìŠ¤\n" +
        " * @author ê¹€í˜•ì‹\n" +
        " * @create 2026.09.01\n" +
        " * <pre>\n" +
        " * ---------- ê°œì •ì´ë ¥ ----------\n" +
        " * ìˆ˜ì •ì¼      ìˆ˜ì •ì    ìˆ˜ì •ë‚´ìš©\n" +
        " * ---------- -------- ---------------------------\n" +
        " * 2026.09.01  ê¹€í˜•ì‹    ìµœì´ˆìƒì„±\n" +
        " * \n" +
        " * </pre>\n" +
        " */";

    public static void main(String[] args) throws Exception {
        Files.walkFileTree(Paths.get(ROOT_DIR), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                String name = dir.getFileName().toString();
                if (name.equals(".git") || name.equals("build") || name.equals(".gradle") || name.equals("scratch") || name.equals("out") || name.equals("bin")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java") && !file.getFileName().toString().equals("AddJavadoc.java")) {
                    processJavaFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("Done.");
    }

    private static void processJavaFile(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        String content = String.join("\n", lines);
        
        if (content.contains("---------- ê°œì •ì´ë ¥ ----------") || content.contains("@className")) {
            System.out.println("Skipping (already has javadoc): " + file);
            return;
        }

        String packageName = "unknown";
        String className = "unknown";

        Matcher pkgMatcher = Pattern.compile("(?m)^\\s*package\\s+([\\w\\.]+)\\s*;").matcher(content);
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1);
        }

        int classDeclIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher classMatcher = Pattern.compile("^\\s*(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|static\\s+)*(class|interface|enum|record)\\s+(\\w+)").matcher(line);
            if (classMatcher.find()) {
                classDeclIdx = i;
                className = classMatcher.group(2);
                break;
            }
        }

        if (classDeclIdx == -1) {
            System.out.println("Skipping (no class declaration found): " + file);
            return;
        }

        int insertIdx = classDeclIdx;
        for (int i = classDeclIdx - 1; i >= 0; i--) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("@")) {
                insertIdx = i;
            } else if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
                break;
            } else {
                break;
            }
        }

        String javadoc = String.format(TEMPLATE, packageName, className);
        lines.add(insertIdx, javadoc);

        Files.write(file, String.join("\n", lines).getBytes("UTF-8"));
        System.out.println("Updated: " + file);
    }
}

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (°³¹ß È¯°æ) ---
shinhan.integration.envrTypeCd=D
shinhan.integration.eai.url=http://10.176.32.181
shinhan.integration.internalMci.url=http://10.176.32.173
shinhan.integration.bancaMci.url=http://10.176.32.117
shinhan.integration.externalMci.url=http://10.176.32.176

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (Å×½ºÆ® È¯°æ) ---
shinhan.integration.envrTypeCd=T
shinhan.integration.eai.url=http://10.174.32.181
shinhan.integration.internalMci.url=http://10.174.32.173
shinhan.integration.bancaMci.url=http://10.174.32.117
shinhan.integration.externalMci.url=http://10.176.32.177

# --- ½ÅÇÑ¶óÀÌÇÁ EAI/MCI ¿¬°è IP Á¤º¸ (¿î¿µ È¯°æ) ---
shinhan.integration.envrTypeCd=R
shinhan.integration.eai.url=http://10.172.32.181
shinhan.integration.internalMci.url=http://10.172.32.173
shinhan.integration.bancaMci.url=http://10.172.32.117
shinhan.integration.externalMci.url=http://10.172.32.177
