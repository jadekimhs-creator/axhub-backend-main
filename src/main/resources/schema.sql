CREATE SCHEMA IF NOT EXISTS S_TIS;
CREATE SCHEMA IF NOT EXISTS S_EIAM_TIS;
SET SCHEMA S_TIS;

-- ZT_통합코드
CREATE TABLE ZT_UNFC_CD (
                            UNFC_CD_ID VARCHAR(50) NOT NULL,
                            UNFC_CD_NM VARCHAR(100),
                            UNFC_CD_HAN_NM VARCHAR(100),
                            UNFC_CD_ENG_NM VARCHAR(100),
                            UNFC_CD_DS VARCHAR(4000),
                            META_SYST_CD VARCHAR(3) NOT NULL,
                            META_DUTJ_TYPE_CD VARCHAR(5) NOT NULL,
                            PUSE_YN VARCHAR(1) NOT NULL,
                            SPPO_UNFC_CD_ID VARCHAR(50),
                            DATA_LOAD_DT DATE,
                            SYST_RGI_DT DATE NOT NULL,
                            SYST_RGI_PRAF_NO VARCHAR(8) NOT NULL,
                            SYST_RGI_OGNZ_NO VARCHAR(7) NOT NULL,
                            SYST_RGI_SYST_CD VARCHAR(3) NOT NULL,
                            SYST_RGI_PRGR_ID VARCHAR(100) NOT NULL,
                            SYST_CHG_DT DATE NOT NULL,
                            SYST_CHG_PRAF_NO VARCHAR(8) NOT NULL,
                            SYST_CHG_OGNZ_NO VARCHAR(7) NOT NULL,
                            SYST_CHG_SYST_CD VARCHAR(3) NOT NULL,
                            SYST_CHG_PRGR_ID VARCHAR(100) NOT NULL,
                            PRIMARY KEY (UNFC_CD_ID)
);

COMMENT ON TABLE  ZT_UNFC_CD                  IS '통합코드';
COMMENT ON COLUMN ZT_UNFC_CD.UNFC_CD_ID       IS '통합코드ID (PK)';
COMMENT ON COLUMN ZT_UNFC_CD.UNFC_CD_NM       IS '통합코드명';
COMMENT ON COLUMN ZT_UNFC_CD.UNFC_CD_HAN_NM   IS '통합코드한글명';
COMMENT ON COLUMN ZT_UNFC_CD.UNFC_CD_ENG_NM   IS '통합코드영문명';
COMMENT ON COLUMN ZT_UNFC_CD.UNFC_CD_DS       IS '통합코드설명';
COMMENT ON COLUMN ZT_UNFC_CD.META_SYST_CD     IS '메타시스템코드';
COMMENT ON COLUMN ZT_UNFC_CD.META_DUTJ_TYPE_CD IS '메타업무유형코드';
COMMENT ON COLUMN ZT_UNFC_CD.PUSE_YN          IS '사용여부 (Y/N)';
COMMENT ON COLUMN ZT_UNFC_CD.SPPO_UNFC_CD_ID  IS '상위통합코드ID';
COMMENT ON COLUMN ZT_UNFC_CD.DATA_LOAD_DT     IS '데이터적재일시';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_RGI_DT      IS '시스템등록일시';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_CHG_DT      IS '시스템변경일시';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN ZT_UNFC_CD.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';

-- ZT_통합코드 상세
CREATE TABLE S_TIS.ZT_UNFC_CD_DET
(
    UNFC_CD_ID VARCHAR(50) NOT NULL,
    CD_VLDT_VALU VARCHAR(50) NOT NULL,
    CD_VLDT_VALU_INQR_SEQ NUMBER(5) NULL,
    CD_VLDT_VALU_NM VARCHAR(400) NULL,
    CD_VLDT_VALU_HNGL_ABR_NM VARCHAR(200) NULL,
    CD_VLDT_VALU_ENGC_ABR_NM VARCHAR(200) NULL,
    CD_VLDT_VALU_HAN_NM VARCHAR(400) NULL,
    CD_VLDT_VALU_ENG_NM VARCHAR(400) NULL,
    CD_VLDT_VALU_DS VARCHAR(4000) NULL,
    CD_ASRT_TYPE_BIT_CD VARCHAR(20) NOT NULL,
    SPPO_CD_VLDT_VALU VARCHAR(50) NULL,
    SPPO_UNFC_CD_ID VARCHAR(50) NULL,
    VLDT_STRT_YMD DATE NULL,
    VLDT_END_YMD DATE NULL,
    USER_DEF_VALU_N01 VARCHAR(100) NULL,
    USER_DEF_VALU_N02 VARCHAR(100) NULL,
    SYST_RGI_DT DATE NOT NULL,
    SYST_RGI_PRAF_NO VARCHAR(8) NOT NULL,
    SYST_RGI_OGNZ_NO VARCHAR(7) NOT NULL,
    SYST_RGI_SYST_CD VARCHAR(3) NOT NULL,
    SYST_RGI_PRGR_ID VARCHAR(100) NOT NULL,
    SYST_CHG_DT DATE NOT NULL,
    SYST_CHG_PRAF_NO VARCHAR(8) NOT NULL,
    SYST_CHG_OGNZ_NO VARCHAR(7) NOT NULL,
    SYST_CHG_SYST_CD VARCHAR(3) NOT NULL,
    SYST_CHG_PRGR_ID VARCHAR(100) NOT NULL,
    DATA_LOAD_DT DATE NULL
);

