package io.shinhanlife.dat.mcc.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestHeader;

class BusinessToolControllerHeaderContractTest {

    @Test
    void receivesTheHeadersForwardedByDapms() {
        Method method = Arrays.stream(BusinessToolController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals("executeDynamicTool"))
                .findFirst()
                .orElseThrow();

        List<String> headerNames = Arrays.stream(method.getParameters())
                .map(parameter -> parameter.getAnnotation(RequestHeader.class))
                .filter(java.util.Objects::nonNull)
                .peek(header -> assertThat(header.required()).isFalse())
                .map(RequestHeader::value)
                .toList();

        assertThat(headerNames).containsExactly(
                "x-request-id", "guid", "mcp-session-id", "employee-no", "virtual-employee-no");
    }
}
