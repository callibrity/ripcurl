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

/**
 * Extension point for per-handler customization. Every {@link JsonRpcMethodHandlerCustomizer}
 * registered as a Spring bean is invoked once per {@code @JsonRpcMethod} handler during
 * construction with a {@link JsonRpcMethodHandlerConfig} describing that handler. Customizers may
 * attach {@link org.jwcarman.methodical.MethodInterceptor MethodInterceptors} scoped to that
 * specific handler — for example, to bake the handler's method name into a metric tag at
 * construction time instead of re-introspecting the method on every invocation.
 */
@FunctionalInterface
public interface JsonRpcMethodHandlerCustomizer {

  void customize(JsonRpcMethodHandlerConfig config);
}
