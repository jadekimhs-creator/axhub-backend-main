import re
import os

FILE_PATH = r"c:\Users\Admin\IdeaProjects\axhub-backend-main\dap-tool-core\src\main\java\io\shinhanlife\dap\common\util\ToolScaffolder.java"

with open(FILE_PATH, 'r', encoding='utf-8') as f:
    content = f.read()

part1_start = content.find('String toolName = baseName.isEmpty() ? baseName : Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);')
part1_end = content.find('log.append("\\n=========================================\\n");')

if part1_start == -1 or part1_end == -1:
    print("Could not find the target block.")
    exit(1)

new_block = '''String toolName = baseName.isEmpty() ? baseName : Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);

        boolean isMci = "MCI".equalsIgnoreCase(routingType);
        String serviceContent;

        if (isMci) {
            serviceContent = """
                package %s.service;

                import %s.annotation.McpFunction;
                import %s.annotation.McpTool;
                import %s.dto.%sReq;
                import %s.dto.%sRes;
                import io.shinhanlife.dap.common.integration.mci.component.AxhubMciComponent;
                import io.shinhanlife.glow.communication.dto.Transfer;
                import org.springframework.stereotype.Service;
                import lombok.RequiredArgsConstructor;
                import lombok.extern.slf4j.Slf4j;

                /**
                 * @package %s.service
                 * @className %sService
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일      수정자    수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Slf4j
                @Service
                @RequiredArgsConstructor
                @McpTool(
                    routingType = "%s",
                    categoryKey = "%s"
                )
                public class %sService {

                    private final AxhubMciComponent mci;

                    @McpFunction(
                        displayName = "%s 툴",
                        name = "%s",
                        description = "%s",
                        prompt = "%s",
                        mappingId = "%s",
                        register = %s,
                        requiresApproval = false,
                        openWorldHint = true
                    )
                    public Object execute(%sReq req) {
                        log.info("[MCI Tool] {} 요청 수신.", "%s");
                        try {
                            Transfer<Object> resTransfer = mci.callTo(
                                    "%s", 
                                    null, 
                                    req, 
                                    Object.class
                            );
                            return resTransfer.getBody() != null ? resTransfer.getBody() : "{\\"status\\":\\"SUCCESS\\"}";
                        } catch (Exception e) {
                            log.error("[MCI Tool] 연동 중 오류 발생: {}", e.getMessage(), e);
                            return "{\\"status\\":\\"ERROR\\", \\"message\\":\\"" + e.getMessage() + "\\"}";
                        }
                    }
                }
                """.formatted(
                    BASE_PACKAGE,
                    BASE_PACKAGE,
                    BASE_PACKAGE,
                    BASE_PACKAGE, baseName,
                    BASE_PACKAGE, baseName,
                    BASE_PACKAGE, baseName,
                    author,
                    createDate,
                    createDate, author,
                    routingType, group.toLowerCase(),
                    baseName,
                    baseName, toolName, description, description + " 해줘.", interfaceId, register,
                    baseName,
                    baseName, interfaceId
                );
        } else {
            serviceContent = """
                package %s.service;

                import %s.annotation.McpFunction;
                import %s.annotation.McpTool;
                import %s.dto.%sReq;
                import %s.dto.%sRes;
                import %s.%s.converter.%sLegacyConverter;
                import org.springframework.stereotype.Service;
                import org.mapstruct.factory.Mappers;

                /**
                 * @package %s.service
                 * @className %sService
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일      수정자    수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Service
                @McpTool(
                    routingType = "%s",
                    categoryKey = "%s"
                )
                public class %sService extends AbstractMcpToolService {

                    private final %sLegacyConverter converter = Mappers.getMapper(%sLegacyConverter.class);

                    @McpFunction(
                        displayName = "%s 툴",
                        name = "%s",
                        description = "%s",
                        prompt = "%s",
                        mappingId = "%s",
                        register = %s,
                        requiresApproval = false
                    )
                    public Object execute(%sReq req) {
                        // %sLegacyReq legacyReq = converter.toLegacyReq(req);
                        return executeLegacy("%s", "%s", req); // Or pass legacyReq
                    }
                }
                """.formatted(
                    BASE_PACKAGE,
                    BASE_PACKAGE,
                    BASE_PACKAGE,
                    BASE_PACKAGE, baseName,
                    BASE_PACKAGE, baseName,
                    BASE_PACKAGE, shortName, baseName,
                    BASE_PACKAGE, baseName,
                    author,
                    createDate,
                    createDate, author,
                    routingType, group.toLowerCase(),
                    baseName,
                    baseName, toolName, description, description + " 해줘.", interfaceId, register,
                    baseName,
                    baseName,
                    routingType, interfaceId
                );
        }
        
        Files.writeString(serviceDir.resolve(baseName + "Service.java"), serviceContent);

        if (!isMci) {
            String legacyReqContent = """
                package %s.%s.dto;

                import lombok.Data;

                /**
                 * @package %s.%s.dto
                 * @className %sLegacyReq
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일      수정자    수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Data
                public class %sLegacyReq {
                    // TODO: Add legacy request fields here
                }
                """.formatted(BASE_PACKAGE, shortName, BASE_PACKAGE, shortName, baseName, author, createDate, createDate, author, baseName);
            Files.writeString(legacyDtoDir.resolve(baseName + "LegacyReq.java"), legacyReqContent);

            String legacyResContent = """
                package %s.%s.dto;

                import lombok.Data;

                /**
                 * @package %s.%s.dto
                 * @className %sLegacyRes
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일      수정자    수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Data
                public class %sLegacyRes {
                    // TODO: Add legacy response fields here
                }
                """.formatted(BASE_PACKAGE, shortName, BASE_PACKAGE, shortName, baseName, author, createDate, createDate, author, baseName);
            Files.writeString(legacyDtoDir.resolve(baseName + "LegacyRes.java"), legacyResContent);

            String converterContent = """
                package %s.%s.converter;

                import %s.dto.%sReq;
                import %s.dto.%sRes;
                import %s.%s.dto.%sLegacyReq;
                import %s.%s.dto.%sLegacyRes;
                import org.mapstruct.Mapper;
                import org.mapstruct.Mapping;
                import org.mapstruct.factory.Mappers;

                /**
                 * @package %s.%s.converter
                 * @className %sLegacyConverter
                 * @description AX HUB 시스템 처리 클래스
                 * @author %s
                 * @create %s
                 * <pre>
                 * ---------- 개정이력 ----------
                 * 수정일      수정자    수정내용
                 * ---------- -------- ---------------------------
                 * %s  %s    최초생성
                 * 
                 * </pre>
                 */
                @Mapper(componentModel = "spring")
                public interface %sLegacyConverter {

                    %sLegacyReq toLegacyReq(%sReq req);
                    
                    %sRes toRes(%sLegacyRes legacyRes);
                }
                """.formatted(
                    BASE_PACKAGE, shortName,
                    BASE_PACKAGE, baseName,
                    BASE_PACKAGE, baseName,
                    BASE_PACKAGE, shortName, baseName,
                    BASE_PACKAGE, shortName, baseName,
                    BASE_PACKAGE, shortName, baseName, author, createDate, createDate, author,
                    baseName, baseName, baseName, baseName, baseName, baseName
                );
            Files.writeString(converterDir.resolve(baseName + "LegacyConverter.java"), converterContent);
        }

        '''

