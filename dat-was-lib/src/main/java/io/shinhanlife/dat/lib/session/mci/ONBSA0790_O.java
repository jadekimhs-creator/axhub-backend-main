package io.shinhanlife.dat.lib.session.mci;

import io.shinhanlife.glow.communication.annotation.GlowTrgmField;
import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ONBSA0790_O {

    @GlowTrgmField(order = 1, length = 1000, description = "인사정보DTO리스트")
    private List<PrafInfoDto> prafInfoDto;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrafInfoDto {
        @GlowTrgmField(order = 1, length = 7, description = "인사번호")
        private String prafNo;

        @GlowTrgmField(order = 2, length = 2, description = "인사유형코드")
        private String prafTypeCd;

        @GlowTrgmField(order = 3, length = 100, description = "인사명")
        private String prafNm;

        @GlowTrgmField(order = 22, length = 3, description = "영업직책코드")
        private String bsduCd;

        @GlowTrgmField(order = 28, length = 6, description = "조직분류코드")
        private String ognzAsrtCd;

        @GlowTrgmField(order = 29, length = 6, description = "인사조직분류코드")
        private String psmrAsrtCd;

        @GlowTrgmField(order = 30, length = 6, description = "영업규정분류코드")
        private String sbsnRulpAsrtCd;

        @GlowTrgmField(order = 31, length = 6, description = "조직레벨코드")
        private String ognzLeveCd;
        
        @GlowTrgmField(order = 32, length = 7, description = "지점번호")
        private String brafNo;
        
        @GlowTrgmField(order = 33, length = 7, description = "영업소코드")
        private String bsquCd;
    }
}
