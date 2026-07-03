package io.shinhanlife.axhub.biz.sm.mmg.domain.model;

import io.shinhanlife.glow.db.dto.AuditInfo;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ZtMenu은 ZT_MENU 테이블을 매핑하는 Entity 클래스입니다.
 * 이 클래스는 테이블의 컬럼을 Java 필드로 매핑합니다.
 *
 * @Data: Lombok을 통해 getter, setter 등을 자동 생성.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ZtMenu extends AuditInfo {

    // 메뉴 ID (PK)
    private Integer menuId;

    // 부모 메뉴 ID (FK)
    private Integer parentMenuId;

    // 컴포넌트 이름
    private String name;

    // 라우트 경로
    private String path;

    // 메뉴 이름
    private String title;

    // 메뉴 아이콘
    private String icon;

    // 메뉴 숨김 여부 (Y/N)
    private String isHide;

    // 탭 숨김 여부 (Y/N)
    private String isHideTab;

    // 링크
    private String link;

    // iframe 여부 (Y/N)
    private String isIframe;

    // 캐시 여부 (Y/N)
    private String keepAlive;

    // 메뉴 표시 순서
    private Integer orderSeq;

    // 활성 여부 (Y/N)
    private String isActive;

}