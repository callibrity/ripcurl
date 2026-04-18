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

import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import com.callibrity.ripcurl.jakarta.ConstraintViolationExceptionTranslator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RipCurlJakartaValidationAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(RipCurlJakartaValidationAutoConfiguration.class));

  @Test
  void registers_the_translator_as_a_JsonRpcExceptionTranslator_bean() {
    // Spring only picks up the translator when it's discovered as a JsonRpcExceptionTranslator<?>
    // bean — otherwise the registry in ripcurl-autoconfigure won't find it. The upcast matters.
    runner.run(
        context ->
            assertThat(context)
                .hasSingleBean(ConstraintViolationExceptionTranslator.class)
                .getBeans(JsonRpcExceptionTranslator.class)
                .hasSize(1));
  }

  @Test
  void conditional_on_missing_bean_allows_application_override() {
    runner
        .withUserConfiguration(UserOverrideConfig.class)
        .run(
            context ->
                assertThat(context.getBean(ConstraintViolationExceptionTranslator.class))
                    .isInstanceOf(UserOverrideConfig.OverridingTranslator.class));
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  static class UserOverrideConfig {

    @org.springframework.context.annotation.Bean
    ConstraintViolationExceptionTranslator constraintViolationExceptionTranslator() {
      return new OverridingTranslator();
    }

    static class OverridingTranslator extends ConstraintViolationExceptionTranslator {}
  }
}
