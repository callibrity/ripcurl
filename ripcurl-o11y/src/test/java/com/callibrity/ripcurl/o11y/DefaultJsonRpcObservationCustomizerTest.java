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
package com.callibrity.ripcurl.o11y;

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerConfig;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.MethodInterceptor;
import org.jwcarman.methodical.ParameterResolver;
import tools.jackson.databind.JsonNode;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultJsonRpcObservationCustomizerTest {

  private final ObservationRegistry registry = ObservationRegistry.create();
  private final JsonRpcExceptionTranslatorRegistry translators =
      new DefaultJsonRpcExceptionTranslatorRegistry(List.of());
  private final DefaultJsonRpcObservationCustomizer customizer =
      new DefaultJsonRpcObservationCustomizer(registry, translators);

  @Test
  void attaches_a_JsonRpcObservationInterceptor_carrying_the_handler_method_name() {
    var config = new RecordingConfig("tools/list");

    customizer.customize(config);

    assertThat(config.interceptors).hasSize(1);
    assertThat(config.interceptors.getFirst())
        .isInstanceOf(JsonRpcObservationInterceptor.class)
        .hasToString(
            "Records Micrometer 'jsonrpc.server' observations"
                + " (OpenTelemetry JSON-RPC semconv) for method 'tools/list'");
  }

  @Test
  void toString_describes_the_default_observation_it_attaches() {
    assertThat(customizer)
        .hasToString(
            "Attaches the default 'jsonrpc.server' observation"
                + " (OpenTelemetry JSON-RPC semconv) to every @JsonRpcMethod handler");
  }

  /** Plain-Java recording fake — this module's tests avoid mocking frameworks. */
  private static final class RecordingConfig implements JsonRpcMethodHandlerConfig {

    private final String name;
    private final List<MethodInterceptor<? super JsonNode>> interceptors = new ArrayList<>();

    private RecordingConfig(String name) {
      this.name = name;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Method method() {
      return null;
    }

    @Override
    public Object bean() {
      return null;
    }

    @Override
    public JsonRpcMethodHandlerConfig resolver(ParameterResolver<? super JsonNode> resolver) {
      return this;
    }

    @Override
    public JsonRpcMethodHandlerConfig interceptor(MethodInterceptor<? super JsonNode> interceptor) {
      interceptors.add(interceptor);
      return this;
    }
  }
}