log_block_start = content.find('log.append("\\n=========================================\\n");')
log_block_end = content.find('return log.toString();')

new_log_block = '''log.append("\\n=========================================\\n");
        log.append(" Scaffolding Complete! (Routing: " + routingType + ")\\n");
        log.append("=========================================\\n");
        log.append("[Service] ").append(serviceDir.resolve(baseName + "Service.java")).append("\\n");
        log.append("[Req DTO] ").append(dtoDir.resolve(baseName + "Req.java")).append("\\n");
        log.append("[Res DTO] ").append(dtoDir.resolve(baseName + "Res.java")).append("\\n");
        
        if (!isMci) {
            log.append("[Legacy Req DTO] ").append(legacyDtoDir.resolve(baseName + "LegacyReq.java")).append("\\n");
            log.append("[Legacy Res DTO] ").append(legacyDtoDir.resolve(baseName + "LegacyRes.java")).append("\\n");
            log.append("[Legacy Converter] ").append(converterDir.resolve(baseName + "LegacyConverter.java")).append("\\n");
        }
        log.append("\\n Tip: ").append(interfaceId).append(" 목업 데이터를 mock-responses.json에 추가하세요.\\n");
        
        '''

content = content[:part1_start] + new_block + new_log_block + content[log_block_end:]

with open(FILE_PATH, 'w', encoding='utf-8') as f:
    f.write(content)

print("ToolScaffolder.java patched successfully.")
