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
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandler;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerCustomizer;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlers;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcDispatcher;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.def.ParameterResolutionExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jwcarman.methodical.MethodInvokerFactory;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(after = MethodicalAutoConfiguration.class)
public class RipCurlAutoConfiguration {

  /**
   * Discovers every bean in the context with at least one {@code @JsonRpcMethod} method and builds
   * one {@link JsonRpcMethodHandler} per matching method. Beans are resolved via the
   * allow-eager-init=false variants of the bean-factory API so lazy / prototype / {@code
   * FactoryBean} beans without JSON-RPC methods are never instantiated by this scan. Proxies are
   * unwrapped to find annotations on the declared user class; invocation targets the proxy so
   * Spring AOP advice still applies.
   */
  @Bean
  public List<JsonRpcMethodHandler> jsonRpcMethodHandlers(
      ConfigurableListableBeanFactory beanFactory,
      ObjectMapper mapper,
      MethodInvokerFactory invokerFactory,
      List<JsonRpcMethodHandlerCustomizer> customizers) {
    return Arrays.stream(beanFactory.getBeanNamesForType(Object.class, false, false))
        .filter(name -> hostsJsonRpcMethod(beanFactory, name))
        .flatMap(
            name -> {
              var bean = beanFactory.getBean(name);
              return MethodUtils.getMethodsListWithAnnotation(
                      AopUtils.getTargetClass(bean), JsonRpcMethod.class)
                  .stream()
                  .map(
                      method ->
                          JsonRpcMethodHandlers.build(
                              bean, method, mapper, invokerFactory, customizers));
            })
        .toList();
  }

  private static boolean hostsJsonRpcMethod(
      ConfigurableListableBeanFactory beanFactory, String name) {
    var declaredType = beanFactory.getType(name, false);
    return declaredType != null
        && !MethodUtils.getMethodsListWithAnnotation(
                ClassUtils.getUserClass(declaredType), JsonRpcMethod.class)
            .isEmpty();
  }

  @Bean
  public JsonRpcDispatcher jsonRpcDispatcher(
      List<JsonRpcMethodHandler> handlers, JsonRpcExceptionTranslatorRegistry translators) {
    return new DefaultJsonRpcDispatcher(handlers, translators);
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
}
