package io.shinhanlife.axhub.common.mcp.exception;

import io.shinhanlife.axhub.common.mcp.adapter.dto.ErrorDetail;
import io.shinhanlife.axhub.common.mcp.adapter.dto.JsonRpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice //  이 어노테이션이 전역 적용의 핵심입니다!
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonRpcResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn(" [Gateway Bad Request] 잘못된 요청: {}", e.getMessage());
        return buildErrorResponse(-32602, "Invalid params: " + e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<JsonRpcResponse> handleRuntime(RuntimeException e) {
        log.error(" [Gateway Internal Error] 시스템 장애: {}", e.getMessage(), e);
        return buildErrorResponse(-32603, "Internal error: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonRpcResponse> handleAllException(Exception e) {
        log.error("💥 [Gateway Fatal Error] 치명적 오류 발생", e);
        return buildErrorResponse(-32000, "Server error: 시스템 관리자에게 문의하세요.");
    }

    private ResponseEntity<JsonRpcResponse> buildErrorResponse(int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setError(new ErrorDetail(code, message));

        return ResponseEntity.ok(response);
    }
}
