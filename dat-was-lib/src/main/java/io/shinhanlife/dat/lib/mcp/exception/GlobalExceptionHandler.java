package io.shinhanlife.dat.lib.mcp.exception;


/**
 * @package io.shinhanlife.dat.lib.mcp.exception
 * @className GlobalExceptionHandler
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
import io.shinhanlife.dat.lib.adapter.dto.ErrorDetail;
import io.shinhanlife.dat.lib.adapter.dto.JsonRpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice //  이 어노테이션이 전역 적용의 핵심입니다!
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn(" [Gateway Not Found] 요청하신 리소스를 찾을 수 없습니다: {}", e.getResourcePath());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonRpcResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn(" [Gateway Bad Request] 잘못된 요청: {}", e.getMessage());
        return buildErrorResponse(-32602, "Invalid params: " + e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.context.request.async.AsyncRequestNotUsableException.class)
    public ResponseEntity<Void> handleAsyncRequestNotUsable(org.springframework.web.context.request.async.AsyncRequestNotUsableException e) {
        log.debug(" [Gateway Info] 클라이언트가 비동기 연결을 종료했습니다 (정상): {}", e.getMessage());
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<JsonRpcResponse> handleRuntime(RuntimeException e) {
        log.error(" [Gateway Internal Error] 시스템 장애: {}", e.getMessage(), e);
        return buildErrorResponse(-32603, "Internal error: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonRpcResponse> handleAllException(Exception e) {
        log.error(" [Gateway Fatal Error] 치명적 오류 발생", e);
        return buildErrorResponse(-32000, "Server error: 시스템 관리자에게 문의하세요.");
    }

    private ResponseEntity<JsonRpcResponse> buildErrorResponse(int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setError(new ErrorDetail(code, message));

        return ResponseEntity.ok(response);
    }
}