COMMENT ON TABLE  S_TIS.ZT_UNFC_CD_DET                          IS '통합코드 상세';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.UNFC_CD_ID               IS '통합코드ID (PK, 부모)';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU             IS '코드유효값 (PK)';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_INQR_SEQ   IS '코드유효값조회순서';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_NM          IS '코드유효값명';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_HNGL_ABR_NM IS '코드유효값한글약어명';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_ENGC_ABR_NM IS '코드유효값영문약어명';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_HAN_NM      IS '코드유효값한글명';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_ENG_NM      IS '코드유효값영문명';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_VLDT_VALU_DS          IS '코드유효값설명';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.CD_ASRT_TYPE_BIT_CD      IS '코드분류유형비트코드';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SPPO_CD_VLDT_VALU        IS '상위코드유효값';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SPPO_UNFC_CD_ID          IS '상위통합코드ID';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.VLDT_STRT_YMD            IS '유효시작일자';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.VLDT_END_YMD             IS '유효종료일자';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.USER_DEF_VALU_N01        IS '사용자정의값1';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.USER_DEF_VALU_N02        IS '사용자정의값2';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_RGI_DT              IS '시스템등록일시';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_RGI_PRAF_NO         IS '시스템등록인사번호';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_RGI_OGNZ_NO         IS '시스템등록조직번호';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_RGI_SYST_CD         IS '시스템등록시스템코드';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_RGI_PRGR_ID         IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_CHG_DT              IS '시스템변경일시';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_CHG_PRAF_NO         IS '시스템변경인사번호';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_CHG_OGNZ_NO         IS '시스템변경조직번호';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_CHG_SYST_CD         IS '시스템변경시스템코드';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.SYST_CHG_PRGR_ID         IS '시스템변경프로그램ID';
COMMENT ON COLUMN S_TIS.ZT_UNFC_CD_DET.DATA_LOAD_DT             IS '데이터적재일시';

-------------------------------------------------------------------------------------------
-- ZT_MENU 테이블 생성 (메뉴 기본 정보 저장)
CREATE TABLE ZT_MENU (
                         MENU_ID          INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,  -- 자동 증가 ID (PK)
                         PARENT_MENU_ID   INTEGER,                                           -- 부모 메뉴 ID (FK, NULL 가능 for 루트 메뉴)
                         NAME             VARCHAR(100),                                      -- 컴포넌트 이름 (옵셔널)
                         PATH             VARCHAR(255) NOT NULL,                             -- 라우트 경로 (필수)
                         TITLE            VARCHAR(100) NOT NULL,                             -- 메뉴 이름 (meta.title, 필수)
                         ICON             VARCHAR(50),                                       -- 메뉴 아이콘 (옵셔널)
                         SHOW_BADGE       VARCHAR(1) DEFAULT 'N' CHECK (SHOW_BADGE IN ('Y', 'N')), -- 배지 표시 여부 (Y/N, 옵셔널)
                         SHOW_TEXT_BADGE  VARCHAR(50),                                       -- 텍스트 배지 표시 (옵셔널)
                         IS_HIDE          VARCHAR(1) DEFAULT 'N' CHECK (IS_HIDE IN ('Y', 'N')),    -- 메뉴 숨김 여부 (Y/N, 옵셔널)
                         IS_HIDE_TAB      VARCHAR(1) DEFAULT 'N' CHECK (IS_HIDE_TAB IN ('Y', 'N')),-- 탭 숨김 여부 (Y/N, 옵셔널)
                         LINK             VARCHAR(255),                                      -- 링크 (옵셔널)
                         IS_IFRAME        VARCHAR(1) DEFAULT 'N' CHECK (IS_IFRAME IN ('Y', 'N')),  -- iframe 여부 (Y/N, 옵셔널)
                         KEEP_ALIVE       VARCHAR(1) DEFAULT 'N' CHECK (KEEP_ALIVE IN ('Y', 'N')), -- 캐시 여부 (Y/N, 옵셔널)
                         ORDER_SEQ        INTEGER DEFAULT 0 NOT NULL,                        -- 메뉴 표시 순서 (정렬용, 필수)
                         IS_ACTIVE        VARCHAR(1) DEFAULT 'Y' CHECK (IS_ACTIVE IN ('Y', 'N')),  -- 활성 여부 (Y/N, 기본 Y)
                         SYST_RGI_DT      DATE,                                              -- 시스템등록일시
                         SYST_RGI_PRAF_NO VARCHAR(50),                                       -- 시스템등록인사번호
                         SYST_RGI_OGNZ_NO VARCHAR(50),                                       -- 시스템등록조직번호
                         SYST_RGI_SYST_CD VARCHAR(50),                                       -- 시스템등록시스템코드
                         SYST_RGI_PRGR_ID VARCHAR(50),                                       -- 시스템등록프로그램ID
                         SYST_CHG_DT      DATE,                                              -- 시스템변경일시
                         SYST_CHG_PRAF_NO VARCHAR(50),                                       -- 시스템변경인사번호
                         SYST_CHG_OGNZ_NO VARCHAR(50),                                       -- 시스템변경조직번호
                         SYST_CHG_SYST_CD VARCHAR(50),                                       -- 시스템변경시스템코드
                         SYST_CHG_PRGR_ID VARCHAR(50)                                        -- 시스템변경프로그램ID
);

