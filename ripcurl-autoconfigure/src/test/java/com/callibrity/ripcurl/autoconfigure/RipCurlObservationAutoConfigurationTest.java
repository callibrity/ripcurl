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

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerCustomizer;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.o11y.JsonRpcObservationInterceptor;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RipCurlObservationAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RipCurlObservationAutoConfiguration.class));

  @Test
  void registers_the_customizer_when_required_beans_are_present() {
    runner
        .withUserConfiguration(RequiredBeansConfig.class)
        .run(
            ctx ->
                assertThat(ctx.getBeansOfType(JsonRpcMethodHandlerCustomizer.class))
                    .containsOnlyKeys("jsonRpcObservationCustomizer"));
  }

  @Test
  void does_not_register_the_customizer_when_no_ObservationRegistry_bean_is_present() {
    runner
        .withUserConfiguration(TranslatorRegistryConfig.class)
        .run(ctx -> assertThat(ctx).doesNotHaveBean(JsonRpcMethodHandlerCustomizer.class));
  }

  @Test
  void
      does_not_register_the_customizer_when_no_JsonRpcExceptionTranslatorRegistry_bean_is_present() {
    runner
        .withUserConfiguration(ObservationRegistryConfig.class)
        .run(ctx -> assertThat(ctx).doesNotHaveBean(JsonRpcMethodHandlerCustomizer.class));
  }

  @Test
  void does_not_activate_when_JsonRpcObservationInterceptor_class_is_absent() {
    runner
        .withClassLoader(new FilteredClassLoader(JsonRpcObservationInterceptor.class))
        .withUserConfiguration(RequiredBeansConfig.class)
        .run(ctx -> assertThat(ctx).doesNotHaveBean(JsonRpcMethodHandlerCustomizer.class));
  }

  @Configuration(proxyBeanMethods = false)
  static class ObservationRegistryConfig {
    @Bean
    ObservationRegistry observationRegistry() {
      return ObservationRegistry.create();
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class TranslatorRegistryConfig {
    @Bean
    JsonRpcExceptionTranslatorRegistry jsonRpcExceptionTranslatorRegistry() {
      return new DefaultJsonRpcExceptionTranslatorRegistry(List.of());
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class RequiredBeansConfig {
    @Bean
    ObservationRegistry observationRegistry() {
      return ObservationRegistry.create();
    }

    @Bean
    JsonRpcExceptionTranslatorRegistry jsonRpcExceptionTranslatorRegistry() {
      return new DefaultJsonRpcExceptionTranslatorRegistry(List.of());
    }
  }
}
