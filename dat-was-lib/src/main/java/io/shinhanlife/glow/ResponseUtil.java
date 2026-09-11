package io.shinhanlife.glow;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseUtil {

    private ResponseUtil() {

    }

    public static <T> ResponseEntity<BaseResponse<T>> ok() {
        return ResponseEntity.ok(
                BaseResponse.<T>builder()
                        .code(ResponseCode.SUCCESS.getStatus().value())
                        .message(ResponseCode.SUCCESS.getMessage())
                        .build()
        );
    }

    public static <T> ResponseEntity<BaseResponse<T>> ok(T data) {
        return ResponseEntity.ok(
                BaseResponse.<T>builder()
                        .code(ResponseCode.SUCCESS.getStatus().value())
                        .message(ResponseCode.SUCCESS.getMessage())
                        .data(data)
                        .build()
        );
    }

    public static <T> ResponseEntity<BaseResponse<T>> ok(T data, ResponseCode responseCode) {
        return ResponseEntity.ok(
                BaseResponse.<T>builder()
                        .code(responseCode.getStatus().value())
                        .message(responseCode.getMessage())
                        .data(data)
                        .build()
        );
    }

    public static <T> ResponseEntity<BaseResponse<T>> error(
            HttpStatus status, String code, String message
    ) {
        return ResponseEntity.status(status)
                .body(
                        BaseResponse.<T>builder()
                                .code(status.value())
                                .error(
                                        BaseException.builder()
                                                .code(code)
                                                .message(message)
                                                .build()
                                )
                                .build()
                );
    }

    public static <T> ResponseEntity<BaseResponse<T>> error(
            HttpStatus status, String code, String message, String exceptionName, String stackTrace
    ) {
        return ResponseEntity.status(status)
                .body(
                        BaseResponse.<T>builder()
                                .code(status.value())
                                .error(
                                        BaseException.builder()
                                                .code(code)
                                                .message(message)
                                                .exceptionName(exceptionName)
                                                .exceptionDetail(stackTrace)
                                                .build()
                                )
                                .build()
                );
    }

    public static <T> ResponseEntity<BaseResponse<T>> okError(
            HttpStatus status, String code, String message
    ) {
        return ResponseEntity.ok(
                BaseResponse.<T>builder()
                        .code(status.value())
                        .error(
                                BaseException.builder()
                                        .code(code)
                                        .message(message)
                                        .build()
                        )
                        .build()
        );
    }

    public static <T> ResponseEntity<BaseResponse<T>> okError(
            HttpStatus status, String code, String message, String exceptionName, String stackTrace
    ) {
        return ResponseEntity.ok(
                BaseResponse.<T>builder()
                        .code(status.value())
                        .error(
                                BaseException.builder()
                                        .code(code)
                                        .message(message)
                                        .exceptionName(exceptionName)
                                        .exceptionDetail(stackTrace)
                                        .build()
                        )
                        .build()
        );
    }

}