-- 제약 조건 추가
ALTER TABLE ZT_MENU ADD CONSTRAINT FK_ZT_MENU_PARENT FOREIGN KEY (PARENT_MENU_ID) REFERENCES ZT_MENU (MENU_ID) ON DELETE CASCADE;  -- 부모 삭제 시 자식도 삭제 (계층 삭제 지원)

-- 인덱스 생성 (조회 최적화)
CREATE INDEX IDX_ZT_MENU_PARENT ON ZT_MENU (PARENT_MENU_ID);
CREATE INDEX IDX_ZT_MENU_PATH ON ZT_MENU (PATH);

-- 컬럼 코멘트 추가
COMMENT ON COLUMN ZT_MENU.MENU_ID IS '메뉴 ID (PK)';
COMMENT ON COLUMN ZT_MENU.PARENT_MENU_ID IS '부모 메뉴 ID (FK)';
COMMENT ON COLUMN ZT_MENU.NAME IS '컴포넌트 이름';
COMMENT ON COLUMN ZT_MENU.PATH IS '라우트 경로';
COMMENT ON COLUMN ZT_MENU.TITLE IS '메뉴 이름';
COMMENT ON COLUMN ZT_MENU.ICON IS '메뉴 아이콘';
COMMENT ON COLUMN ZT_MENU.SHOW_BADGE IS '배지 표시 여부 (Y/N)';
COMMENT ON COLUMN ZT_MENU.SHOW_TEXT_BADGE IS '텍스트 배지 표시';
COMMENT ON COLUMN ZT_MENU.IS_HIDE IS '메뉴 숨김 여부 (Y/N)';
COMMENT ON COLUMN ZT_MENU.IS_HIDE_TAB IS '탭 숨김 여부 (Y/N)';
COMMENT ON COLUMN ZT_MENU.LINK IS '링크';
COMMENT ON COLUMN ZT_MENU.IS_IFRAME IS 'iframe 여부 (Y/N)';
COMMENT ON COLUMN ZT_MENU.KEEP_ALIVE IS '캐시 여부 (Y/N)';
COMMENT ON COLUMN ZT_MENU.ORDER_SEQ IS '메뉴 표시 순서';
COMMENT ON COLUMN ZT_MENU.IS_ACTIVE IS '활성 여부 (Y/N)';
COMMENT ON COLUMN ZT_MENU.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN ZT_MENU.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN ZT_MENU.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN ZT_MENU.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN ZT_MENU.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN ZT_MENU.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN ZT_MENU.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN ZT_MENU.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN ZT_MENU.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN ZT_MENU.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';

-- ============================================================
-- AX HUB 역할-Tool/지식 권한 관리 DDL
-- 대상 스키마 : 프로젝트 AP 스키마 (S_EIAM_TIS 는 읽기전용 참조)
-- 작성일 : 2026-06-25
-- ============================================================

