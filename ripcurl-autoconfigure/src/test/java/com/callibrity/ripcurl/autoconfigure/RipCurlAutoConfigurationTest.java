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
import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcParamsResolver;
import com.callibrity.ripcurl.core.annotation.JsonRpcService;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.def.ParameterResolutionExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
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
                  RipCurlResolversAutoConfiguration.class,
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
              assertThat(ctx).hasSingleBean(JsonRpcParamsResolver.class);
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

  @Test
  void shouldRegisterAllBuiltInExceptionTranslatorBeans() {
    // The autoconfigure module exposes each built-in translator as a @ConditionalOnMissingBean
    // so apps can override individually. Verify they all show up by default.
    runner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(DefaultJsonRpcExceptionTranslator.class);
          assertThat(ctx).hasSingleBean(IllegalArgumentExceptionTranslator.class);
          assertThat(ctx).hasSingleBean(ParameterResolutionExceptionTranslator.class);
          assertThat(ctx).hasSingleBean(JsonRpcExceptionTranslatorRegistry.class);
        });
  }

  @Test
  void registryShouldRouteExceptionsThroughRegisteredTranslators() {
    // End-to-end verification that translator beans flow into the registry and the registry
    // then dispatches exception -> JsonRpcErrorDetail correctly. Without this, a broken wiring
    // would pass the existence-check above but still route nothing.
    runner.run(
        ctx -> {
          var registry = ctx.getBean(JsonRpcExceptionTranslatorRegistry.class);
          JsonRpcErrorDetail iae =
              registry.translate(new IllegalArgumentException("age must be >= 0"));
          assertThat(iae.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
          assertThat(iae.message()).isEqualTo("age must be >= 0");

          JsonRpcErrorDetail fallback =
              registry.translate(new RuntimeException("unexpected impl detail"));
          assertThat(fallback.code()).isEqualTo(JsonRpcProtocol.INTERNAL_ERROR);
          assertThat(fallback.message()).isEqualTo("Internal error");
        });
  }

  @Test
  void userCanOverrideAnIndividualBuiltInTranslator() {
    // @ConditionalOnMissingBean on each translator means a user-registered bean of the same
    // type replaces the built-in. Verify that's actually wired correctly — a missing
    // annotation would let both beans register and the registry would reject on duplicate.
    runner
        .withUserConfiguration(OverrideIllegalArgumentConfig.class)
        .run(
            ctx -> {
              assertThat(ctx.getBean(IllegalArgumentExceptionTranslator.class))
                  .isInstanceOf(OverrideIllegalArgumentConfig.AngryIaeTranslator.class);
              JsonRpcErrorDetail detail =
                  ctx.getBean(JsonRpcExceptionTranslatorRegistry.class)
                      .translate(new IllegalArgumentException("no"));
              assertThat(detail.code()).isEqualTo(-29000);
            });
  }

  @Test
  void jakartaValidationTranslatorFlowsIntoTheRegistryWhenAutoConfigIsLoaded() {
    // When both the core autoconfig and the jakarta-validation autoconfig are present, the
    // ConstraintViolationExceptionTranslator bean must be picked up by the registry. Spring
    // processes all autoconfigs before bean instantiation, so order-of-autoconfig shouldn't
    // matter — this test proves that contract instead of relying on it.
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration.class,
                Jackson3AutoConfiguration.class,
                RipCurlResolversAutoConfiguration.class,
                MethodicalAutoConfiguration.class,
                RipCurlAutoConfiguration.class,
                RipCurlJakartaValidationAutoConfiguration.class))
        .run(
            ctx -> {
              var registry = ctx.getBean(JsonRpcExceptionTranslatorRegistry.class);
              var violation =
                  new jakarta.validation.ConstraintViolationException(java.util.Set.of());
              JsonRpcErrorDetail detail = registry.translate(violation);
              // ConstraintViolationExceptionTranslator wins over the built-in fallback — the
              // -32602 code proves the jakarta translator bean actually reached the registry.
              assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
              assertThat(detail.message()).isEqualTo("Invalid params");
            });
  }

  @Test
  void userCanOverrideTheRegistryWholesale() {
    runner
        .withUserConfiguration(OverrideRegistryConfig.class)
        .run(
            ctx ->
                assertThat(ctx.getBean(JsonRpcExceptionTranslatorRegistry.class))
                    .isInstanceOf(OverrideRegistryConfig.EmptyRegistry.class));
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  static class OverrideIllegalArgumentConfig {

    @org.springframework.context.annotation.Bean
    IllegalArgumentExceptionTranslator illegalArgumentExceptionTranslator() {
      return new AngryIaeTranslator();
    }

    static class AngryIaeTranslator extends IllegalArgumentExceptionTranslator {
      @Override
      public JsonRpcErrorDetail translate(IllegalArgumentException exception) {
        return new JsonRpcErrorDetail(-29000, "rejected: " + exception.getMessage());
      }
    }
  }

  @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  static class OverrideRegistryConfig {

    @org.springframework.context.annotation.Bean
    JsonRpcExceptionTranslatorRegistry jsonRpcExceptionTranslatorRegistry() {
      return new EmptyRegistry();
    }

    static class EmptyRegistry implements JsonRpcExceptionTranslatorRegistry {
      @Override
      public JsonRpcErrorDetail translate(Exception exception) {
        return new JsonRpcErrorDetail(-28000, "always this");
      }
    }
  }
}
