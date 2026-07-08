package io.shinhanlife.axhub.biz.mcp.adapter.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Params {
    private String routingType;
    private String name;
    private String interfaceId;
    private Map<String, Object> data;
    private List<Map<String, Object>> spec;
}