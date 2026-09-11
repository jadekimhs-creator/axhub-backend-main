package io.shinhanlife.dat.lib.session.mci;

import io.shinhanlife.glow.communication.annotation.GlowTrgmField;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ONBSA0790_I {

    @GlowTrgmField(order = 1, length = 100, description = "설계사인사화면조회입력DTO")
    private PlnrPrafScrnInqrInDto plnrPrafScrnInqrInDto;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlnrPrafScrnInqrInDto {
        @GlowTrgmField(order = 1, length = 7, description = "인사번호")
        private String prafNo;
    }
}
