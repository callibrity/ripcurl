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
package com.callibrity.ripcurl.core.annotation;

import java.lang.reflect.Method;
import org.jwcarman.methodical.MethodInterceptor;
import org.jwcarman.methodical.ParameterResolver;
import tools.jackson.databind.JsonNode;

/**
 * Per-handler configuration view passed to each {@link JsonRpcMethodHandlerCustomizer} while a
 * {@link JsonRpcMethodHandler} is being built. Customizers may read the JSON-RPC method name,
 * target method, and target bean, and append {@link ParameterResolver}s or {@link
 * MethodInterceptor}s to the handler's invocation chain.
 *
 * <p>Customizer-added resolvers are slotted between RipCurl's two built-in resolvers: {@code
 * JsonRpcParamsResolver} (for {@code @JsonRpcParams}-annotated parameters) always runs first, then
 * customizer-added resolvers in the order they were added, then {@code Jackson3ParameterResolver}
 * (name/index binding) as the catch-all. Methodical's {@code @Argument} tail resolver still runs
 * last.
 */
public interface JsonRpcMethodHandlerConfig {

  String name();

  Method method();

  Object bean();

  JsonRpcMethodHandlerConfig resolver(ParameterResolver<? super JsonNode> resolver);

  JsonRpcMethodHandlerConfig interceptor(MethodInterceptor<? super JsonNode> interceptor);
}