-- ① Tool 마스터
CREATE TABLE AX_TOOL_MST (
                             TOOL_ID          VARCHAR(20)  NOT NULL,   -- Tool ID (PK)
                             TOOL_NM          VARCHAR(100) NOT NULL,   -- Tool명
                             TOOL_DS          VARCHAR(500),            -- Tool설명
                             TOOL_TYPE_CD     VARCHAR(20),             -- Tool유형코드 (MCP / FUNC / EXT)
                             PUSE_YN          VARCHAR(1)   DEFAULT 'Y',-- 사용여부
                             SYST_RGI_DT      DATE,
                             SYST_RGI_PRAF_NO VARCHAR(8),
                             SYST_RGI_OGNZ_NO VARCHAR(7),
                             SYST_RGI_SYST_CD VARCHAR(3),
                             SYST_RGI_PRGR_ID VARCHAR(100),
                             SYST_CHG_DT      DATE,
                             SYST_CHG_PRAF_NO VARCHAR(8),
                             SYST_CHG_OGNZ_NO VARCHAR(7),
                             SYST_CHG_SYST_CD VARCHAR(3),
                             SYST_CHG_PRGR_ID VARCHAR(100),
                             CONSTRAINT PK_AX_TOOL_MST PRIMARY KEY (TOOL_ID)
);

COMMENT ON TABLE  AX_TOOL_MST              IS 'AX HUB Tool 마스터';
COMMENT ON COLUMN AX_TOOL_MST.TOOL_ID      IS 'Tool ID (PK)';
COMMENT ON COLUMN AX_TOOL_MST.TOOL_NM      IS 'Tool명';
COMMENT ON COLUMN AX_TOOL_MST.TOOL_DS      IS 'Tool설명';
COMMENT ON COLUMN AX_TOOL_MST.TOOL_TYPE_CD IS 'Tool유형코드 (MCP/FUNC/EXT)';
COMMENT ON COLUMN AX_TOOL_MST.PUSE_YN      IS '사용여부 (Y/N)';


-- ② 지식(Knowledge) 마스터
CREATE TABLE AX_KNWL_MST (
                             KNWL_ID          VARCHAR(20)  NOT NULL,   -- 지식 ID (PK)
                             KNWL_NM          VARCHAR(100) NOT NULL,   -- 지식명
                             KNWL_DS          VARCHAR(500),            -- 지식설명
                             KNWL_TYPE_CD     VARCHAR(20),             -- 지식유형코드 (PRODUCT/MANUAL/LEGAL 등)
                             PUSE_YN          VARCHAR(1)   DEFAULT 'Y',
                             SYST_RGI_DT      DATE,
                             SYST_RGI_PRAF_NO VARCHAR(8),
                             SYST_RGI_OGNZ_NO VARCHAR(7),
                             SYST_RGI_SYST_CD VARCHAR(3),
                             SYST_RGI_PRGR_ID VARCHAR(100),
                             SYST_CHG_DT      DATE,
                             SYST_CHG_PRAF_NO VARCHAR(8),
                             SYST_CHG_OGNZ_NO VARCHAR(7),
                             SYST_CHG_SYST_CD VARCHAR(3),
                             SYST_CHG_PRGR_ID VARCHAR(100),
                             CONSTRAINT PK_AX_KNWL_MST PRIMARY KEY (KNWL_ID)
);

COMMENT ON TABLE  AX_KNWL_MST              IS 'AX HUB 지식 마스터';
COMMENT ON COLUMN AX_KNWL_MST.KNWL_ID      IS '지식 ID (PK)';
COMMENT ON COLUMN AX_KNWL_MST.KNWL_NM      IS '지식명';
COMMENT ON COLUMN AX_KNWL_MST.KNWL_DS      IS '지식설명';
COMMENT ON COLUMN AX_KNWL_MST.KNWL_TYPE_CD IS '지식유형코드 (PRODUCT/MANUAL/LEGAL/REPORT)';
COMMENT ON COLUMN AX_KNWL_MST.PUSE_YN      IS '사용여부 (Y/N)';


-- ③ 역할-Tool 권한 매핑
--   SYST_ID + ROLE_NO → S_EIAM_TIS.AA_ROLE 참조 (FK 미설정: 스키마 분리 환경)
CREATE TABLE AX_ROLE_TOOL_ATHR (
                                   ROLE_TOOL_ATHR_ID VARCHAR(20)  NOT NULL,  -- 역할Tool권한ID (PK)
                                   SYST_ID           VARCHAR(10)  NOT NULL,  -- 시스템ID (EIAM 연동 키)
                                   ROLE_NO           VARCHAR(50)  NOT NULL,  -- 역할번호 (EIAM AA_ROLE.ROLE_NO)
                                   TOOL_ID           VARCHAR(20)  NOT NULL,  -- Tool ID
                                   PUSE_YN           VARCHAR(1)   DEFAULT 'Y',
                                   SYST_RGI_DT       DATE,
                                   SYST_RGI_PRAF_NO  VARCHAR(8),
                                   SYST_RGI_OGNZ_NO  VARCHAR(7),
                                   SYST_RGI_SYST_CD  VARCHAR(3),
                                   SYST_RGI_PRGR_ID  VARCHAR(100),
                                   SYST_CHG_DT       DATE,
                                   SYST_CHG_PRAF_NO  VARCHAR(8),
                                   SYST_CHG_OGNZ_NO  VARCHAR(7),
                                   SYST_CHG_SYST_CD  VARCHAR(3),
                                   SYST_CHG_PRGR_ID  VARCHAR(100),
                                   CONSTRAINT PK_AX_ROLE_TOOL_ATHR PRIMARY KEY (ROLE_TOOL_ATHR_ID)
);

