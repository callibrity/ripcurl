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

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcError;
import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcMessage;
import com.callibrity.ripcurl.core.JsonRpcNotification;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.JsonRpcResult;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers binding hints for RipCurl's public JSON-RPC message types. These types frequently cross
 * a codec boundary (e.g., when a caller journals {@link JsonRpcMessage} values through an external
 * store) without appearing in a {@code @JsonRpcMethod} signature, so {@link
 * JsonRpcServiceBeanAotProcessor} cannot discover them transitively. The types are registered
 * explicitly so binaries compiled with GraalVM {@code native-image} can serialize and deserialize
 * them.
 */
public class RipCurlRuntimeHints implements RuntimeHintsRegistrar {

  private static final BindingReflectionHintsRegistrar BINDING =
      new BindingReflectionHintsRegistrar();

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcMessage.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcRequest.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcResponse.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcCall.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcNotification.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcResult.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcError.class);
    BINDING.registerReflectionHints(hints.reflection(), JsonRpcErrorDetail.class);
  }
}
