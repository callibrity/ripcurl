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
package com.callibrity.ripcurl.autoconfigure.aot;

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcError;
import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcMessage;
import com.callibrity.ripcurl.core.JsonRpcNotification;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.JsonRpcResult;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.def.ParameterResolutionExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

class RipCurlRuntimeHintsTest {

  @Test
  void registersBindingHintsForEverySealedJsonRpcMessageType() {
    var hints = new RuntimeHints();
    new RipCurlRuntimeHints().registerHints(hints, getClass().getClassLoader());

    assertTypeHintRegistered(hints, JsonRpcMessage.class);
    assertTypeHintRegistered(hints, JsonRpcRequest.class);
    assertTypeHintRegistered(hints, JsonRpcResponse.class);
    assertTypeHintRegistered(hints, JsonRpcCall.class);
    assertTypeHintRegistered(hints, JsonRpcNotification.class);
    assertTypeHintRegistered(hints, JsonRpcResult.class);
    assertTypeHintRegistered(hints, JsonRpcError.class);
    assertTypeHintRegistered(hints, JsonRpcErrorDetail.class);
  }

  @Test
  void registersReflectionHintsForTheExceptionTranslatorSpiAndBuiltIns() {
    // The registry reads the exception type via TypeRef at bean-init time. Under native-image,
    // generic signatures are stripped unless the class is explicitly registered — missing these
    // hints would surface as "Unable to resolve the exception type parameter" at startup.
    var hints = new RuntimeHints();
    new RipCurlRuntimeHints().registerHints(hints, getClass().getClassLoader());

    assertTypeHintRegistered(hints, JsonRpcExceptionTranslator.class);
    assertTypeHintRegistered(hints, DefaultJsonRpcExceptionTranslator.class);
    assertTypeHintRegistered(hints, IllegalArgumentExceptionTranslator.class);
    assertTypeHintRegistered(hints, ParameterResolutionExceptionTranslator.class);
  }

  private static void assertTypeHintRegistered(RuntimeHints hints, Class<?> type) {
    assertThat(hints.reflection().typeHints())
        .as("expected binding hints for %s", type.getName())
        .anyMatch(th -> th.getType().equals(TypeReference.of(type)));
  }
}
