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
     * 페이징 응답 데이터를 대상(target) 객체에 병합합니다.
     * <p>
     * pageInfo와 scrPageInfo는 마지막 페이지 메타데이터로 교체하고,
     * 나머지 List 업무 데이터만 순차적으로 append합니다.
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
                field.setAccessible(true);
                try {
                    // List<PageInfo> / List<ScrPageInfo>도 업무 목록이 아니라
                    // 마지막 페이지의 continuation/총계 정보이므로 append하지 않는다.
                    if (isPagingMetadataField(field.getName())) {
                        Object srcValue = field.get(src);
                        if (srcValue != null) {
                            field.set(target, srcValue);
                        }
                        continue;
                    }

                    if (List.class.isAssignableFrom(field.getType())) {
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
                    }
                } catch (Exception e) {
                    log.warn("[MCI Tool] 응답 데이터 병합 중 예외 발생 (field: {}): {}", field.getName(), e.getMessage());
                }
            }
            current = current.getSuperclass();
        }
    }

    private static boolean isPagingMetadataField(String fieldName) {
        return "pageInfo".equals(fieldName) || "scrPageInfo".equals(fieldName);
    }
}
