package io.shinhanlife.glow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@Builder
@AllArgsConstructor
public class BaseResponse<T> {

//    @Schema(description = "응답 코드 HTTP STATUS 오류 - 실제 Header 는 200 으로 내려감", shinhanlife = "200")
    private int code;

//    @Schema(description = "응답 메시지. 응답 코드와 매핑된 메시지.", shinhanlife = "데이터 생성 성공")
    private String message;

//    @Schema(description = "응답 본문 데이터. 실제 비즈니스 데이터.")
    private T data;

    private BaseException error;

}
