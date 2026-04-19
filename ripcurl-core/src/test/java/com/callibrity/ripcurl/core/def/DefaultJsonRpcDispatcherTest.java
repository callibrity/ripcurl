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
package com.callibrity.ripcurl.core.def;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcError;
import com.callibrity.ripcurl.core.JsonRpcNotification;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.JsonRpcResult;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandler;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlers;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.util.List;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.MethodInvokerFactory;
import org.jwcarman.methodical.def.DefaultMethodInvokerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class DefaultJsonRpcDispatcherTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final MethodInvokerFactory INVOKER_FACTORY = new DefaultMethodInvokerFactory();

  public static class HelloService {
    @JsonRpcMethod
    public String sayHello(String name) {
      return String.format("Hello, %s!", name);
    }

    @JsonRpcMethod
    public void fireAndForget(String name) {
      // no-op
    }

    @JsonRpcMethod
    public JsonRpcResult rawResponse(String name) {
      return new JsonRpcResult(StringNode.valueOf("raw:" + name), StringNode.valueOf("custom-id"));
    }

    @JsonRpcMethod
    public String throwsException(String name) {
      throw new JsonRpcException(
          JsonRpcProtocol.INTERNAL_ERROR, "caused", new RuntimeException("root"));
    }

    @JsonRpcMethod
    public String throwsRuntimeException(String name) {
      throw new IllegalStateException("something broke");
    }

    @JsonRpcMethod
    public String throwsIllegalArgument(String name) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }

  private static List<JsonRpcMethodHandler> handlersFor(Object bean) {
    return MethodUtils.getMethodsListWithAnnotation(bean.getClass(), JsonRpcMethod.class).stream()
        .map(m -> JsonRpcMethodHandlers.build(bean, m, MAPPER, INVOKER_FACTORY, List.of()))
        .toList();
  }

  @Test
  void shouldReturnResultForValidCall() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("123");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    var result = (JsonRpcResult) response;
    assertThat(result.result().stringValue()).isEqualTo("Hello, World!");
    assertThat(result.id()).isSameAs(id);
  }

  @Test
  void subsequentCallsShouldWork() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("123");
    var call =
        new JsonRpcCall(
            "2.0", "HelloService.sayHello", MAPPER.createObjectNode().put("name", "World"), id);
    service.dispatch(call);
    var response = service.dispatch(call);
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).result().stringValue()).isEqualTo("Hello, World!");
  }

  @Test
  void shouldAllowNumericIds() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = IntNode.valueOf(123);
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).id()).isSameAs(id);
  }

  @Test
  void shouldReturnNullForNotification() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var response =
        service.dispatch(
            new JsonRpcNotification(
                "2.0", "HelloService.sayHello", MAPPER.createObjectNode().put("name", "World")));
    assertThat(response).isNull();
  }

  @Test
  void nullIdShouldReturnResponseNotNotification() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                NullNode.getInstance()));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(response.id()).isEqualTo(NullNode.getInstance());
  }

  @Test
  void voidMethodShouldReturnNullResult() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("v1");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.fireAndForget",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).result().isNull()).isTrue();
  }

  @Test
  void handlerReturningJsonRpcResultShouldPassThrough() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("r1");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.rawResponse",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    var result = (JsonRpcResult) response;
    assertThat(result.result().asString()).isEqualTo("raw:World");
    assertThat(result.id()).isEqualTo(StringNode.valueOf("custom-id"));
  }

  // --- Error handling ---

  @Test
  void invalidJsonRpcValueShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of());
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.01",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code()).isEqualTo(JsonRpcProtocol.INVALID_REQUEST);
  }

  @Test
  void nullMethodShouldThrow() {
    var params = MAPPER.createObjectNode().put("name", "World");
    var id = StringNode.valueOf("123");
    assertThatThrownBy(() -> new JsonRpcCall("2.0", null, params, id))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void missingMethodNameShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "bogus",
                MAPPER.createObjectNode().put("name", "World"),
                StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcProtocol.METHOD_NOT_FOUND);
  }

  @Test
  void wrongIdTypeShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                MAPPER.createObjectNode()));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code()).isEqualTo(JsonRpcProtocol.INVALID_REQUEST);
  }

  @Test
  void handlerExceptionShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("e1");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.throwsException",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) response;
    assertThat(error.error().code()).isEqualTo(JsonRpcProtocol.INTERNAL_ERROR);
    assertThat(error.error().message()).isEqualTo("caused");
    assertThat(error.id()).isEqualTo(id);
  }

  @Test
  void notificationErrorShouldReturnNull() {
    var service = new DefaultJsonRpcDispatcher(List.of());
    var response = service.dispatch(new JsonRpcNotification("2.01", "bad", null));
    assertThat(response).isNull();
  }

  @Test
  void notificationWithInvalidParamsShouldReturnNull() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var params = MAPPER.createObjectNode();
    params.putObject("name"); // object instead of string → ParameterResolutionException
    var response =
        service.dispatch(new JsonRpcNotification("2.0", "HelloService.sayHello", params));
    assertThat(response).isNull();
  }

  @Test
  void notificationWithRuntimeExceptionShouldReturnNull() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var response =
        service.dispatch(
            new JsonRpcNotification(
                "2.0",
                "HelloService.throwsRuntimeException",
                MAPPER.createObjectNode().put("name", "x")));
    assertThat(response).isNull();
  }

  @Test
  void rpcPrefixShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var response =
        service.dispatch(new JsonRpcCall("2.0", "rpc.discover", null, StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcProtocol.METHOD_NOT_FOUND);
  }

  @Test
  void unexpectedRuntimeExceptionShouldReturnInternalError() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("u1");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.throwsRuntimeException",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) response;
    assertThat(error.error().code()).isEqualTo(JsonRpcProtocol.INTERNAL_ERROR);
    assertThat(error.error().message()).isEqualTo("Internal error");
    assertThat(error.id()).isEqualTo(id);
  }

  @Test
  void invalidParamsShouldReturnInvalidParamsError() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("p1");
    var params = MAPPER.createObjectNode();
    params.putObject("name");
    var response = service.dispatch(new JsonRpcCall("2.0", "HelloService.sayHello", params, id));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) response;
    assertThat(error.error().code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
    assertThat(error.id()).isEqualTo(id);
  }

  @Test
  void handlerThrowingIllegalArgumentExceptionProducesInvalidParams() {
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()));
    var id = StringNode.valueOf("ia1");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.throwsIllegalArgument",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) response;
    assertThat(error.error().code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
    assertThat(error.error().message()).isEqualTo("name must not be blank");
  }

  @Test
  void twoArgConstructorUsesTheSuppliedRegistry() {
    var customRegistry =
        new DefaultJsonRpcExceptionTranslatorRegistry(
            List.of(
                new com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator<Exception>() {
                  @Override
                  public com.callibrity.ripcurl.core.JsonRpcErrorDetail translate(
                      Exception exception) {
                    return new com.callibrity.ripcurl.core.JsonRpcErrorDetail(
                        -42000, "custom-override");
                  }
                }));
    var service = new DefaultJsonRpcDispatcher(handlersFor(new HelloService()), customRegistry);
    var id = StringNode.valueOf("c1");
    var response =
        service.dispatch(
            new JsonRpcCall(
                "2.0",
                "HelloService.throwsRuntimeException",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) response;
    assertThat(error.error().code()).isEqualTo(-42000);
    assertThat(error.error().message()).isEqualTo("custom-override");
  }
}
