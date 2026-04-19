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
package com.callibrity.ripcurl.autoconfigure;

import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcDispatcher;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.def.ParameterResolutionExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import java.util.List;
import org.jwcarman.methodical.MethodInvokerFactory;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.jwcarman.methodical.intercept.MethodInterceptor;
import org.jwcarman.methodical.param.ParameterResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = MethodicalAutoConfiguration.class)
public class RipCurlAutoConfiguration {

  // -------------------------- OTHER METHODS --------------------------

  @Bean
  public JsonRpcServiceMethodProvider jsonRpcServiceMethodProvider(
      ApplicationContext ctx,
      ObjectMapper mapper,
      MethodInvokerFactory invokerFactory,
      List<ParameterResolver<? super JsonNode>> resolvers,
      List<MethodInterceptor<? super JsonNode>> interceptors) {
    return new JsonRpcServiceMethodProvider(ctx, mapper, invokerFactory, resolvers, interceptors);
  }

  @Bean
  public JsonRpcMethodProvider defaultJsonRpcMethodProvider(List<JsonRpcMethod> methods) {
    return () -> methods;
  }

  @Bean
  public JsonRpcDispatcher jsonRpcDispatcher(
      List<JsonRpcMethodProvider> providers, JsonRpcExceptionTranslatorRegistry translators) {
    return new DefaultJsonRpcDispatcher(providers, translators);
  }

  @Bean
  @ConditionalOnMissingBean
  public JsonRpcExceptionTranslatorRegistry jsonRpcExceptionTranslatorRegistry(
      List<JsonRpcExceptionTranslator<?>> translators) {
    return new DefaultJsonRpcExceptionTranslatorRegistry(translators);
  }

  @Bean
  @ConditionalOnMissingBean
  public DefaultJsonRpcExceptionTranslator defaultJsonRpcExceptionTranslator() {
    return new DefaultJsonRpcExceptionTranslator();
  }

  @Bean
  @ConditionalOnMissingBean
  public IllegalArgumentExceptionTranslator illegalArgumentExceptionTranslator() {
    return new IllegalArgumentExceptionTranslator();
  }

  @Bean
  @ConditionalOnMissingBean
  public ParameterResolutionExceptionTranslator parameterResolutionExceptionTranslator() {
    return new ParameterResolutionExceptionTranslator();
  }

  @Bean
  public AnnotationJsonRpcMethodProviderFactory annotationJsonRpcMethodHandlerProviderFactory(
      ObjectMapper objectMapper,
      MethodInvokerFactory invokerFactory,
      List<ParameterResolver<? super JsonNode>> resolvers,
      List<MethodInterceptor<? super JsonNode>> interceptors) {
    return new DefaultAnnotationJsonRpcMethodProviderFactory(
        objectMapper, invokerFactory, resolvers, interceptors);
  }
}
