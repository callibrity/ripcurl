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

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.JsonNode;

/**
 * Sealed base for all JSON-RPC 2.0 message types. Jackson deserializes this polymorphic type via
 * the annotated {@link #fromJson(JsonNode)} creator, which dispatches to the appropriate concrete
 * subtype based on the structural shape of the JSON (presence of {@code method}, {@code result}, or
 * {@code error} fields). Use {@code objectMapper.treeToValue(node, JsonRpcMessage.class)} or {@code
 * objectMapper.readerFor(JsonRpcMessage.class).readValue(json)}.
 */
public sealed interface JsonRpcMessage permits JsonRpcRequest, JsonRpcResponse {

  String jsonrpc();

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static JsonRpcMessage fromJson(JsonNode body) {
    if (body.has("method")) {
      return JsonRpcRequest.fromJson(body);
    }
    if (body.has("result") || body.has("error")) {
      return JsonRpcResponse.fromJson(body);
    }
    throw new IllegalArgumentException("Unrecognized JSON-RPC message");
  }
}
