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
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.StringNode;

class JsonRpcRequestTest {

  @Test
  void requestFactorySetsVersion() {
    var params = JsonNodeFactory.instance.objectNode();
    var id = StringNode.valueOf("1");
    var request = JsonRpcRequest.request("test.method", params, id);

    assertThat(request.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(request.method()).isEqualTo("test.method");
    assertThat(request.params()).isSameAs(params);
    assertThat(request.id()).isSameAs(id);
  }

  @Test
  void notificationFactorySetsVersionAndNullId() {
    var params = JsonNodeFactory.instance.objectNode();
    var request = JsonRpcRequest.notification("test.notify", params);

    assertThat(request.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(request.method()).isEqualTo("test.notify");
    assertThat(request.params()).isSameAs(params);
    assertThat(request.id()).isNull();
  }

  @Test
  void responseShouldEchoId() {
    var id = StringNode.valueOf("42");
    var request = JsonRpcRequest.request("test", null, id);
    var result = request.response(StringNode.valueOf("ok"));
    assertThat(result.id()).isSameAs(id);
    assertThat(result.result()).isEqualTo(StringNode.valueOf("ok"));
  }

  @Test
  void errorShouldEchoId() {
    var id = StringNode.valueOf("42");
    var request = JsonRpcRequest.request("test", null, id);
    var error = request.error(-32601, "Not found");
    assertThat(error.id()).isSameAs(id);
    assertThat(error.error().code()).isEqualTo(-32601);
    assertThat(error.error().message()).isEqualTo("Not found");
  }
}
