package io.shinhanlife.dat.lib.session.mci;

import io.shinhanlife.dat.lib.integration.mci.component.AxhubMciComponent;
import io.shinhanlife.dat.lib.session.dto.SessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AxhubSessionService {

    private final AxhubMciComponent mciComponent;

    public SessionDto fetchUserSession(String employeeNo) {
        if (employeeNo == null || employeeNo.trim().isEmpty()) {
            return null;
        }

        try {
            ONBSA0790_I request = ONBSA0790_I.builder()
                    .plnrPrafScrnInqrInDto(ONBSA0790_I.PlnrPrafScrnInqrInDto.builder()
                            .prafNo(employeeNo)
                            .build())
                    .build();

            // callTo makes a synchronous MCI call.
            var responseTransfer = mciComponent.callTo("ONBSA0790", "ONBSA", request, ONBSA0790_O.class);
            ONBSA0790_O response = responseTransfer.getBody();

            if (response != null && response.getPrafInfoDto() != null && !response.getPrafInfoDto().isEmpty()) {
                ONBSA0790_O.PrafInfoDto info = response.getPrafInfoDto().get(0);
                
                SessionDto sessionDto = new SessionDto();
                sessionDto.setOgnzNo(info.getOgnzAsrtCd());
                sessionDto.setPrafNo(info.getPrafNo());
                sessionDto.setStrYmd(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                sessionDto.setBrafNo(info.getBrafNo());
                sessionDto.setPsmrAsrtCd(info.getPsmrAsrtCd());
                sessionDto.setSbsnRulpAsrtCd(info.getSbsnRulpAsrtCd());
                sessionDto.setBsduCd(info.getBsduCd());
                sessionDto.setBsquCd(info.getBsquCd());
                sessionDto.setOgnzAsrtCd(info.getOgnzAsrtCd());
                sessionDto.setOgnzLeveCd(info.getOgnzLeveCd());
                
                log.info("[AxhubSessionService] MCI ONBSA0790 조회 성공: 사번={}, 조직코드={}", employeeNo, info.getOgnzAsrtCd());
                return sessionDto;
            }
        } catch (Exception e) {
            log.error("[AxhubSessionService] MCI ONBSA0790 사원 정보 조회 실패 (사번: {})", employeeNo, e);
        }
        return null;
    }
}
