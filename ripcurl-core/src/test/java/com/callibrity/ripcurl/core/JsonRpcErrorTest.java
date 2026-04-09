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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

class JsonRpcErrorTest {

  private static final StringNode TEST_ID = StringNode.valueOf("1");

  @Test
  void convenienceConstructorWithCodeAndMessage() {
    var error = new JsonRpcError(-32601, "Method not found", TEST_ID);
    assertThat(error.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(error.error().code()).isEqualTo(-32601);
    assertThat(error.error().message()).isEqualTo("Method not found");
    assertThat(error.id()).isEqualTo(TEST_ID);
  }

  @Test
  void constructorWithDetail() {
    var detail = new JsonRpcErrorDetail(-32600, "Invalid Request");
    var error = new JsonRpcError(detail, TEST_ID);
    assertThat(error.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(error.error()).isSameAs(detail);
    assertThat(error.id()).isEqualTo(TEST_ID);
  }

  @Test
  void errorDetailWithData() {
    var detail = new JsonRpcErrorDetail(-32603, "Internal error", StringNode.valueOf("extra"));
    assertThat(detail.data()).isEqualTo(StringNode.valueOf("extra"));
  }

  @Test
  void errorDetailWithoutData() {
    var detail = new JsonRpcErrorDetail(-32603, "Internal error");
    assertThat(detail.data()).isNull();
  }

  @Test
  void serializedJsonHasCorrectShape() {
    var error = new JsonRpcError(-32601, "Method not found", TEST_ID);
    var json = new tools.jackson.databind.ObjectMapper().writeValueAsString(error);
    assertThat(json)
        .contains("\"jsonrpc\":\"2.0\"")
        .contains("\"error\"")
        .contains("\"code\":-32601")
        .contains("\"message\":\"Method not found\"")
        .contains("\"id\":\"1\"")
        .doesNotContain("\"result\"");
  }
}
