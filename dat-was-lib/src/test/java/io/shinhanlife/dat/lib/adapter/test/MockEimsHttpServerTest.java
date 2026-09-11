package io.shinhanlife.dat.lib.adapter.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockEimsHttpServerTest {

    @Test
    void returnsTheScaffoldGeneratedJsonResponse() {
        MockEimsHttpServer server = new MockEimsHttpServer(new ObjectMapper());

        var response = server.mockToolHttpResponse("cmm_memo_retriever", null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().path("resultCode").asText()).isEqualTo("SUCCESS");
    }
}
