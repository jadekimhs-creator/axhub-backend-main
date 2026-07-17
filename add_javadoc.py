import os
import re

JAVADOC_FILES = [
    r'.\McpBridge.java',
    r'.\dap-common\src\main\java\io\shinhanlife\dap\common\mcp\exception\GlobalExceptionHandler.java',
    r'.\dap-common\src\main\java\io\shinhanlife\dap\common\util\ToolSourceUpdater.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowAppServiceId.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowControllerId.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowIndexPaging.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowMciFieldInfo.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowMybatisMapper.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowServiceGroupId.java',
    r'.\dap-common\src\main\java\io\shinhanlife\glow\GlowTrgmField.java',
    r'.\dap-common\src\test\java\io\shinhanlife\glow\util\GlowMciParserTest.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\DapGatewayApplication.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\audit\AuditLogService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\config\AgentResponseBudgetProperties.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\config\McpGatewayProperties.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\guardrail\GuardrailService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\guardrail\SensitiveDataMasker.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\guardrail\ToolResponseGuardrailService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\presentation\ChatController.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\presentation\McpRouterController.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\presentation\ScaffoldingController.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\redis\McpMonitorEventService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\redis\RedisToolTraceService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\CircuitBreaker.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\CircuitBreakerMonitorController.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\CircuitBreakerService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\CircuitBreakerSnapshot.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\CircuitBreakerState.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\FailureType.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\RetryPolicy.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\resilience\ToolExecutionException.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\security\McpRequestContext.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\security\McpRequestContextResolver.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\security\ToolAuthorizationService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\service\ExecuteService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\sync\DynamicMcpController.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\sync\DynamicMcpServerManager.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\sync\RegistryMcpToolSpecificationFactory.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\tool\large\AgentResponseBudgetService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\tool\large\LargeToolResponseService.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\tool\large\PaginationRequestValidator.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\tool\result\ToolExecutionResult.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\tool\result\ToolExecutionResultFormatter.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\transport\HttpToolInvoker.java',
    r'.\dap-gateway\src\main\java\io\shinhanlife\dap\biz\mcp\gateway\transport\ToolInvoker.java',
    r'.\dap-gateway\src\test\java\io\shinhanlife\dap\biz\mcp\gateway\DapGatewayApplicationTests.java',
    r'.\dap-gateway\src\test\java\io\shinhanlife\dap\biz\mcp\gateway\tool\large\PaginationRequestValidatorTest.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\sample\converter\SampleLegacyConverter.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\sample\dto\SampleAiReqDto.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\annotation\McpFunction.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\annotation\McpParameter.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\annotation\McpTool.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\aop\ToolSlaMonitoringAspect.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\presentation\BusinessToolController.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\service\ToolRegistryHeartbeatSender.java',
    r'.\dap-tool-core\src\main\java\io\shinhanlife\dap\biz\mcp\tool\util\JsonSchemaGenerator.java',
    r'.\dap-tool-core\src\test\java\io\shinhanlife\dap\biz\mcp\sample\converter\SampleLegacyConverterTest.java',
    r'.\dap-tool-email\src\main\java\io\shinhanlife\dap\biz\mcp\tool\email\DapToolEmailApplication.java',
    r'.\dap-tool-other\src\main\java\io\shinhanlife\dap\biz\mcp\tool\dto\BalanceReq.java',
    r'.\dap-tool-other\src\main\java\io\shinhanlife\dap\biz\mcp\tool\dto\BalanceRes.java',
    r'.\dap-tool-other\src\main\java\io\shinhanlife\dap\biz\mcp\tool\other\DapToolOtherApplication.java',
    r'.\dap-tool-other\src\main\java\io\shinhanlife\dap\biz\mcp\tool\service\BalanceService.java',
    r'.\dap-tool-payment\src\main\java\io\shinhanlife\dap\biz\mcp\tool\payment\DapToolPaymentApplication.java',
    r'.\dap-tool-sms\src\main\java\io\shinhanlife\dap\biz\mcp\tool\sms\DapToolSmsApplication.java',
    r'.\dap-tool-sms\src\main\java\io\shinhanlife\dap\biz\mcp\tool\sms\converter\SmsLegacyConverter.java',
    r'.\dap-tool-sms\src\main\java\io\shinhanlife\dap\biz\mcp\tool\sms\dto\SmsLegacyReqDto.java',
]

JAVADOC_TEMPLATE = '''/**
 * @package {package}
 * @className {classname}
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
 */'''

def get_package_and_class(content, filename):
    pkg_match = re.search(r'^package\s+([^\s;]+)', content, re.MULTILINE)
    package = pkg_match.group(1) if pkg_match else 'io.shinhanlife'
    classname = os.path.splitext(filename)[0]
    return package, classname

def add_javadoc(file_path):
    if not os.path.exists(file_path):
        print(f'SKIP (not found): {file_path}')
        return
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    if '@className' in content or '@author' in content:
        print(f'SKIP (already has javadoc): {file_path}')
        return

    filename = os.path.basename(file_path)
    package, classname = get_package_and_class(content, filename)
    javadoc = JAVADOC_TEMPLATE.format(package=package, classname=classname)

    # Insert after package declaration
    pkg_pattern = re.compile(r'^(package\s+[^\s;]+;\s*\n)', re.MULTILINE)
    match = pkg_pattern.search(content)
    if match:
        insert_pos = match.end()
        new_content = content[:insert_pos] + '\n' + javadoc + '\n' + content[insert_pos:]
    else:
        new_content = javadoc + '\n' + content

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print(f'OK: {file_path}')

for f in JAVADOC_FILES:
    add_javadoc(f)

print('\nDone!')
