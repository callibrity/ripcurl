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
package com.callibrity.ripcurl.core.invoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

class JsonRpcParamResolverTest {

  private final ObjectMapper mapper = new ObjectMapper();

  public static class ServiceWithContext {
    public String greet(String name, StringBuilder context) {
      context.append("called");
      return "Hello, " + name + "!";
    }
  }

  @Test
  void resolverShouldProvideNonJsonParameters() {
    var service = new ServiceWithContext();
    var method =
        MethodUtils.getMatchingMethod(
            ServiceWithContext.class, "greet", String.class, StringBuilder.class);
    var context = new StringBuilder();

    JsonRpcParamResolver contextResolver =
        (parameter, index, params) ->
            StringBuilder.class.equals(parameter.getType()) ? context : null;

    var invoker = new JsonMethodInvoker(mapper, service, method, List.of(contextResolver));
    var params = JsonNodeFactory.instance.objectNode().put("name", "World");
    var result = invoker.invoke(params);

    assertThat(result.asString()).isEqualTo("Hello, World!");
    assertThat(context).hasToString("called");
  }

  public static class SimpleService {
    public String echo(String input) {
      return input;
    }
  }

  @Test
  void resolverReturningNullShouldFallBackToJson() {
    var service = new SimpleService();
    var method = MethodUtils.getMatchingMethod(SimpleService.class, "echo", String.class);

    JsonRpcParamResolver noopResolver = (parameter, index, params) -> null;

    var invoker = new JsonMethodInvoker(mapper, service, method, List.of(noopResolver));
    var params = JsonNodeFactory.instance.objectNode().put("input", "World");
    var result = invoker.invoke(params);

    assertThat(result.asString()).isEqualTo("World");
  }

  @Test
  void firstResolvingResolverWins() {
    var service = new ServiceWithContext();
    var method =
        MethodUtils.getMatchingMethod(
            ServiceWithContext.class, "greet", String.class, StringBuilder.class);
    var first = new StringBuilder("first");
    var second = new StringBuilder("second");

    JsonRpcParamResolver firstResolver =
        (parameter, index, params) ->
            StringBuilder.class.equals(parameter.getType()) ? first : null;
    JsonRpcParamResolver secondResolver =
        (parameter, index, params) ->
            StringBuilder.class.equals(parameter.getType()) ? second : null;

    var invoker =
        new JsonMethodInvoker(mapper, service, method, List.of(firstResolver, secondResolver));
    var params = JsonNodeFactory.instance.objectNode().put("name", "World");
    invoker.invoke(params);

    assertThat(first).hasToString("firstcalled");
    assertThat(second).hasToString("second");
  }
}
