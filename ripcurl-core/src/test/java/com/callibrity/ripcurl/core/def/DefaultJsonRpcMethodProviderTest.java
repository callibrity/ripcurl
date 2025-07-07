package com.callibrity.ripcurl.core.def;

import com.callibrity.ripcurl.core.spi.JsonRpcMethodHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultJsonRpcMethodProviderTest {

    public static class EchoMethodHandler implements JsonRpcMethodHandler {
        @Override
        public String methodName() {
            return "echo";
        }

        @Override
        public JsonNode call(JsonNode params) {
            return params;
        }
    }

    @Test
    void shouldReturnCopyOfListGivenToConstructor() {

        EchoMethodHandler echo = new EchoMethodHandler();
        List<JsonRpcMethodHandler> original = List.of(echo);
        var provider = new DefaultJsonRpcMethodProvider(original);

        var list = provider.getJsonRpcMethodHandlers();
        assertThat(list).isEqualTo(original);
    }
}