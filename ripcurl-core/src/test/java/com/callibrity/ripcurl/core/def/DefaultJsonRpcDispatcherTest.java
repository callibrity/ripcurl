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

import com.callibrity.ripcurl.core.JsonRpcError;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResult;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.def.DefaultMethodInvokerFactory;
import org.jwcarman.methodical.jackson3.Jackson3ParameterResolver;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class DefaultJsonRpcDispatcherTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final AnnotationJsonRpcMethodProviderFactory factory =
      new DefaultAnnotationJsonRpcMethodProviderFactory(
          MAPPER, new DefaultMethodInvokerFactory(List.of(new Jackson3ParameterResolver(MAPPER))));

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
      return new JsonRpcResult(StringNode.valueOf("raw:" + name), StringNode.valueOf("custom-id"))
          .withMetadata("test", true);
    }

    @JsonRpcMethod
    public String throwsException(String name) {
      throw new JsonRpcException(
          JsonRpcException.INTERNAL_ERROR, "caused", new RuntimeException("root"));
    }
  }

  @Test
  void shouldReturnResultForValidRequest() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var id = StringNode.valueOf("123");
    var response =
        service.dispatch(
            new JsonRpcRequest(
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
  void subsequentRequestsShouldWork() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var id = StringNode.valueOf("123");
    var request =
        new JsonRpcRequest(
            "2.0", "HelloService.sayHello", MAPPER.createObjectNode().put("name", "World"), id);
    service.dispatch(request);
    var response = service.dispatch(request);
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).result().stringValue()).isEqualTo("Hello, World!");
  }

  @Test
  void shouldAllowNumericIds() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var id = IntNode.valueOf(123);
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).id()).isSameAs(id);
  }

  @Test
  void shouldReturnNullForNotification() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                null));
    assertThat(response).isNull();
  }

  @Test
  void nullIdShouldReturnResponseNotNotification() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                NullNode.getInstance()));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
  }

  @Test
  void voidMethodShouldReturnNullResult() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var id = StringNode.valueOf("v1");
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.fireAndForget",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(((JsonRpcResult) response).result().isNull()).isTrue();
  }

  @Test
  void handlerReturningJsonRpcResultShouldPassThrough() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var id = StringNode.valueOf("r1");
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.rawResponse",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    var result = (JsonRpcResult) response;
    assertThat(result.result().asString()).isEqualTo("raw:World");
    assertThat(result.id()).isEqualTo(StringNode.valueOf("custom-id"));
    assertThat(result.getMetadata("test", Boolean.class)).hasValue(true);
  }

  // --- Error handling ---

  @Test
  void invalidJsonRpcValueShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of());
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.01",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcException.INVALID_REQUEST);
  }

  @Test
  void missingMethodShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of());
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                null,
                MAPPER.createObjectNode().put("name", "World"),
                StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcException.INVALID_REQUEST);
  }

  @Test
  void missingMethodNameShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "bogus",
                MAPPER.createObjectNode().put("name", "World"),
                StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcException.METHOD_NOT_FOUND);
  }

  @Test
  void wrongIdTypeShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                MAPPER.createObjectNode().put("name", "World"),
                MAPPER.createObjectNode()));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcException.INVALID_REQUEST);
  }

  @Test
  void handlerExceptionShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var id = StringNode.valueOf("e1");
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.throwsException",
                MAPPER.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) response;
    assertThat(error.error().code()).isEqualTo(JsonRpcException.INTERNAL_ERROR);
    assertThat(error.error().message()).isEqualTo("caused");
    assertThat(error.id()).isEqualTo(id);
  }

  @Test
  void notificationErrorShouldReturnNull() {
    var service = new DefaultJsonRpcDispatcher(List.of());
    var response = service.dispatch(new JsonRpcRequest("2.01", "bad", null, null));
    assertThat(response).isNull();
  }

  @Test
  void rpcPrefixShouldReturnError() {
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var response =
        service.dispatch(
            new JsonRpcRequest("2.0", "rpc.discover", null, StringNode.valueOf("123")));
    assertThat(response).isInstanceOf(JsonRpcError.class);
    assertThat(((JsonRpcError) response).error().code())
        .isEqualTo(JsonRpcException.METHOD_NOT_FOUND);
  }
}
