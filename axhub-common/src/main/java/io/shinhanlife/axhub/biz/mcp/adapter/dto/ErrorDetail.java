package io.shinhanlife.axhub.biz.mcp.adapter.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ErrorDetail {
    private int code;
    private String message;
}