package io.shinhanlife.glow;

/**
 * 업무 예외.
 *
 * <p>두 가지 방식으로 쓸 수 있다.</p>
 * <ol>
 *   <li><b>메시지코드 방식(권장)</b> — {@code throw new BizException("DAH00004", "사번")}<br>
 *       통합메시지(ZT_UNFC_MSG)에서 문구를 찾아 {0},{1}.. 을 인자로 치환해 응답한다.
 *       문구가 화면·서버 한곳(관리 화면)에서 관리되고, 다국어 확장도 여기서 처리된다.</li>
 *   <li><b>문구 직접 방식(기존 호환)</b> — {@code throw new BizException("사번은 필수입니다.")}<br>
 *       메시지코드로 해석되지 않으면 문구 그대로 응답한다.</li>
 * </ol>
 *
 * <p>변환은 {@code common/config/GlobalExceptionHandler} 가 수행한다.
 * 메시지코드 여부는 코드 형식(영문 대문자+숫자 8자리)으로 판별한다.</p>
 */
public class BizException extends RuntimeException {

    /** 통합메시지코드 (문구 직접 방식이면 null) */
    private final String msgCd;

    /** 메시지 치환 인자 */
    private final Object[] msgArgs;

    /**
     * 문구를 직접 지정하거나, 메시지코드만 던진다.
     *
     * @param messageOrCode 메시지 문구 또는 통합메시지코드
     */
    public BizException(String messageOrCode) {
        super(messageOrCode);
        this.msgCd = isMessageCode(messageOrCode) ? messageOrCode : null;
        this.msgArgs = new Object[0];
    }

    /**
     * 메시지코드 + 치환 인자.
     *
     * @param msgCd   통합메시지코드 (예: DAH00004)
     * @param msgArgs {0},{1}.. 에 순서대로 치환될 인자
     */
    public BizException(String msgCd, Object... msgArgs) {
        super(msgCd);
        this.msgCd = msgCd;
        this.msgArgs = msgArgs == null ? new Object[0] : msgArgs;
    }

    public String getMsgCd() {
        return msgCd;
    }

    public Object[] getMsgArgs() {
        return msgArgs;
    }

    /** 통합메시지코드 형식인지 — 영문 대문자 3자리 + 숫자 5자리 (예: DAH00001) */
    private static boolean isMessageCode(String value) {
        return value != null && value.matches("^[A-Z]{3}\\d{5}$");
    }
}