-- (SYST_ID, ROLE_NO, TOOL_ID) 중복 방지
CREATE UNIQUE INDEX UK_AX_ROLE_TOOL_ATHR
    ON AX_ROLE_TOOL_ATHR (SYST_ID, ROLE_NO, TOOL_ID);

COMMENT ON TABLE  AX_ROLE_TOOL_ATHR                   IS '역할-Tool 권한 매핑';
COMMENT ON COLUMN AX_ROLE_TOOL_ATHR.ROLE_TOOL_ATHR_ID IS '역할Tool권한ID (PK)';
COMMENT ON COLUMN AX_ROLE_TOOL_ATHR.SYST_ID           IS '시스템ID';
COMMENT ON COLUMN AX_ROLE_TOOL_ATHR.ROLE_NO           IS '역할번호 (EIAM)';
COMMENT ON COLUMN AX_ROLE_TOOL_ATHR.TOOL_ID           IS 'Tool ID';
COMMENT ON COLUMN AX_ROLE_TOOL_ATHR.PUSE_YN           IS '사용여부';


-- ④ 역할-지식 권한 매핑
CREATE TABLE AX_ROLE_KNWL_ATHR (
                                   ROLE_KNWL_ATHR_ID VARCHAR(20)  NOT NULL,  -- 역할지식권한ID (PK)
                                   SYST_ID           VARCHAR(10)  NOT NULL,
                                   ROLE_NO           VARCHAR(50)  NOT NULL,
                                   KNWL_ID           VARCHAR(20)  NOT NULL,
                                   PUSE_YN           VARCHAR(1)   DEFAULT 'Y',
                                   SYST_RGI_DT       DATE,
                                   SYST_RGI_PRAF_NO  VARCHAR(8),
                                   SYST_RGI_OGNZ_NO  VARCHAR(7),
                                   SYST_RGI_SYST_CD  VARCHAR(3),
                                   SYST_RGI_PRGR_ID  VARCHAR(100),
                                   SYST_CHG_DT       DATE,
                                   SYST_CHG_PRAF_NO  VARCHAR(8),
                                   SYST_CHG_OGNZ_NO  VARCHAR(7),
                                   SYST_CHG_SYST_CD  VARCHAR(3),
                                   SYST_CHG_PRGR_ID  VARCHAR(100),
                                   CONSTRAINT PK_AX_ROLE_KNWL_ATHR PRIMARY KEY (ROLE_KNWL_ATHR_ID)
);

CREATE UNIQUE INDEX UK_AX_ROLE_KNWL_ATHR
    ON AX_ROLE_KNWL_ATHR (SYST_ID, ROLE_NO, KNWL_ID);

COMMENT ON TABLE  AX_ROLE_KNWL_ATHR                   IS '역할-지식 권한 매핑';
COMMENT ON COLUMN AX_ROLE_KNWL_ATHR.ROLE_KNWL_ATHR_ID IS '역할지식권한ID (PK)';
COMMENT ON COLUMN AX_ROLE_KNWL_ATHR.SYST_ID           IS '시스템ID';
COMMENT ON COLUMN AX_ROLE_KNWL_ATHR.ROLE_NO           IS '역할번호 (EIAM)';
COMMENT ON COLUMN AX_ROLE_KNWL_ATHR.KNWL_ID           IS '지식ID';
COMMENT ON COLUMN AX_ROLE_KNWL_ATHR.PUSE_YN           IS '사용여부';



