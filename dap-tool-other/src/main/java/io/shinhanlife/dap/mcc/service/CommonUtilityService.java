package io.shinhanlife.dap.mcc.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.VacationRegisterReq;
import io.shinhanlife.dap.mcc.dto.LeaveCountReq;
import io.shinhanlife.dap.mcc.dto.LeaveCountReq;

@McpTool(
    routingType = "HTTP",
    categoryKey = "hr"
)
/**
 * @package io.shinhanlife.dap.mcc.service
 * @className CommonUtilityService
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
public class CommonUtilityService extends AbstractMcpToolService {

    @McpFunction(register = false, displayName = "register_vacation 툴", name = "register_vacation", description = "휴가 등록", prompt = "내일 하루 연차 휴가를 등록해줘.", mappingId = "HR_VAC_01")
    public Object registerVacation(VacationRegisterReq data) {
        return executeLegacy("HTTP", "HR_VAC_01", data);
    }

    @McpFunction(register = false, displayName = "get_leave_count 툴", name = "get_leave_count", description = "연차 갯수 조회", prompt = "현재 사용 가능한 남은 연차 일수를 알려줘.", mappingId = "HR_VAC_02")
    public Object getLeaveCount(LeaveCountReq data) {
        return executeLegacy("HTTP", "HR_VAC_02", data);
    }

    @McpFunction(register = false, displayName = "secret_tool 툴", name = "secret_tool", description = "비공개 툴 테스트", prompt = "숨겨진 툴 강제 호출", mappingId = "SECRET_001", visible = false)
    public Object secretTool(LeaveCountReq data) {
        return executeLegacy("HTTP", "SECRET_001", data);
    }

}