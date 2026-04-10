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
package com.callibrity.ripcurl.core.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcResult;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcDispatcher;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.def.DefaultMethodInvokerFactory;
import org.jwcarman.methodical.jackson3.Jackson3ParameterResolver;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

/**
 * Integration test that runs a full dispatch with both {@link JsonRpcParamsResolver} and the
 * default {@link Jackson3ParameterResolver} registered. Verifies that {@code @JsonRpcParams}
 * parameters are resolved as whole-params deserialization, and the default resolver handles
 * individual field resolution for non-annotated parameters.
 */
class JsonRpcParamsIntegrationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public record CreatePersonParams(String name, int age) {}

  public static class PersonService {

    @JsonRpcMethod("create")
    public String create(@JsonRpcParams CreatePersonParams params) {
      return String.format("%s is %d", params.name(), params.age());
    }

    @JsonRpcMethod("fieldByField")
    public String fieldByField(String name, int age) {
      return String.format("%s is %d (fbf)", name, age);
    }
  }

  private final DefaultJsonRpcDispatcher dispatcher =
      new DefaultJsonRpcDispatcher(
          List.of(
              new DefaultAnnotationJsonRpcMethodProviderFactory(
                      MAPPER,
                      new DefaultMethodInvokerFactory(
                          List.of(
                              new JsonRpcParamsResolver(MAPPER),
                              new Jackson3ParameterResolver(MAPPER))))
                  .create(new PersonService())));

  @Test
  void jsonRpcParamsDeserializesWholeParamsObject() {
    var params = MAPPER.createObjectNode().put("name", "Alice").put("age", 30);
    var response =
        dispatcher.dispatch(new JsonRpcCall("2.0", "create", params, StringNode.valueOf("1")));

    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).result().asString()).isEqualTo("Alice is 30");
  }

  @Test
  void defaultResolverStillHandlesFieldByField() {
    var params = MAPPER.createObjectNode().put("name", "Bob").put("age", 25);
    var response =
        dispatcher.dispatch(
            new JsonRpcCall("2.0", "fieldByField", params, StringNode.valueOf("2")));

    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).result().asString()).isEqualTo("Bob is 25 (fbf)");
  }
}