-- AA_USAC 테이블 생성 (유저 마스터)
CREATE TABLE S_EIAM_TIS.AA_USAC (
                                    PRAF_NO          VARCHAR(8)  PRIMARY KEY,  -- 인사번호 (PK)
                                    PRAF_NM          VARCHAR(200),             -- 인사명
                                    OGNZ_NO          VARCHAR(7)  NOT NULL,     -- 조직번호 (NOT NULL)
                                    ADDRE            VARCHAR(250),             -- 이메일주소
                                    PRAF_OFDU_CD     VARCHAR(10),              -- 인사직무코드
                                    PRAF_OFDU_NM     VARCHAR(100),             -- 인사직무명
                                    PRAF_OFLE_CD     VARCHAR(10),              -- 인사직급코드
                                    PRAF_OFLE_NM     VARCHAR(100),             -- 인사직급명
                                    PRAF_DUTY_CD     VARCHAR(10),              -- 인사직책코드
                                    PRAF_DUTY_NM     VARCHAR(100),             -- 인사직책명
                                    MNGR_PRAF_NO     VARCHAR(8),               -- 관리자인사번호
                                    PUSE_YN          VARCHAR(1),               -- 사용여부
                                    SYST_RGI_DT      DATE,                      -- 시스템등록일시
                                    SYST_RGI_PRAF_NO VARCHAR(8),               -- 시스템등록인사번호
                                    SYST_RGI_OGNZ_NO VARCHAR(7),               -- 시스템등록조직번호
                                    SYST_RGI_SYST_CD VARCHAR(3),               -- 시스템등록시스템코드
                                    SYST_RGI_PRGR_ID VARCHAR(100),             -- 시스템등록프로그램ID
                                    SYST_CHG_DT      DATE,                      -- 시스템변경일시
                                    SYST_CHG_PRAF_NO VARCHAR(8),               -- 시스템변경인사번호
                                    SYST_CHG_OGNZ_NO VARCHAR(7),               -- 시스템변경조직번호
                                    SYST_CHG_SYST_CD VARCHAR(3),               -- 시스템변경시스템코드
                                    SYST_CHG_PRGR_ID VARCHAR(100)             -- 시스템변경프로그램ID
);

-- 인덱스 생성
CREATE INDEX IDX_AA_USAC_OGNZ_NO ON S_EIAM_TIS.AA_USAC (OGNZ_NO);

-- 컬럼 코멘트 추가
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_NO IS '인사번호 (PK)';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_NM IS '인사명';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.OGNZ_NO IS '조직번호 (NOT NULL)';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.ADDRE IS '이메일주소';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_OFDU_CD IS '인사직무코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_OFDU_NM IS '인사직무명';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_OFLE_CD IS '인사직급코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_OFLE_NM IS '인사직급명';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_DUTY_CD IS '인사직책코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PRAF_DUTY_NM IS '인사직책명';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.MNGR_PRAF_NO IS '관리자인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.PUSE_YN IS '사용여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';



-- 위임
CREATE TABLE S_EIAM_TIS.AA_MNDT
(
    MNDT_ID             VARCHAR(10) NOT NULL,
    SYST_ID             VARCHAR(10) NOT NULL,
    PRAF_NO             VARCHAR(8) NOT NULL,
    OGNZ_NO             VARCHAR(7),
    TGTR_PRAF_NO        VARCHAR(8) NOT NULL,
    TGTR_OGNZ_NO        VARCHAR(7),
    ROLE_NO             VARCHAR(50) NOT NULL,
    MNDT_STRT_DT        DATE,
    MNDT_END_DT         DATE,
    MNDT_DS             VARCHAR(1000),
    PUSE_YN             VARCHAR(1),
    SYST_RGI_DT         DATE,
    SYST_RGI_PRAF_NO    VARCHAR(8),
    SYST_RGI_OGNZ_NO    VARCHAR(7),
    SYST_RGI_SYST_CD    VARCHAR(3),
    SYST_RGI_PRGR_ID    VARCHAR(100),
    SYST_CHG_DT         DATE,
    SYST_CHG_PRAF_NO    VARCHAR(8),
    SYST_CHG_OGNZ_NO    VARCHAR(7),
    SYST_CHG_SYST_CD    VARCHAR(3),
    SYST_CHG_PRGR_ID    VARCHAR(100)
);

ALTER TABLE S_EIAM_TIS.AA_MNDT ADD CONSTRAINT PK_AA_MNDT PRIMARY KEY (MNDT_ID);

COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.MNDT_ID IS '위임ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_ID IS '시스템ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.PRAF_NO IS '인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.OGNZ_NO IS '조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.TGTR_PRAF_NO IS '대상자인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.TGTR_OGNZ_NO IS '대상자조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.ROLE_NO IS '역할번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.MNDT_STRT_DT IS '위임시작일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.MNDT_END_DT IS '위임종료일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.MNDT_DS IS '위임설명';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.PUSE_YN IS '사용여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_MNDT.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';

