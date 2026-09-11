package io.shinhanlife.glow.communication.module.eai.component;

import io.shinhanlife.glow.communication.dto.Transfer;

import org.springframework.stereotype.Component;

/**
 * TODO: 실제 Glow Framework 의존성(JAR)이 추가되면 이 Mock 클래스를 삭제하세요.
 */
@Component
public class GlowEaiComponent<I, O> {
    public Transfer<O> sync(Transfer<I> request) {
        // Mock 구현
        Transfer<O> response = new Transfer<>();
        response.setHeader(request.getHeader());
        response.setResBodyClass((Class<O>) request.getResBodyClass());
        return response;
    }
    
    public <S, R> Transfer<R> call(Transfer<S> request, Class<R> resBody) {
        Transfer<R> response = new Transfer<>();
        response.setHeader(request.getHeader());
        response.setResBodyClass(resBody);
        return response;
    }
}
