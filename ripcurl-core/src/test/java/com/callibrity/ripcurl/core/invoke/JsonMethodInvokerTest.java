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

import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.lang.reflect.Method;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.StringNode;

class JsonMethodInvokerTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private static final StringNode TEST_ID = StringNode.valueOf("test-id");

  public abstract static class EchoService<T> {
    public T echo(T original) {
      return original;
    }
  }

  public static class StringEchoService extends EchoService<String> {}

  static class DummyService {
    public String echo(String input) {
      return input;
    }

    public String concat(String a, String b) {
      return a + b;
    }

    public void doNothingPrimitive() {
      // Do nothing
    }

    public Void doNothingObject() {
      return null;
    }

    public String evilMethod(String input) {
      throw new IllegalArgumentException(String.format("Invalid input: %s", input));
    }

    public String jsonRpcException(String input) {
      throw new JsonRpcException(JsonRpcException.INVALID_PARAMS, "I don't like you!");
    }

    public JsonRpcResponse rawResponse() {
      return new JsonRpcResponse(StringNode.valueOf("custom"), TEST_ID)
          .withMetadata("key", "value");
    }

    private String hidden(String input) {
      return "nope";
    }
  }

  private JsonRpcRequest request(Object params) {
    return JsonRpcRequest.request(
        "test", params instanceof tools.jackson.databind.JsonNode jn ? jn : null, TEST_ID);
  }

  @Test
  void testInvokeMethodWithTypeParameters() {
    var service = new StringEchoService();
    var method = MethodUtils.getMatchingMethod(StringEchoService.class, "echo", String.class);
    var invoker = new JsonMethodInvoker(mapper, service, method);

    var params = JsonNodeFactory.instance.objectNode();
    params.put("original", "Hello");

    var response = invoker.invoke(request(params));

    assertThat(response.result().isString()).isTrue();
  }

  @Test
  void testInvokeWithMethodThrowingException() throws Exception {
    var service = new DummyService();
    var method = DummyService.class.getDeclaredMethod("evilMethod", String.class);
    var invoker = new JsonMethodInvoker(mapper, service, method);
    var params = JsonNodeFactory.instance.objectNode();
    params.put("input", "Hello");

    var req = request(params);
    assertThatThrownBy(() -> invoker.invoke(req))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INTERNAL_ERROR);
  }

  @Test
  void testInvokeWithNamedParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("concat", String.class, String.class);

    var params = JsonNodeFactory.instance.objectNode();
    params.put("a", "Hello ");
    params.put("b", "World");

    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(request(params));

    assertThat(response.result().asString()).isEqualTo("Hello World");
    assertThat(response.id()).isEqualTo(TEST_ID);
  }

  @Test
  void testInvokeWithInvalidNamedParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("concat", String.class, String.class);

    var params = JsonNodeFactory.instance.objectNode();
    params.putObject("a");
    params.putObject("b");

    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    var req = request(params);
    assertThatThrownBy(() -> invoker.invoke(req))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_PARAMS);
  }

  @Test
  void testInvokeWithPositionalParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("concat", String.class, String.class);

    var params = JsonNodeFactory.instance.arrayNode();
    params.add("Foo");
    params.add("Bar");

    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(request(params));

    assertThat(response.result().asString()).isEqualTo("FooBar");
  }

  @Test
  void testInvokeWithJsonNullMissingParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("echo", String.class);
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response =
        invoker.invoke(
            new JsonRpcRequest(
                "2.0", "test", tools.jackson.databind.node.NullNode.getInstance(), TEST_ID));
    assertThat(response.result().isNull()).isTrue();
  }

  @Test
  void testInvokeWithNullMissingParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("echo", String.class);
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(new JsonRpcRequest("2.0", "test", null, TEST_ID));
    assertThat(response.result().isNull()).isTrue();
  }

  @Test
  void testInvokeWithMissingNamedParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("concat", String.class, String.class);

    var params = JsonNodeFactory.instance.objectNode();
    params.put("a", "Hello ");

    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(request(params));

    assertThat(response.result().asString()).isEqualTo("Hello null");
  }

  @Test
  void testInvokeWithInvalidParametersType() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("echo", String.class);
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    var req = new JsonRpcRequest("2.0", "test", StringNode.valueOf("Hello World"), TEST_ID);
    assertThatThrownBy(() -> invoker.invoke(req))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_PARAMS);
  }

  @Test
  void testInvokeWithTooFewPositionalParameters() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("concat", String.class, String.class);

    var params = JsonNodeFactory.instance.arrayNode();
    params.add("Hello");

    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(request(params));

    assertThat(response.result().asString()).isEqualTo("Hellonull");
  }

  @Test
  void testInvokeWithVoidPrimitiveReturnType() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("doNothingPrimitive");
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(new JsonRpcRequest("2.0", "test", null, TEST_ID));
    assertThat(response.result().isNull()).isTrue();
  }

  @Test
  void testInvokeWithVoidObjectReturnType() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("doNothingObject");
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    JsonRpcResponse response = invoker.invoke(new JsonRpcRequest("2.0", "test", null, TEST_ID));
    assertThat(response.result().isNull()).isTrue();
  }

  @Test
  void methodThrowingJsonRpcExceptionShouldPropagate() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("jsonRpcException", String.class);
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
    var params = JsonNodeFactory.instance.objectNode();
    params.put("input", "Hello");
    var req = request(params);
    assertThatThrownBy(() -> invoker.invoke(req))
        .isExactlyInstanceOf(JsonRpcException.class)
        .hasMessage("I don't like you!");
  }

  @Test
  void testInvokeWithPrivateMethod() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getDeclaredMethod("hidden", String.class);
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);

    var params = JsonNodeFactory.instance.objectNode();
    params.put("input", "test");

    var req = request(params);
    assertThatThrownBy(() -> invoker.invoke(req))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INTERNAL_ERROR);
  }

  @Test
  void handlerReturningJsonRpcResponsePassesThrough() throws Exception {
    DummyService service = new DummyService();
    Method method = DummyService.class.getMethod("rawResponse");
    JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);

    JsonRpcResponse response = invoker.invoke(new JsonRpcRequest("2.0", "test", null, TEST_ID));

    assertThat(response.result()).isEqualTo(StringNode.valueOf("custom"));
    assertThat(response.id()).isEqualTo(TEST_ID);
    assertThat(response.getMetadata("key", String.class)).hasValue("value");
  }
}
