package io.shinhanlife.glow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@Schema(description = "응답 에러 객체. 성공 케이스일 경우 null. 실제 에러가 발생할 경우에만 예외명, 예외 메시지 필드 세팅 예정.")
public class BaseException {

//    @Schema(description = "Error 코드 Meta 참조 운영. (예) 20001, 50001 등", shinhanlife = "20001")
    String code;

//    @Schema(description = "메시지")
    String message;

//    @Schema(description = "예외명.", shinhanlife = "NullPointerException")
    @Builder.Default
    String exceptionName = "";

//    @Schema(description = "예외상세", shinhanlife = "StackTrace")
    @Builder.Default
    String exceptionDetail = "";

}