-- 역할
CREATE TABLE S_EIAM_TIS.AA_ROLE
(
    SYST_ID                 VARCHAR(10) NOT NULL,
    ROLE_NO                 VARCHAR(50) NOT NULL,
    ROLE_NM                 VARCHAR(100),
    ROLE_DS                 VARCHAR(1000),
    ASST_OWNR_ROLE_TYPE_CD  VARCHAR(3),
    MASK_ECPT_MD_CD         VARCHAR(1),
    INDV_INFO_ATHR_YN       VARCHAR(1),
    MAIN_CST_IFIN_YN        VARCHAR(1),
    PUSE_YN                 VARCHAR(1),
    SYST_RGI_DT             DATE,
    SYST_RGI_PRAF_NO        VARCHAR(8),
    SYST_RGI_OGNZ_NO        VARCHAR(7),
    SYST_RGI_SYST_CD        VARCHAR(3),
    SYST_RGI_PRGR_ID        VARCHAR(100),
    SYST_CHG_DT             DATE,
    SYST_CHG_PRAF_NO        VARCHAR(8),
    SYST_CHG_OGNZ_NO        VARCHAR(7),
    SYST_CHG_SYST_CD        VARCHAR(3),
    SYST_CHG_PRGR_ID        VARCHAR(100)
);

CREATE UNIQUE INDEX S_EIAM_TIS.PK_AA_ROLE
    ON S_EIAM_TIS.AA_ROLE (SYST_ID,ROLE_NO);

ALTER TABLE S_EIAM_TIS.AA_ROLE ADD CONSTRAINT PK_AA_ROLE PRIMARY KEY (SYST_ID,ROLE_NO);

COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_ID IS '시스템ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.ROLE_NO IS '역할번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.ROLE_NM IS '역할명';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.ROLE_DS IS '역할설명';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.ASST_OWNR_ROLE_TYPE_CD IS '자산소유자역할유형코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.MASK_ECPT_MD_CD IS '마스킹제외방법코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.INDV_INFO_ATHR_YN IS '개인정보권한여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.MAIN_CST_IFIN_YN IS '주요고객정보조회여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.PUSE_YN IS '사용여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_ROLE.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';


-- 사용자그룹
CREATE TABLE S_EIAM_TIS.AA_USER_GROU
(
    SYST_ID             VARCHAR(10) NOT NULL,
    USER_GROU_ID        VARCHAR(10) NOT NULL,
    OGNZ_NO             VARCHAR(7),
    USER_GROU_NM        VARCHAR(100),
    USER_GROU_DS        VARCHAR(1000),
    PUSE_YN             VARCHAR(1),
    SYST_RGI_DT         DATE,
    SYST_RGI_PRAF_NO    VARCHAR(8),
    SYST_RGI_OGNZ_NO    VARCHAR(7),
    SYST_RGI_SYST_CD    VARCHAR(3),
    SYST_RGI_PRGR_ID    VARCHAR(100),
    SYST_CHG_DT         DATE,
    SYST_CHG_PRAF_NO    VARCHAR(8),
    SYST_CHG_OGNZ_NO    VARCHAR(7),
    SYST_CHG_SYST_CD    VARCHAR(3),
    SYST_CHG_PRGR_ID    VARCHAR(100)
);

CREATE UNIQUE INDEX S_EIAM_TIS.PK_AA_USER_GROU
    ON S_EIAM_TIS.AA_USER_GROU (SYST_ID,USER_GROU_ID);

ALTER TABLE S_EIAM_TIS.AA_USER_GROU
    ADD CONSTRAINT PK_AA_USER_GROU PRIMARY KEY (SYST_ID,USER_GROU_ID);


COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_ID IS '시스템ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.USER_GROU_ID IS '사용자그룹ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.OGNZ_NO IS '조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.USER_GROU_NM IS '사용자그룹명';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.USER_GROU_DS IS '사용자그룹설명';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.PUSE_YN IS '사용여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';


-- 사용자/그룹관계
CREATE TABLE S_EIAM_TIS.AA_USAC_USER_GROU_RTNS
(
    USAC_USER_GROU_RTNS_ID  VARCHAR(10) NOT NULL,
    SYST_ID                 VARCHAR(10) NOT NULL,
    PRAF_NO                 VARCHAR(8) NOT NULL,
    USER_GROU_ID            VARCHAR(10) NOT NULL,
    VLDT_YMD                DATE,
    EXPR_YMD                DATE,
    PUSE_YN                 VARCHAR(1),
    SYST_RGI_DT             DATE,
    SYST_RGI_PRAF_NO        VARCHAR(8),
    SYST_RGI_OGNZ_NO        VARCHAR(7),
    SYST_RGI_SYST_CD        VARCHAR(3),
    SYST_RGI_PRGR_ID        VARCHAR(100),
    SYST_CHG_DT             DATE,
    SYST_CHG_PRAF_NO        VARCHAR(8),
    SYST_CHG_OGNZ_NO        VARCHAR(7),
    SYST_CHG_SYST_CD        VARCHAR(3),
    SYST_CHG_PRGR_ID        VARCHAR(100)
);

CREATE UNIQUE INDEX S_EIAM_TIS.PK_AA_USAC_USER_GROU_RTNS
    ON S_EIAM_TIS.AA_USAC_USER_GROU_RTNS (USAC_USER_GROU_RTNS_ID);

