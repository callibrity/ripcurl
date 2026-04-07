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

import com.callibrity.ripcurl.core.annotation.JsonRpc;
import com.callibrity.ripcurl.core.annotation.JsonRpcService;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

class JsonRpcServiceMethodProviderTest {

  @JsonRpcService
  public static class TestService {
    @JsonRpc("test.hello")
    public String hello(String name) {
      return "Hello, " + name + "!";
    }

    @JsonRpc("test.ping")
    public String ping() {
      return "pong";
    }
  }

  @Configuration
  static class TestConfig {
    @Bean
    TestService testService() {
      return new TestService();
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }

  @Test
  void shouldDiscoverJsonRpcServiceMethods() {
    try (var ctx = new AnnotationConfigApplicationContext(TestConfig.class)) {
      var provider =
          new JsonRpcServiceMethodProvider(ctx, ctx.getBean(ObjectMapper.class), List.of());
      provider.initialize();

      List<JsonRpcMethod> methods = provider.getJsonRpcMethodHandlers();
      assertThat(methods).hasSize(2);
      assertThat(methods)
          .extracting(JsonRpcMethod::methodName)
          .containsExactlyInAnyOrder("test.hello", "test.ping");
    }
  }

  @Test
  void shouldReturnEmptyWhenNoServicesPresent() {
    try (var ctx = new AnnotationConfigApplicationContext()) {
      ctx.register(EmptyConfig.class);
      ctx.refresh();

      var provider = new JsonRpcServiceMethodProvider(ctx, new ObjectMapper(), List.of());
      provider.initialize();

      assertThat(provider.getJsonRpcMethodHandlers()).isEmpty();
    }
  }

  @Configuration
  static class EmptyConfig {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }
  }
}
