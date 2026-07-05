package io.shinhanlife.axhub.biz.mcp.adapter.sender;

public interface EimsSender {
    // 프로토콜에 상관없이 이 메서드 하나로 통일합니다.
    String send(String interfaceId, String payload) throws Exception;
}
