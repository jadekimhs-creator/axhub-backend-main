package io.shinhanlife.dat.lib.paging;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @package io.shinhanlife.dat.lib.paging
 * @className MciPagingUtils
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
@Slf4j
public final class MciPagingUtils {

    private MciPagingUtils() {
    }

    /**
     * 페이징 응답 데이터(List 필드)를 대상(target) 객체에 순차적으로 병합(append)합니다.
     * 클래스 상속 계층 구조를 탐색하여 모든 List 타입 필드를 병합합니다.
     *
     * @param target 누적할 최종 응답 DTO
     * @param src    각 페이지별 응답 DTO
     */
    public static void mergeResponseData(Object target, Object src) {
        if (target == null || src == null) {
            return;
        }
        Class<?> current = target.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        List<?> srcList = (List<?>) field.get(src);
                        if (srcList != null && !srcList.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            List<Object> targetList = (List<Object>) field.get(target);
                            if (targetList == null) {
                                targetList = new ArrayList<>(srcList);
                                field.set(target, targetList);
                            } else {
                                if (!(targetList instanceof ArrayList)) {
                                    targetList = new ArrayList<>(targetList);
                                    field.set(target, targetList);
                                }
                                targetList.addAll(srcList);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[MCI Tool] 리스트 병합 중 예외 발생 (field: {}): {}", field.getName(), e.getMessage());
                    }
                }
            }
            current = current.getSuperclass();
        }
    }
}
