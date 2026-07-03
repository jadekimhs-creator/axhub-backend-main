package io.shinhanlife.axhub.common.mcp.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ErrorDetail {
    private int code;
    private String message;
}