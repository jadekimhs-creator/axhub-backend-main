package io.shinhanlife.dap.mcc.annotation;


/**
 * @package io.shinhanlife.dap.mcc.annotation
 * @className McpFunction
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
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpFunction {
    String displayName();             // 사람이 읽는 라벨 (예: "고객 조회 툴")
    String name();      // MCP 서브툴 명칭 (예: "customer_search")
    String description();
    String prompt() default "";
    String mappingId() default "";
    
    // 추가: 해당 함수가 요구하는 비즈니스 파라미터(JSON 형태의 properties)를 정의
    String inputSchema() default "{}";
    
    // 추가: Redis 자동 등록 및 Heartbeat 대상 여부 제어
    boolean register() default false;
    
    // 추가: 툴 목록 노출 여부 제어 (false 시 라우팅은 되나 목록에서 숨김)
    boolean visible() default true;

    // 추가: HITL 승인 체계 지원 (실행 전 사용자 승인 필요 여부)
    boolean requiresApproval() default false;

    boolean readOnlyHint() default false;
    boolean destructiveHint() default false;
    boolean idempotentHint() default false;
    boolean openWorldHint() default false;

    // 추가: 툴 별 기본 Timeout 설정 (기본 300초 = 300000ms)
}
