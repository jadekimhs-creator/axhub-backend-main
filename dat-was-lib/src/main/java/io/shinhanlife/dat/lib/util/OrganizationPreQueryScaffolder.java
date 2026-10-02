package io.shinhanlife.dat.lib.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Generates a Pod-local MCI adapter for organization-number pre-queries. */
public final class OrganizationPreQueryScaffolder {

    private static final String BASE_PACKAGE = "io.shinhanlife.dat.mcc";
    private static final Path BASE_PACKAGE_PATH = Path.of("io", "shinhanlife", "dat", "mcc");

    private OrganizationPreQueryScaffolder() {
    }

    public record Definition(
            String moduleName,
            String adapterName,
            String interfaceId,
            String mciIoPrefix,
            String clientSystemCode,
            String requestFieldName,
            String responseOrganizationNoFieldName,
            String responseOrganizationNameFieldName) {
    }

    public static String scaffold(Definition definition) throws IOException {
        validate(definition);

        String sourceDirectory = System.getProperty("AXHUB_SOURCE_DIR");
        if (sourceDirectory == null || sourceDirectory.isBlank()) {
            throw new IllegalStateException("AXHUB_SOURCE_DIR is required");
        }

        String adapterName = pascalCase(definition.adapterName());
        String normalizedSystemCode = definition.clientSystemCode().trim().toUpperCase(Locale.ROOT);
        String mciPackageSegment = mciPackageSegment(normalizedSystemCode);
        String clientClassName = "Mci" + pascalCase(mciClientPrefix(normalizedSystemCode).toLowerCase(Locale.ROOT)) + "Client";
        String ioPrefix = definition.mciIoPrefix().trim().toUpperCase(Locale.ROOT);
        String adapterPackage = BASE_PACKAGE + ".common";
        String mciPackage = BASE_PACKAGE + ".infra.itrf.mci." + mciPackageSegment;

        Path rootDir = Path.of(sourceDirectory);
        Path moduleRoot = ToolScaffolder.resolveModuleRoot(rootDir, definition.moduleName().trim());
        Path sourceRoot = moduleRoot.resolve("src/main/java").resolve(BASE_PACKAGE_PATH);
        Path adapterDirectory = sourceRoot.resolve("common");
        Path implDirectory = adapterDirectory.resolve("impl");
        requireExistingMciContract(sourceRoot, mciPackageSegment, clientClassName, ioPrefix);
        Files.createDirectories(implDirectory);

        writeNew(adapterDirectory.resolve(adapterName + "Adapter.java"), """
                package %s;

                import %s.io.%s_I;
                import %s.io.%s_O;

                /**
                 * @package %s
                 * @className %sAdapter
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
                public interface %sAdapter {

                    %s_O find(%s_I request);
                }
                """.formatted(adapterPackage,
                mciPackage, ioPrefix,
                mciPackage, ioPrefix,
                adapterPackage, adapterName,
                adapterName, ioPrefix, ioPrefix));

        String implPackage = adapterPackage + ".impl";

        writeNew(implDirectory.resolve(adapterName + "AdapterImpl.java"), """
                package %s;

                import %s.%sAdapter;
                import %s.%s;
                import %s.io.%s_I;
                import %s.io.%s_O;
                import io.shinhanlife.glow.communication.dto.Transfer;
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Component;

                /**
                 * @package %s
                 * @className %sAdapterImpl
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
                @Component
                @RequiredArgsConstructor
                public class %sAdapterImpl implements %sAdapter {

                    private static final String INTERFACE_ID = "%s";
                    private static final String RECEIVE_SERVICE_ID = "%s";

                    private final %s mci;

                    @Override
                    public %s_O find(%s_I request) {
                        if (request == null) {
                            throw new IllegalArgumentException("request is required for organization lookup");
                        }

                        try {
                            Transfer<%s_O> transfer = mci.callTo(INTERFACE_ID, RECEIVE_SERVICE_ID, request, %s_O.class);
                            return transfer == null ? null : transfer.getBody();
                        } catch (RuntimeException exception) {
                            throw exception;
                        } catch (Exception exception) {
                            throw new IllegalStateException("Organization lookup MCI call failed", exception);
                        }
                    }
                }
                """.formatted(
                implPackage,
                adapterPackage, adapterName,
                mciPackage, clientClassName,
                mciPackage, ioPrefix,
                mciPackage, ioPrefix,
                implPackage, adapterName,
                adapterName, adapterName,
                definition.interfaceId().trim().toUpperCase(Locale.ROOT), normalizedSystemCode,
                clientClassName,
                ioPrefix, ioPrefix,
                ioPrefix, ioPrefix));

        return "Generated organization pre-query adapter: " + adapterDirectory;
    }

    private static void validate(Definition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        if (definition.moduleName() == null || !definition.moduleName().matches("[A-Za-z0-9][A-Za-z0-9-]*")) {
            throw new IllegalArgumentException("moduleName must contain letters, numbers, or hyphens");
        }
        requiredIdentifier(definition.adapterName(), "adapterName");
        requiredIdentifier(definition.interfaceId(), "interfaceId");
        requiredIdentifier(definition.mciIoPrefix(), "mciIoPrefix");
        requiredIdentifier(definition.requestFieldName(), "requestFieldName");
        requiredIdentifier(definition.responseOrganizationNoFieldName(), "responseOrganizationNoFieldName");
        requiredIdentifier(definition.responseOrganizationNameFieldName(), "responseOrganizationNameFieldName");
        String systemCode = definition.clientSystemCode() == null ? "" : definition.clientSystemCode().trim();
        if (systemCode.length() != 9) {
            throw new IllegalArgumentException("clientSystemCode must contain exactly 9 characters");
        }
    }

    private static void requiredIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(label + " must be a Java identifier");
        }
    }

    private static String mciPackageSegment(String clientSystemCode) {
        if (clientSystemCode.length() == 9) {
            return clientSystemCode.substring(1, 4).toLowerCase(Locale.ROOT)
                    + "." + clientSystemCode.substring(4, 5).toLowerCase(Locale.ROOT);
        }
        return clientSystemCode.substring(0, 3).toLowerCase(Locale.ROOT)
                + "." + clientSystemCode.substring(3).toLowerCase(Locale.ROOT);
    }

    private static String mciClientPrefix(String clientSystemCode) {
        return clientSystemCode.length() == 9 ? clientSystemCode.substring(1, 5) : clientSystemCode;
    }

    private static String pascalCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static void writeNew(Path file, String content) throws IOException {
        if (Files.exists(file)) {
            throw new IllegalStateException("Refusing to overwrite existing adapter file: " + file);
        }
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void requireExistingMciContract(Path sourceRoot, String mciPackageSegment,
                                                   String clientClassName, String ioPrefix) {
        Path mciDirectory = sourceRoot.resolve("infra/itrf/mci")
                .resolve(mciPackageSegment.replace('.', '/'));
        Path client = mciDirectory.resolve(clientClassName + ".java");
        Path request = mciDirectory.resolve("io").resolve(ioPrefix + "_I.java");
        Path response = mciDirectory.resolve("io").resolve(ioPrefix + "_O.java");
        if (!Files.isRegularFile(client) || !Files.isRegularFile(request) || !Files.isRegularFile(response)) {
            throw new IllegalStateException("MCI contract is missing. Generate/select "
                    + ioPrefix + "_I, " + ioPrefix + "_O, and " + clientClassName + " first.");
        }
    }
}
