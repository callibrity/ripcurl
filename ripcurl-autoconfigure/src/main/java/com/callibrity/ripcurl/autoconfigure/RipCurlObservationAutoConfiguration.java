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

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerCustomizer;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.o11y.JsonRpcObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * Wires {@link JsonRpcObservationInterceptor} onto every {@code @JsonRpcMethod} handler via
 * ripcurl's {@link JsonRpcMethodHandlerCustomizer} SPI, producing one Micrometer observation per
 * JSON-RPC dispatch with OpenTelemetry JSON-RPC semantic-convention attributes. Activates when the
 * {@code ripcurl-o11y} module is on the classpath AND an {@link ObservationRegistry} bean exists in
 * the context — Spring Boot auto-creates a registry whenever Actuator or any Micrometer Observation
 * autoconfiguration is present, so this autoconfig lights up automatically when paired with a
 * metrics or tracing stack.
 */
@AutoConfiguration(
    after = RipCurlAutoConfiguration.class,
    afterName =
        "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnClass({JsonRpcObservationInterceptor.class, ObservationRegistry.class})
@ConditionalOnBean({ObservationRegistry.class, JsonRpcExceptionTranslatorRegistry.class})
public class RipCurlObservationAutoConfiguration {

  @Bean
  public JsonRpcMethodHandlerCustomizer jsonRpcObservationCustomizer(
      ObservationRegistry registry, JsonRpcExceptionTranslatorRegistry translators) {
    return config ->
        config.interceptor(new JsonRpcObservationInterceptor(registry, translators, config.name()));
  }
}
