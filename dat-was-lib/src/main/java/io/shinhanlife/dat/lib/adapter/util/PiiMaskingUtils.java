package io.shinhanlife.dat.lib.adapter.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @package io.shinhanlife.dat.lib.adapter.util
 * @className PiiMaskingUtils
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
public class PiiMaskingUtils {
    
    // 1. 주민등록번호 패턴 (ex: 900101-1234567 또는 9001011234567)
    private static final Pattern RRN_PATTERN = Pattern.compile("(\\d{6})[-]?([1-4]\\d{6})");
    
    // 2. 휴대전화번호 패턴 (ex: 010-1234-5678)
    private static final Pattern PHONE_PATTERN = Pattern.compile("(01[016789])[-]?(\\d{3,4})[-]?(\\d{4})");

    // 3. 신한라이프 계좌/증권번호 패턴 (단순 예시용 계좌번호 11~14자리)
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("(\\d{3})-?(\\d{3})-?(\\d{5,8})");

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String masked = input;
        
        // [1] 주민번호 뒷자리 마스킹 (첫자리 성별 식별자는 남기고 마스킹: 900101-1******)
        Matcher rrnMatcher = RRN_PATTERN.matcher(masked);
        StringBuffer rrnBuffer = new StringBuffer();
        while (rrnMatcher.find()) {
            String firstPart = rrnMatcher.group(1);
            String secondPart = rrnMatcher.group(2);
            rrnMatcher.appendReplacement(rrnBuffer, firstPart + "-" + secondPart.charAt(0) + "******");
        }
        rrnMatcher.appendTail(rrnBuffer);
        masked = rrnBuffer.toString();

        // [2] 전화번호 중간자리 마스킹 (010-****-5678)
        Matcher phoneMatcher = PHONE_PATTERN.matcher(masked);
        StringBuffer phoneBuffer = new StringBuffer();
        while (phoneMatcher.find()) {
            String p1 = phoneMatcher.group(1);
            String p2 = phoneMatcher.group(2);
            String p3 = phoneMatcher.group(3);
            String maskedP2 = p2.replaceAll(".", "*");
            phoneMatcher.appendReplacement(phoneBuffer, p1 + "-" + maskedP2 + "-" + p3);
        }
        phoneMatcher.appendTail(phoneBuffer);
        masked = phoneBuffer.toString();

        // [3] 계좌번호 뒷자리 마스킹 (110-123-********)
        Matcher accMatcher = ACCOUNT_PATTERN.matcher(masked);
        StringBuffer accBuffer = new StringBuffer();
        while (accMatcher.find()) {
            String a1 = accMatcher.group(1);
            String a2 = accMatcher.group(2);
            String a3 = accMatcher.group(3);
            String maskedA3 = a3.replaceAll(".", "*");
            accMatcher.appendReplacement(accBuffer, a1 + "-" + a2 + "-" + maskedA3);
        }
        accMatcher.appendTail(accBuffer);
        masked = accBuffer.toString();

        return masked;
    }
}