package io.shinhanlife.glow.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO: 실제 Glow Framework 의존성(JAR)이 추가되면 이 Mock 클래스를 삭제하세요.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer<T> {
    private Object header;
    private T body;
    private Class<T> resBodyClass;
    private Object message;
}
