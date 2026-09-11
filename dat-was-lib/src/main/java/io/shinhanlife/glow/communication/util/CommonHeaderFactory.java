package io.shinhanlife.glow.communication.util;
import io.shinhanlife.glow.communication.dto.CommonHeader;
import io.shinhanlife.glow.communication.dto.HeaderDefaults;
import java.util.Map;
public class CommonHeaderFactory {
    public static CommonHeader createRequestHeader(Map<HeaderDefaults, String> commonHeaderMap) {
        CommonHeader header = new CommonHeader();
        header.setItrfId(commonHeaderMap.get(HeaderDefaults.ITRF_ID));
        header.setRcvSvcId(commonHeaderMap.get(HeaderDefaults.RCV_SVC_ID));
        return header;
    }
    public static CommonHeader createRequestHeader(String itrfId, String rcvSvcId) {
        CommonHeader header = new CommonHeader();
        header.setItrfId(itrfId);
        header.setRcvSvcId(rcvSvcId);
        return header;
    }
}
