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

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.JsonRpcResult;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandler;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.def.ParameterResolutionExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.autoconfigure.Jackson3AutoConfiguration;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

class RipCurlAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  JacksonAutoConfiguration.class,
                  Jackson3AutoConfiguration.class,
                  MethodicalAutoConfiguration.class,
                  RipCurlAutoConfiguration.class));

  public static class TestService {
    @JsonRpcMethod("test.hello")
    public String hello(String name) {
      return "Hello, " + name + "!";
    }
  }

  @Test
  void shouldCreateCoreBeans() {
    runner
        .withBean(TestService.class)
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(JsonRpcDispatcher.class);
              assertThat(ctx.getBean("jsonRpcMethodHandlers"))
                  .asInstanceOf(
                      org.assertj.core.api.InstanceOfAssertFactories.list(
                          JsonRpcMethodHandler.class))
                  .hasSize(1);
            });
  }

  @Test
  void dispatcherShouldDiscoverAnnotatedMethodsOnPlainBeans() {
    runner
        .withBean(TestService.class)
        .run(
            ctx -> {
              var dispatcher = ctx.getBean(JsonRpcDispatcher.class);
              var mapper = ctx.getBean(ObjectMapper.class);
              var params = mapper.createObjectNode().put("name", "World");
              var response =
                  dispatcher.dispatch(
                      new JsonRpcCall("2.0", "test.hello", params, StringNode.valueOf("1")));
              assertThat(response).isInstanceOf(JsonRpcResult.class);
              assertThat(((JsonRpcResult) response).result().asString()).isEqualTo("Hello, World!");
            });
  }

  @Test
  void shouldWorkWithNoJsonRpcMethodBeans() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasSingleBean(JsonRpcDispatcher.class);
          assertThat(ctx.getBean("jsonRpcMethodHandlers"))
              .asInstanceOf(
                  org.assertj.core.api.InstanceOfAssertFactories.list(JsonRpcMethodHandler.class))
              .isEmpty();
        });
  }

  @Test
  void handlerCustomizerRunsOncePerHandlerWithCorrectMetadata() {
    // Registers a customizer that captures the config handed to it. Asserts the name/method/bean
    // triple reflects the TestService's single handler. This is the only place the customizer SPI
    // is exercised through the full Spring wiring.
    runner
        .withBean(TestService.class)
        .withUserConfiguration(CapturingCustomizerConfig.class)
        .run(
            ctx -> {
              var captor = ctx.getBean(CapturingCustomizerConfig.Captor.class);
              assertThat(captor.names).containsExactly("test.hello");
              assertThat(captor.beans).singleElement().isInstanceOf(TestService.class);
              assertThat(captor.methods)
                  .singleElement()
                  .satisfies(m -> assertThat(m.getName()).isEqualTo("hello"));
            });
  }

  @Test
  void shouldRegisterAllBuiltInExceptionTranslatorBeans() {
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
    new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration.class,
                Jackson3AutoConfiguration.class,
                MethodicalAutoConfiguration.class,
                RipCurlAutoConfiguration.class,
                RipCurlJakartaValidationAutoConfiguration.class))
        .run(
            ctx -> {
              var registry = ctx.getBean(JsonRpcExceptionTranslatorRegistry.class);
              var violation =
                  new jakarta.validation.ConstraintViolationException(java.util.Set.of());
              JsonRpcErrorDetail detail = registry.translate(violation);
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
  static class CapturingCustomizerConfig {

    @org.springframework.context.annotation.Bean
    Captor captor() {
      return new Captor();
    }

    @org.springframework.context.annotation.Bean
    com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerCustomizer capturingCustomizer(
        ObjectProvider<Captor> captorProvider) {
      return config -> {
        var captor = captorProvider.getObject();
        captor.names.add(config.name());
        captor.methods.add(config.method());
        captor.beans.add(config.bean());
      };
    }

    static class Captor {
      final java.util.List<String> names = new java.util.ArrayList<>();
      final java.util.List<java.lang.reflect.Method> methods = new java.util.ArrayList<>();
      final java.util.List<Object> beans = new java.util.ArrayList<>();
    }
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
