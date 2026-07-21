package io.shinhanlife.dap.mcc.other.service;

import io.shinhanlife.dap.mcc.annotation.McpFunction;
import io.shinhanlife.dap.mcc.annotation.McpTool;
import io.shinhanlife.dap.mcc.dto.CustomerDetailReq;
import io.shinhanlife.dap.mcc.dto.CustomerGradeReq;
import io.shinhanlife.dap.mcc.service.AbstractMcpToolService;

@McpTool(
    routingType = "TCP",
    categoryKey = "customer"
)
/**
 * @package io.shinhanlife.dap.mcc.service
 * @className CustomerInfoService
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
public class CustomerInfoService extends AbstractMcpToolService {

    @McpFunction(register = false, displayName = "grade 툴", name = "grade", description = "고객등급 조회", prompt = "이 고객의 VIP 등급을 조회해줘.", mappingId = "CRM_001")
    public Object getGrade(CustomerGradeReq req) {
        return executeLegacy("TCP", "CRM_001", req);
    }

    @McpFunction(register = false, displayName = "detail 툴", name = "detail", description = "고객상세 정보 조회", prompt = "이 고객의 상세 기본정보(주소, 연락처 등)를 알려줘.", mappingId = "CRM_002")
    public Object getDetail(CustomerDetailReq data) {
        return executeLegacy("TCP", "CRM_002", data);
    }
}