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
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.def.ParameterResolutionExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Registers native-image hints for RipCurl's AOT-sensitive types.
 *
 * <ul>
 *   <li><b>Binding hints</b> for the public JSON-RPC message records — these frequently cross a
 *       codec boundary (e.g. when a caller journals {@link JsonRpcMessage} values through an
 *       external store) without appearing in a {@code @JsonRpcMethod} signature, so {@link
 *       JsonRpcServiceBeanAotProcessor} cannot discover them transitively.
 *   <li><b>Type-parameter resolution hints</b> for the {@link JsonRpcExceptionTranslator} SPI and
 *       each built-in implementation — {@code DefaultJsonRpcExceptionTranslatorRegistry} reads the
 *       generic {@code <E extends Exception>} off the translator class via Specular's {@code
 *       TypeRef} at bean construction. Under native-image, generic signatures are stripped unless
 *       the classes are explicitly registered; without these hints, the registry would throw
 *       "Unable to resolve the exception type parameter" at startup.
 * </ul>
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

    // The SPI interface itself — TypeRef reads its type parameters to drive resolution.
    hints.reflection().registerType(JsonRpcExceptionTranslator.class);
    // Each built-in translator needs its generic interface signature preserved so TypeRef can
    // extract the bound exception type. PUBLIC_CLASSES keeps the class visible; the binding
    // registrar above covers field/method access we don't need here — just the type metadata.
    hints
        .reflection()
        .registerType(DefaultJsonRpcExceptionTranslator.class, MemberCategory.PUBLIC_CLASSES);
    hints
        .reflection()
        .registerType(IllegalArgumentExceptionTranslator.class, MemberCategory.PUBLIC_CLASSES);
    hints
        .reflection()
        .registerType(ParameterResolutionExceptionTranslator.class, MemberCategory.PUBLIC_CLASSES);
  }
}
