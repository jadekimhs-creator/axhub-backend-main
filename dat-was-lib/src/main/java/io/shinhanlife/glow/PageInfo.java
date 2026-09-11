package io.shinhanlife.glow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.session.RowBounds;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class PageInfo extends RowBounds implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 페이지번호 (입력값)
     */
    @GlowTrgmField(order = 1, length = 5, description = "페이지번호")
    private int pageNo;

    /**
     * 페이지 데이터 건수 (열 건수, 입력값)
     */
    @GlowTrgmField(order = 2, length = 5, description = "페이지데이터건수")
    private int pageDataCc;

    /**
     * 총페이지 수 (리턴값)
     */
    @GlowTrgmField(order = 3, length = 10, description = "총페이지수")
    private int totaPageCn;

    /**
     * 총 페이지 데이터 건수 (리턴값)
     */
    @GlowTrgmField(order = 4, length = 10, description = "총페이지데이터건수")
    private int totaPageDataCc;

    public PageInfo(int pageNo, int pageDataCc) {
        super(((pageNo <= 0 ? 1 : pageNo) - 1) * pageDataCc, pageDataCc);
        this.pageNo = pageNo;
        this.pageDataCc = pageDataCc;
    }

    public PageInfo() {

    }

    @JsonIgnore
    @Override
    public int getOffset() {
        return super.getOffset();
    }

    @JsonIgnore
    @Override
    public int getLimit() {
        return super.getLimit();
    }

}
