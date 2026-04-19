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

import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import java.util.List;
import org.jwcarman.methodical.MethodInvokerFactory;
import org.jwcarman.methodical.intercept.MethodInterceptor;
import org.jwcarman.methodical.param.ParameterResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class DefaultAnnotationJsonRpcMethodProviderFactory
    implements AnnotationJsonRpcMethodProviderFactory {

  // ------------------------------ FIELDS ------------------------------

  private final ObjectMapper mapper;
  private final MethodInvokerFactory invokerFactory;
  private final List<ParameterResolver<? super JsonNode>> resolvers;
  private final List<MethodInterceptor<? super JsonNode>> interceptors;

  // --------------------------- CONSTRUCTORS ---------------------------

  public DefaultAnnotationJsonRpcMethodProviderFactory(
      ObjectMapper mapper,
      MethodInvokerFactory invokerFactory,
      List<ParameterResolver<? super JsonNode>> resolvers) {
    this(mapper, invokerFactory, resolvers, List.of());
  }

  public DefaultAnnotationJsonRpcMethodProviderFactory(
      ObjectMapper mapper,
      MethodInvokerFactory invokerFactory,
      List<ParameterResolver<? super JsonNode>> resolvers,
      List<MethodInterceptor<? super JsonNode>> interceptors) {
    this.mapper = mapper;
    this.invokerFactory = invokerFactory;
    this.resolvers = List.copyOf(resolvers);
    this.interceptors = List.copyOf(interceptors);
  }

  // ------------------------ INTERFACE METHODS ------------------------

  // --------------------- Interface AnnotationJsonRpcMethodHandlerProviderFactory
  // ---------------------

  @Override
  public JsonRpcMethodProvider create(Object targetObject) {
    var methods =
        AnnotationJsonRpcMethod.createMethods(
            mapper, targetObject, invokerFactory, resolvers, interceptors);
    return () -> List.copyOf(methods);
  }
}
