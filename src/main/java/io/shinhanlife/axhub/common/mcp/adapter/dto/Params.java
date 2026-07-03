package io.shinhanlife.axhub.common.mcp.adapter.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Params {
    private String routingType;
    private String name;
    private String interfaceId;
    private Map<String, Object> data;
    private List<Map<String, Object>> spec;
}