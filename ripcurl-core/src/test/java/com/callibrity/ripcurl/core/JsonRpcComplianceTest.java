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

import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcDispatcher;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.def.DefaultMethodInvokerFactory;
import org.jwcarman.methodical.jackson3.Jackson3ParameterResolver;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

/**
 * Formal compliance tests against the JSON-RPC 2.0 specification.
 *
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
class JsonRpcComplianceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private JsonRpcDispatcher dispatcher;

  public static class TestService {
    @JsonRpcMethod("subtract")
    public int subtract(int minuend, int subtrahend) {
      return minuend - subtrahend;
    }

    @JsonRpcMethod("update")
    public void update(int a, int b, int c) {
      // notification target
    }

    @JsonRpcMethod("foobar")
    public String foobar() {
      return "foobar";
    }
  }

  @BeforeEach
  void setUp() {
    var factory =
        new DefaultAnnotationJsonRpcMethodProviderFactory(
            MAPPER,
            new DefaultMethodInvokerFactory(List.of(new Jackson3ParameterResolver(MAPPER))));
    dispatcher = new DefaultJsonRpcDispatcher(List.of(factory.create(new TestService())));
  }

  // --- Protocol constants ---

  @Nested
  class ProtocolConstants {

    @Test
    void versionIs2point0() {
      assertThat(JsonRpcProtocol.VERSION).isEqualTo("2.0");
    }

    @Test
    void parseErrorCode() {
      assertThat(JsonRpcProtocol.PARSE_ERROR).isEqualTo(-32700);
    }

    @Test
    void invalidRequestCode() {
      assertThat(JsonRpcProtocol.INVALID_REQUEST).isEqualTo(-32600);
    }

    @Test
    void methodNotFoundCode() {
      assertThat(JsonRpcProtocol.METHOD_NOT_FOUND).isEqualTo(-32601);
    }

    @Test
    void invalidParamsCode() {
      assertThat(JsonRpcProtocol.INVALID_PARAMS).isEqualTo(-32602);
    }

    @Test
    void internalErrorCode() {
      assertThat(JsonRpcProtocol.INTERNAL_ERROR).isEqualTo(-32603);
    }
  }

  // --- Section 4.1: Request object ---

  @Nested
  class RequestObject {

    @Test
    void jsonrpcMustBeExactly2point0() {
      var response =
          dispatcher.dispatch(
              new JsonRpcRequest("2.1", "subtract", MAPPER.createObjectNode(), intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
      assertThat(((JsonRpcError) response).error().code())
          .isEqualTo(JsonRpcProtocol.INVALID_REQUEST);
    }

    @Test
    void methodMustBePresent() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", null, null, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
      assertThat(((JsonRpcError) response).error().code())
          .isEqualTo(JsonRpcProtocol.INVALID_REQUEST);
    }

    @Test
    void methodMustNotBeBlank() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "  ", null, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
      assertThat(((JsonRpcError) response).error().code())
          .isEqualTo(JsonRpcProtocol.INVALID_REQUEST);
    }

    @Test
    void rpcPrefixMethodsMustBeRejected() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "rpc.discover", null, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
      assertThat(((JsonRpcError) response).error().code())
          .isEqualTo(JsonRpcProtocol.METHOD_NOT_FOUND);
    }

    @Test
    void idMayBeString() {
      var response =
          dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, StringNode.valueOf("abc")));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
      assertThat(((JsonRpcResult) response).id()).isEqualTo(StringNode.valueOf("abc"));
    }

    @Test
    void idMayBeNumber() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, intId(42)));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
      assertThat(((JsonRpcResult) response).id()).isEqualTo(intId(42));
    }

    @Test
    void idMayBeNull() {
      // "The value SHOULD normally not be Null" but it's valid — treated as request, not
      // notification
      var response =
          dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, NullNode.getInstance()));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
      assertThat(((JsonRpcResult) response).id()).isEqualTo(NullNode.getInstance());
    }

    @Test
    void idMustNotBeObject() {
      var response =
          dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, MAPPER.createObjectNode()));
      assertThat(response).isInstanceOf(JsonRpcError.class);
    }

    @Test
    void idMustNotBeArray() {
      var response =
          dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, MAPPER.createArrayNode()));
      assertThat(response).isInstanceOf(JsonRpcError.class);
    }

    @Test
    void idMustNotBeBoolean() {
      var response =
          dispatcher.dispatch(
              new JsonRpcRequest("2.0", "foobar", null, MAPPER.getNodeFactory().booleanNode(true)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
    }
  }

  // --- Section 4.1.1: Parameter Structures ---

  @Nested
  class ParameterStructures {

    @Test
    void byNameParams() {
      var params = MAPPER.createObjectNode().put("minuend", 42).put("subtrahend", 23);
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "subtract", params, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
      assertThat(((JsonRpcResult) response).result().intValue()).isEqualTo(19);
    }

    @Test
    void byPositionParams() {
      var params = MAPPER.createArrayNode().add(42).add(23);
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "subtract", params, intId(2)));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
      assertThat(((JsonRpcResult) response).result().intValue()).isEqualTo(19);
    }

    @Test
    void paramsAreOptional() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, intId(3)));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
    }
  }

  // --- Section 4.2: Response object ---

  @Nested
  class ResponseObject {

    @Test
    void successResponseHasJsonrpcResultAndId() {
      var response =
          dispatcher.dispatch(
              new JsonRpcRequest(
                  "2.0",
                  "subtract",
                  MAPPER.createObjectNode().put("minuend", 10).put("subtrahend", 3),
                  intId(1)));
      assertThat(response).isInstanceOf(JsonRpcResult.class);
      var result = (JsonRpcResult) response;
      assertThat(result.jsonrpc()).isEqualTo("2.0");
      assertThat(result.result()).isNotNull();
      assertThat(result.id()).isEqualTo(intId(1));
    }

    @Test
    void successResponseSerializesWithoutErrorField() {
      var result = new JsonRpcResult(StringNode.valueOf("ok"), intId(1));
      var json = MAPPER.writeValueAsString(result);
      assertThat(json).contains("\"result\"").contains("\"id\"").doesNotContain("\"error\"");
    }

    @Test
    void errorResponseSerializesWithoutResultField() {
      var error = new JsonRpcError(-32601, "Method not found", intId(1));
      var json = MAPPER.writeValueAsString(error);
      assertThat(json).contains("\"error\"").contains("\"id\"").doesNotContain("\"result\"");
    }

    @Test
    void errorResponseHasJsonrpcErrorAndId() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "nonexistent", null, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
      var error = (JsonRpcError) response;
      assertThat(error.jsonrpc()).isEqualTo("2.0");
      assertThat(error.error()).isNotNull();
      assertThat(error.error().code()).isEqualTo(JsonRpcProtocol.METHOD_NOT_FOUND);
      assertThat(error.error().message()).isNotBlank();
      assertThat(error.id()).isEqualTo(intId(1));
    }

    @Test
    void responseIdMustMatchRequestId() {
      var id = intId(99);
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "foobar", null, id));
      assertThat(response.id()).isSameAs(id);
    }
  }

  // --- Section 4.2.2: Error Object ---

  @Nested
  class ErrorObject {

    @Test
    void errorHasCodeAndMessage() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "nonexistent", null, intId(1)));
      var error = (JsonRpcError) response;
      assertThat(error.error().code()).isNotZero();
      assertThat(error.error().message()).isNotNull();
    }

    @Test
    void errorDataFieldIsOptional() {
      var detail = new JsonRpcErrorDetail(-32603, "Internal error");
      assertThat(detail.data()).isNull();

      var detailWithData =
          new JsonRpcErrorDetail(-32603, "Internal error", StringNode.valueOf("extra"));
      assertThat(detailWithData.data()).isEqualTo(StringNode.valueOf("extra"));
    }

    @Test
    void invalidRequestErrorCode() {
      var response = dispatcher.dispatch(new JsonRpcRequest("1.0", "subtract", null, intId(1)));
      assertThat(((JsonRpcError) response).error().code()).isEqualTo(-32600);
    }

    @Test
    void methodNotFoundErrorCode() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "nonexistent", null, intId(1)));
      assertThat(((JsonRpcError) response).error().code()).isEqualTo(-32601);
    }

    @Test
    void invalidParamsErrorCode() {
      var params = MAPPER.createObjectNode();
      params.putObject("minuend"); // object instead of int
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "subtract", params, intId(1)));
      assertThat(((JsonRpcError) response).error().code()).isEqualTo(-32602);
    }
  }

  // --- Section 4.1: Notification ---

  @Nested
  class Notifications {

    @Test
    void notificationHasNoId() {
      var response =
          dispatcher.dispatch(
              new JsonRpcRequest(
                  "2.0", "update", MAPPER.createArrayNode().add(1).add(2).add(3), null));
      assertThat(response).isNull();
    }

    @Test
    void notificationErrorReturnsNull() {
      // Even if the method doesn't exist, notification gets no response
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "nonexistent", null, null));
      assertThat(response).isNull();
    }

    @Test
    void notificationValidationErrorReturnsNull() {
      // Bad jsonrpc version on a notification still returns null
      var response = dispatcher.dispatch(new JsonRpcRequest("1.0", "update", null, null));
      assertThat(response).isNull();
    }
  }

  // --- Case sensitivity ---

  @Nested
  class CaseSensitivity {

    @Test
    void methodNamesAreCaseSensitive() {
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "Subtract", null, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
      assertThat(((JsonRpcError) response).error().code())
          .isEqualTo(JsonRpcProtocol.METHOD_NOT_FOUND);
    }

    @Test
    void parameterNamesAreCaseSensitive() {
      // "Minuend" instead of "minuend" — params not found, null passed to primitive int → error
      var params = MAPPER.createObjectNode().put("Minuend", 42).put("Subtrahend", 23);
      var response = dispatcher.dispatch(new JsonRpcRequest("2.0", "subtract", params, intId(1)));
      assertThat(response).isInstanceOf(JsonRpcError.class);
    }
  }

  private static IntNode intId(int value) {
    return IntNode.valueOf(value);
  }
}
