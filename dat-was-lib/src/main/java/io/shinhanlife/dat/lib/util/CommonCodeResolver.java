package io.shinhanlife.dat.lib.util;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @package io.shinhanlife.dat.lib.util
 * @className CommonCodeResolver
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
@Slf4j
@Component
public class CommonCodeResolver {

    private static final String DEFAULT_RESOURCE_PATH = "codes.csv";

    // Group -> (Code -> Name)
    private static final Map<String, Map<String, String>> CODE_TO_NAME_MAP = new ConcurrentHashMap<>();
    // Group -> (Normalized Name or Code -> Code)
    private static final Map<String, Map<String, String>> NAME_TO_CODE_MAP = new ConcurrentHashMap<>();

    static {
        loadCodes(DEFAULT_RESOURCE_PATH);
    }

    @PostConstruct
    public void init() {
        loadCodes(DEFAULT_RESOURCE_PATH);
    }

    public static synchronized void reload() {
        loadCodes(DEFAULT_RESOURCE_PATH);
    }

    public static synchronized void loadCodes(String resourcePath) {
        ClassLoader classLoader = CommonCodeResolver.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("[CommonCodeResolver] 리소스 파일을 찾을 수 없습니다: {}", resourcePath);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                CODE_TO_NAME_MAP.clear();
                NAME_TO_CODE_MAP.clear();

                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        if (line.startsWith("\uFEFF")) {
                            line = line.substring(1);
                        }
                        if (line.contains("그룹") || line.contains("group")) {
                            continue;
                        }
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(",", -1);
                    if (parts.length < 3) {
                        continue;
                    }
                    String group = parts[0].trim().toUpperCase(Locale.ROOT);
                    String code = parts[1].trim();
                    String name = parts[2].trim();

                    if (group.isEmpty() || code.isEmpty()) {
                        continue;
                    }

                    Map<String, String> codeToName = CODE_TO_NAME_MAP.computeIfAbsent(group, k -> new ConcurrentHashMap<>());
                    Map<String, String> nameToCode = NAME_TO_CODE_MAP.computeIfAbsent(group, k -> new ConcurrentHashMap<>());

                    codeToName.put(code, name);
                    nameToCode.put(code, code);
                    nameToCode.put(name, code);
                    nameToCode.put(name.toLowerCase(Locale.ROOT), code);
                    nameToCode.put(name.replace(" ", ""), code);
                }
                log.info("[CommonCodeResolver] 공통코드 로드 완료: {}개 그룹", CODE_TO_NAME_MAP.size());
            }
        } catch (Exception e) {
            log.error("[CommonCodeResolver] 공통코드 로드 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 입력값(명칭 또는 코드)을 처리계 표준 코드값으로 변환합니다.
     *
     * @param group 코드 그룹명 (예: COMPANY, STATUS)
     * @param input 사용자가 입력한 명칭("삼성전자") 또는 코드("005930")
     * @return 매핑된 코드값(예: "005930"). 매핑되지 않으면 원본 입력값 반환.
     */
    public static String resolve(String group, String input) {
        return resolve(group, input, input);
    }

    /**
     * 입력값(명칭 또는 코드)을 처리계 표준 코드값으로 변환합니다.
     *
     * @param group 코드 그룹명
     * @param input 입력값
     * @param defaultValue 매핑 실패 시 반환할 기본값
     * @return 매핑된 코드값 또는 defaultValue
     */
    public static String resolve(String group, String input, String defaultValue) {
        if (input == null || input.isBlank()) {
            return defaultValue;
        }
        if (group == null) {
            return input.trim();
        }
        String normalizedGroup = group.trim().toUpperCase(Locale.ROOT);
        Map<String, String> nameToCode = NAME_TO_CODE_MAP.get(normalizedGroup);
        if (nameToCode == null) {
            return defaultValue;
        }
        String trimmed = input.trim();
        String matched = nameToCode.get(trimmed);
        if (matched != null) {
            return matched;
        }
        matched = nameToCode.get(trimmed.toLowerCase(Locale.ROOT));
        if (matched != null) {
            return matched;
        }
        matched = nameToCode.get(trimmed.replace(" ", ""));
        if (matched != null) {
            return matched;
        }
        return defaultValue;
    }

    /**
     * 코드값에 해당하는 표준 명칭을 반환합니다. (응답 변환 시 사용)
     */
    public static String getName(String group, String code) {
        if (group == null || code == null) {
            return code;
        }
        String normalizedGroup = group.trim().toUpperCase(Locale.ROOT);
        Map<String, String> codeToName = CODE_TO_NAME_MAP.get(normalizedGroup);
        if (codeToName != null) {
            return codeToName.getOrDefault(code.trim(), code);
        }
        return code;
    }

    /**
     * 특정 그룹의 전체 코드-명칭 맵을 반환합니다.
     */
    public static Map<String, String> getCodeMap(String group) {
        if (group == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = CODE_TO_NAME_MAP.get(group.trim().toUpperCase(Locale.ROOT));
        return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }
}