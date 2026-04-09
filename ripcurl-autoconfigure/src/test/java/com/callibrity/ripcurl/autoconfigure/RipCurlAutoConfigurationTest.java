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

import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcService;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.autoconfigure.Jackson3AutoConfiguration;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RipCurlAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  JacksonAutoConfiguration.class,
                  Jackson3AutoConfiguration.class,
                  MethodicalAutoConfiguration.class,
                  RipCurlAutoConfiguration.class));

  @JsonRpcService
  public static class TestService {
    @JsonRpcMethod("test.hello")
    public String hello(String name) {
      return "Hello, " + name + "!";
    }
  }

  @Test
  void shouldCreateAllBeans() {
    runner
        .withBean(TestService.class)
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(JsonRpcDispatcher.class);
              assertThat(ctx).hasSingleBean(JsonRpcServiceMethodProvider.class);
              assertThat(ctx).hasSingleBean(AnnotationJsonRpcMethodProviderFactory.class);
              assertThat(ctx).hasBean("defaultJsonRpcMethodProvider");
            });
  }

  @Test
  void dispatcherShouldDiscoverAnnotatedMethods() {
    runner
        .withBean(TestService.class)
        .run(
            ctx -> {
              var dispatcher = ctx.getBean(JsonRpcDispatcher.class);
              assertThat(dispatcher).isNotNull();

              var providers = ctx.getBeansOfType(JsonRpcMethodProvider.class);
              assertThat(providers).isNotEmpty();
            });
  }

  @Test
  void shouldWorkWithNoJsonRpcServices() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(JsonRpcDispatcher.class);
          assertThat(ctx).hasSingleBean(JsonRpcServiceMethodProvider.class);
        });
  }
}