ALTER TABLE S_EIAM_TIS.AA_USAC_USER_GROU_RTNS
    ADD CONSTRAINT PK_AA_USAC_USER_GROU_RTNS PRIMARY KEY (USAC_USER_GROU_RTNS_ID);


COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.USAC_USER_GROU_RTNS_ID IS '사용자계정사용자그룹관계ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_ID IS '시스템ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.PRAF_NO IS '인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.USER_GROU_ID IS '사용자그룹ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.VLDT_YMD IS '유효일자';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.EXPR_YMD IS '만료일자';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.PUSE_YN IS '사용여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USAC_USER_GROU_RTNS.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';



-- 사용자그룹/역할 관계
CREATE TABLE S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS
(
    USER_GROU_ROLE_RTNS_ID  VARCHAR(10) NOT NULL,
    SYST_ID                 VARCHAR(10) NOT NULL,
    USER_GROU_ID            VARCHAR(10) NOT NULL,
    ROLE_NO                 VARCHAR(50) NOT NULL,
    PUSE_YN                 VARCHAR(1),
    SYST_RGI_DT             DATE,
    SYST_RGI_PRAF_NO        VARCHAR(8),
    SYST_RGI_OGNZ_NO        VARCHAR(7),
    SYST_RGI_SYST_CD        VARCHAR(3),
    SYST_RGI_PRGR_ID        VARCHAR(100),
    SYST_CHG_DT             DATE,
    SYST_CHG_PRAF_NO        VARCHAR(8),
    SYST_CHG_OGNZ_NO        VARCHAR(7),
    SYST_CHG_SYST_CD        VARCHAR(3),
    SYST_CHG_PRGR_ID        VARCHAR(100)
);

CREATE UNIQUE INDEX S_EIAM_TIS.PK_AA_USER_GROU_ROLE_RTNS
    ON S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS (USER_GROU_ROLE_RTNS_ID);

ALTER TABLE S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS
    ADD CONSTRAINT PK_AA_USER_GROU_ROLE_RTNS PRIMARY KEY (USER_GROU_ROLE_RTNS_ID);


COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.USER_GROU_ROLE_RTNS_ID IS '사용자그룹역할관계ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_ID IS '시스템ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.USER_GROU_ID IS '사용자그룹ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.ROLE_NO IS '역할번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.PUSE_YN IS '사용여부';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_RGI_DT IS '시스템등록일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_RGI_PRAF_NO IS '시스템등록인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_RGI_OGNZ_NO IS '시스템등록조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_RGI_SYST_CD IS '시스템등록시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_RGI_PRGR_ID IS '시스템등록프로그램ID';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_CHG_DT IS '시스템변경일시';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_CHG_PRAF_NO IS '시스템변경인사번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_CHG_OGNZ_NO IS '시스템변경조직번호';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_CHG_SYST_CD IS '시스템변경시스템코드';
COMMENT ON COLUMN S_EIAM_TIS.AA_USER_GROU_ROLE_RTNS.SYST_CHG_PRGR_ID IS '시스템변경프로그램ID';

-- AA_OGNZ 테이블 생성 (조직)
CREATE TABLE S_EIAM_TIS.AA_OGNZ (
                                    OGNZ_NM          VARCHAR2(8)  PRIMARY KEY,  -- 인사번호 (PK)
                                    OGNZ_ABR_NM          VARCHAR2(200),             -- 인사명
                                    OGNZ_NO          VARCHAR2(7)  NOT NULL,     -- 조직번호 (NOT NULL)
                                    SPPO_GONZ_NO            VARCHAR2(250),             -- 이메일주소
                                    VLDT_YMD     VARCHAR2(10),              -- 인사직무코드
                                    PUSE_YN     VARCHAR2(100),             -- 인사직무명
                                    SYST_RGI_DT      DATE,                      -- 시스템등록일시
                                    SYST_RGI_PRAF_NO VARCHAR2(8),               -- 시스템등록인사번호
                                    SYST_RGI_OGNZ_NO VARCHAR2(7),               -- 시스템등록조직번호
                                    SYST_RGI_SYST_CD VARCHAR2(3),               -- 시스템등록시스템코드
                                    SYST_RGI_PRGR_ID VARCHAR2(100),             -- 시스템등록프로그램ID
                                    SYST_CHG_DT      DATE,                      -- 시스템변경일시
                                    SYST_CHG_PRAF_NO VARCHAR2(8),               -- 시스템변경인사번호
                                    SYST_CHG_OGNZ_NO VARCHAR2(7),               -- 시스템변경조직번호
                                    SYST_CHG_SYST_CD VARCHAR2(3),               -- 시스템변경시스템코드
                                    SYST_CHG_PRGR_ID VARCHAR2(100)             -- 시스템변경프로그램ID
);



