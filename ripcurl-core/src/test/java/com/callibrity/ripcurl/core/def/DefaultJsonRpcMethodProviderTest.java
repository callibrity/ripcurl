/*
 * Copyright © 2025 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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