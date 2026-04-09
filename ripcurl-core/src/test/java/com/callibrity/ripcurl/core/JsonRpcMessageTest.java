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
package com.callibrity.ripcurl.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JsonRpcMessageTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void shouldParseCall() {
    var body =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","method":"subtract","params":{"a":1},"id":1}
        """);
    var message = JsonRpcMessage.parse(body);
    assertThat(message).isInstanceOf(JsonRpcCall.class);
    var call = (JsonRpcCall) message;
    assertThat(call.method()).isEqualTo("subtract");
    assertThat(call.id()).isNotNull();
  }

  @Test
  void shouldParseNotification() {
    var body =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","method":"update","params":[1,2,3]}
        """);
    var message = JsonRpcMessage.parse(body);
    assertThat(message).isInstanceOf(JsonRpcNotification.class);
    var notification = (JsonRpcNotification) message;
    assertThat(notification.method()).isEqualTo("update");
    assertThat(notification.params()).isNotNull();
  }

  @Test
  void nullIdShouldParseAsCallNotNotification() {
    var body =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","method":"test","id":null}
        """);
    var message = JsonRpcMessage.parse(body);
    assertThat(message).isInstanceOf(JsonRpcCall.class);
    assertThat(((JsonRpcCall) message).id().isNull()).isTrue();
  }

  @Test
  void shouldParseResult() {
    var body =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","result":19,"id":1}
        """);
    var message = JsonRpcMessage.parse(body);
    assertThat(message).isInstanceOf(JsonRpcResult.class);
    var result = (JsonRpcResult) message;
    assertThat(result.result().intValue()).isEqualTo(19);
    assertThat(result.id().intValue()).isEqualTo(1);
  }

  @Test
  void shouldParseError() {
    var body =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","error":{"code":-32601,"message":"Not found"},"id":1}
        """);
    var message = JsonRpcMessage.parse(body);
    assertThat(message).isInstanceOf(JsonRpcError.class);
    var error = (JsonRpcError) message;
    assertThat(error.error().code()).isEqualTo(-32601);
    assertThat(error.id().intValue()).isEqualTo(1);
  }

  @Test
  void shouldThrowForUnrecognizedMessage() {
    var body =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","unknown":"field"}
        """);
    assertThatThrownBy(() -> JsonRpcMessage.parse(body))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void notificationFactorySetsVersion() {
    var notification = JsonRpcNotification.of("test", null);
    assertThat(notification.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(notification.method()).isEqualTo("test");
    assertThat(notification.params()).isNull();
  }

  @Test
  void patternMatchingCoversAllTypes() {
    var call =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","method":"test","id":1}
        """);
    var notification =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","method":"test"}
        """);
    var result =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","result":"ok","id":1}
        """);
    var error =
        MAPPER.readTree(
            """
        {"jsonrpc":"2.0","error":{"code":-1,"message":"bad"},"id":1}
        """);

    assertThat(describe(JsonRpcMessage.parse(call))).isEqualTo("call");
    assertThat(describe(JsonRpcMessage.parse(notification))).isEqualTo("notification");
    assertThat(describe(JsonRpcMessage.parse(result))).isEqualTo("result");
    assertThat(describe(JsonRpcMessage.parse(error))).isEqualTo("error");
  }

  private String describe(JsonRpcMessage message) {
    return switch (message) {
      case JsonRpcRequest request ->
          switch (request) {
            case JsonRpcCall _ -> "call";
            case JsonRpcNotification _ -> "notification";
          };
      case JsonRpcResponse response ->
          switch (response) {
            case JsonRpcResult _ -> "result";
            case JsonRpcError _ -> "error";
          };
    };
  }
}
