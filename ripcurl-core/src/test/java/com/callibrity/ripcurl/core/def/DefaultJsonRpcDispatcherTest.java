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

import com.callibrity.ripcurl.core.JsonRpcRequest;
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
  }

  @Test
  void shouldReturnResponseForValidRequest() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));

    var id = StringNode.valueOf("123");
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                mapper.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isNotNull();
    assertThat(response.id()).isSameAs(id);
    assertThat(response.result().stringValue()).isEqualTo("Hello, World!");
  }

  @Test
  void subsequentRequestsShouldReturnPreviouslyCalculatedMethods() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));

    var id = StringNode.valueOf("123");
    var request =
        new JsonRpcRequest(
            "2.0", "HelloService.sayHello", mapper.createObjectNode().put("name", "World"), id);
    service.dispatch(request);
    var response = service.dispatch(request);
    assertThat(response).isNotNull();
    assertThat(response.id()).isSameAs(id);
    assertThat(response.result().stringValue()).isEqualTo("Hello, World!");
  }

  @Test
  void missingMethodNameShouldThrowException() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));

    var id = StringNode.valueOf("123");
    var request =
        new JsonRpcRequest("2.0", "bogus", mapper.createObjectNode().put("name", "World"), id);
    assertThatThrownBy(() -> service.dispatch(request))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.METHOD_NOT_FOUND);
  }

  @Test
  void shouldAllowNumericIds() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));

    var id = IntNode.valueOf(123);
    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                mapper.createObjectNode().put("name", "World"),
                id));
    assertThat(response).isNotNull();
    assertThat(response.id()).isSameAs(id);
    assertThat(response.result().stringValue()).isEqualTo("Hello, World!");
  }

  @Test
  void shouldReturnNullForNotificationRequest() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));

    var response =
        service.dispatch(
            new JsonRpcRequest(
                "2.0",
                "HelloService.sayHello",
                mapper.createObjectNode().put("name", "World"),
                null));
    assertThat(response).isNull();
  }

  @Test
  void invalidJsonRpcValueShouldThrowException() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of());
    var request =
        new JsonRpcRequest(
            "2.01",
            "HelloService.sayHello",
            mapper.createObjectNode().put("name", "World"),
            StringNode.valueOf("123"));
    assertThatThrownBy(() -> service.dispatch(request))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_REQUEST);
  }

  @Test
  void missingMethodShouldThrowException() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of());
    var request =
        new JsonRpcRequest(
            "2.0", null, mapper.createObjectNode().put("name", "World"), StringNode.valueOf("123"));
    assertThatThrownBy(() -> service.dispatch(request))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_REQUEST);
  }

  @Test
  void wrongIdTypeShouldThrowException() {
    ObjectMapper mapper = new ObjectMapper();
    var service = new DefaultJsonRpcDispatcher(List.of(factory.create(new HelloService())));
    var request =
        new JsonRpcRequest(
            "2.0",
            "HelloService.sayHello",
            mapper.createObjectNode().put("name", "World"),
            mapper.createObjectNode());
    assertThatThrownBy(() -> service.dispatch(request))
        .isExactlyInstanceOf(JsonRpcException.class)
        .extracting("code")
        .isEqualTo(JsonRpcException.INVALID_REQUEST);
  }
}
