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

import com.callibrity.ripcurl.core.annotation.JsonRpcParamsResolver;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.ObjectMapper;

/**
 * Registers RipCurl-provided {@link org.jwcarman.methodical.param.ParameterResolver
 * ParameterResolvers} before Methodical wires up its {@link
 * org.jwcarman.methodical.MethodInvokerFactory}, so they are picked up by the factory.
 */
@AutoConfiguration(before = MethodicalAutoConfiguration.class)
public class RipCurlResolversAutoConfiguration {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public JsonRpcParamsResolver jsonRpcParamsResolver(ObjectMapper mapper) {
    return new JsonRpcParamsResolver(mapper);
  }
}
