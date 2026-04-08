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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class JsonParamResolverTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonParamResolver resolver = new JsonParamResolver(mapper, TestService.class);

  public static class TestService {
    public String echo(String input) {
      return input;
    }

    public String concat(String a, String b) {
      return a + b;
    }
  }

  private Parameter paramOf(String methodName, int index) throws Exception {
    for (var method : TestService.class.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method.getParameters()[index];
      }
    }
    throw new IllegalArgumentException("Method not found: " + methodName);
  }

  @Test
  void resolveWithNullParamsReturnsNull() throws Exception {
    var param = paramOf("echo", 0);
    assertThat(resolver.resolve(param, 0, null)).isNull();
  }

  @Test
  void resolveWithNullNodeParamsReturnsNull() throws Exception {
    var param = paramOf("echo", 0);
    assertThat(resolver.resolve(param, 0, NullNode.getInstance())).isNull();
  }

  @Test
  void resolveWithObjectParamsByName() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.objectNode().put("input", "hello");
    assertThat(resolver.resolve(param, 0, params)).isEqualTo("hello");
  }

  @Test
  void resolveWithObjectParamsMissingKeyReturnsNull() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.objectNode().put("other", "value");
    assertThat(resolver.resolve(param, 0, params)).isNull();
  }

  @Test
  void resolveWithObjectParamsNullValueReturnsNull() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.objectNode();
    params.set("input", NullNode.getInstance());
    assertThat(resolver.resolve(param, 0, params)).isNull();
  }

  @Test
  void resolveWithArrayParamsByPosition() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.arrayNode().add("hello");
    assertThat(resolver.resolve(param, 0, params)).isEqualTo("hello");
  }

  @Test
  void resolveWithArrayParamsOutOfBoundsReturnsNull() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.arrayNode();
    assertThat(resolver.resolve(param, 1, params)).isNull();
  }

  @Test
  void resolveWithArrayParamsNullElementReturnsNull() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.arrayNode();
    params.add(NullNode.getInstance());
    assertThat(resolver.resolve(param, 0, params)).isNull();
  }

  @Test
  void resolveWithUnsupportedParamsTypeThrows() throws Exception {
    var param = paramOf("echo", 0);
    var params = StringNode.valueOf("not an object or array");
    assertThatThrownBy(() -> resolver.resolve(param, 0, params))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_PARAMS);
  }

  @Test
  void resolveWithInvalidTypeThrows() throws Exception {
    var param = paramOf("echo", 0);
    var params = JsonNodeFactory.instance.objectNode();
    params.putObject("input");
    assertThatThrownBy(() -> resolver.resolve(param, 0, params))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_PARAMS);
  }
}
