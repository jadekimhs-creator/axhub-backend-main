package io.shinhanlife.axhub.biz.mcp.adapter.dto;

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