package io.shinhanlife.dat.lib.integration.mci.component;

import io.shinhanlife.dat.lib.session.dto.SessionDto;
import io.shinhanlife.dat.lib.util.SessionUtil;
import io.shinhanlife.dat.lib.config.GlowCommunicationProperties;
import io.shinhanlife.glow.BizException;
import io.shinhanlife.glow.communication.dto.CommonHeader;
import io.shinhanlife.glow.communication.dto.HeaderDefaults;
import io.shinhanlife.glow.communication.dto.Transfer;
import io.shinhanlife.glow.communication.module.mci.component.GlowMciComponent;
import io.shinhanlife.glow.communication.util.CommonHeaderFactory;
import io.shinhanlife.dat.lib.integration.mci.enums.IndvCtinRoleTyp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 신한라이프 내부 Glow 표준 컴포넌트 어댑터 (AXHUB)
 */
@Slf4j
@Component
@RequiredArgsConstructor

/**
 * @package io.shinhanlife.dat.lib.integration.mci.component
 * @className AxhubMciComponent
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
public class AxhubMciComponent {
    
    @SuppressWarnings("rawtypes")
    private final GlowMciComponent mci;
    private final GlowCommunicationProperties communicationProperties;

    /** 전문생성채널유형코드 : 1 (채널계) */
    private final static String TGRM_CREA_CHNN_TYPE_CD_1 = "1";
    private static final String SUCO_UNBL_CODE = "NNB00147"; // 청약불가

    /**
     * 전문 Common Header 생성
     * @param itrfName 인터페이스Id
     * @param rcvSvcId 수신서비스Id
     * @return Map<HeaderDefaults, String>
     */
    private Map<HeaderDefaults, String> createCommonHeaderMap(String itrfName, String rcvSvcId) {
        SessionDto sessionDto = SessionUtil.getSession();
        Map<HeaderDefaults, String> commonHeaderMap = new HashMap<>();
        
        commonHeaderMap.put(HeaderDefaults.ITRF_ID, itrfName);
        commonHeaderMap.put(HeaderDefaults.RCV_SVC_ID, rcvSvcId);
        
        if (sessionDto != null) {
            commonHeaderMap.put(HeaderDefaults.STR_YMD, sessionDto.getStrYmd());
            commonHeaderMap.put(HeaderDefaults.ACNT_OGNZ_NO, sessionDto.getBrafNo());
            commonHeaderMap.put(HeaderDefaults.PSMR_ASRT_CD, sessionDto.getPsmrAsrtCd());
            commonHeaderMap.put(HeaderDefaults.SBSN_RULP_ASRT_CD, sessionDto.getSbsnRulpAsrtCd());
            commonHeaderMap.put(HeaderDefaults.BSDU_CD, sessionDto.getBsduCd());
            commonHeaderMap.put(HeaderDefaults.BSQU_CD, sessionDto.getBsquCd());
            commonHeaderMap.put(HeaderDefaults.OGNZ_ASRT_CD, sessionDto.getOgnzAsrtCd());
            commonHeaderMap.put(HeaderDefaults.OGNZ_LEVE_CD, sessionDto.getOgnzLeveCd());
            commonHeaderMap.put(HeaderDefaults.SCRN_ID, sessionDto.getPrgrId());
            commonHeaderMap.put(HeaderDefaults.DRTM_CD, sessionDto.getPrafDutyCd());
            commonHeaderMap.put(HeaderDefaults.USER_ID, sessionDto.getPrafNo());
        }
        
        commonHeaderMap.put(HeaderDefaults.INDV_CTIN_ROLE_CD, IndvCtinRoleTyp.CD_Z99.getCode());
        commonHeaderMap.put(HeaderDefaults.TGRM_CREA_CHNN_TYPE_CD, TGRM_CREA_CHNN_TYPE_CD_1);
        
        if (communicationProperties != null && communicationProperties.getCommon() != null) {
            commonHeaderMap.put(HeaderDefaults.ENVR_TYPE_CD, communicationProperties.getCommon().getEnvType());
        }
        
        return commonHeaderMap;
    }

    @SuppressWarnings("unchecked")
    private <O> Transfer<O> syncMci(Transfer<Object> request) {
        // LOG 저장 (AXHUB 방식 로깅)
        CommonHeader reqHeader = (CommonHeader) request.getHeader();
        log.info("[AxhubMciComponent] {} MCI 호출시작 (수신서비스: {})", reqHeader.getItrfId(), reqHeader.getRcvSvcId());
        
        Transfer<O> response;
        try {
            response = (Transfer<O>) mci.sync(request);
        } catch (RuntimeException e) {
            throw new BizException("CST00477", new String[]{"대내 MCI 호출 결과 처리중 오류가 발생했습니다."}, e);
        }
        
        log.info("[AxhubMciComponent] {} MCI 호출종료 (수신서비스: {})", reqHeader.getItrfId(), reqHeader.getRcvSvcId());

        if (response != null && response.getHeader() != null) {
            CommonHeader resHeader = (CommonHeader) response.getHeader();
            String tgrmDalRsltCd = resHeader.getTgrmDalRsltCd();
            // TODO 추가 메시지 처리 및 오류 코드 제어 로직
        }

        return response;
    }

    public <O, I> Transfer<O> callTo(String itrfName, String rcvSvcId, I inputDto) throws Exception {
        Map<HeaderDefaults, String> commonHeaderMap = createCommonHeaderMap(itrfName, rcvSvcId);
        CommonHeader header = CommonHeaderFactory.createRequestHeader(commonHeaderMap);

        Transfer<Object> request = Transfer.builder()
                .header(header)
                .body(inputDto)
                .build();

        return syncMci(request);
    }

    /**
     * 대내 mci 호출
     * @param itrfName 인터페이스Id
     * @param rcvSvcId 수신서비스Id
     * @param inputDto inputDto
     * @param resBodyClass resBodyClass
     * @return Transfer
     * @param <O> resBodyClass 제너릭
     * @param <I> inputDto 제너릭
     */
    @SuppressWarnings("unchecked")
    public <O, I> Transfer<O> callTo(String itrfName, String rcvSvcId, I inputDto, Class<O> resBodyClass) throws Exception {
        Map<HeaderDefaults, String> commonHeaderMap = createCommonHeaderMap(itrfName, rcvSvcId);
        CommonHeader header = CommonHeaderFactory.createRequestHeader(commonHeaderMap);

        Transfer<Object> request = Transfer.builder()
                .header(header)
                .body(inputDto)
                .resBodyClass((Class<Object>) (Class<?>) resBodyClass)
                .build();
                
        return syncMci(request);
    }

    /**
     * 대내 mci 호출 (rcvSvcId 없는 경우)
     * @param itrfName 인터페이스Id
     * @param inputDTO 수신서비스Id (클래스명 대체)
     * @param resBodyClass resBodyClass
     * @return Transfer
     * @param <O> resBodyClass 제너릭
     * @param <I> inputDto 제너릭
     * @throws Exception Exception
     */
    public <O, I> Transfer<O> callTo(String itrfName, I inputDTO, Class<O> resBodyClass) throws Exception {
        String className = inputDTO.getClass().getSimpleName();
        String rcvSvcId = className.replace("_I", "");
        return callTo(itrfName, rcvSvcId, inputDTO, resBodyClass);
    }

    /**
     * 대내 mci 호출 (Response body class와 rcvSvcId 없는 경우)
     * @param itrfName 인터페이스Id
     * @param inputDTO 수신서비스Id (클래스명 대체)
     * @return Transfer
     * @param <O> resBodyClass 제너릭
     * @param <I> inputDto 제너릭
     * @throws Exception Exception
     */
    public <O, I> Transfer<O> callTo(String itrfName, I inputDTO) throws Exception {
        String className = inputDTO.getClass().getSimpleName();
        String rcvSvcId = className.replace("_I", "");
        return callTo(itrfName, rcvSvcId, inputDTO);
    }
}
