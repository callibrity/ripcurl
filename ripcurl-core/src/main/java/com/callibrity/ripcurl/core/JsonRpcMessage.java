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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Sealed base for all JSON-RPC 2.0 message types. Use {@link #parse(JsonNode, ObjectMapper)} to
 * deserialize an incoming message into the appropriate type.
 */
public sealed interface JsonRpcMessage
    permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse {

  String jsonrpc();

  /**
   * Parses a JSON-RPC message from a JsonNode, returning the appropriate concrete type.
   *
   * @param body the raw JSON body
   * @param mapper the ObjectMapper for deserialization
   * @return the parsed message
   * @throws IllegalArgumentException if the message cannot be classified
   */
  static JsonRpcMessage parse(JsonNode body, ObjectMapper mapper) {
    if (body.has("method")) {
      String jsonrpc = body.path("jsonrpc").asString(null);
      String method = body.path("method").asString(null);
      JsonNode params = body.get("params");
      JsonNode id = body.get("id");
      if (id == null) {
        return new JsonRpcNotification(jsonrpc, method, params);
      }
      return new JsonRpcRequest(jsonrpc, method, params, id);
    }
    if (body.has("result")) {
      return mapper.treeToValue(body, JsonRpcResult.class);
    }
    if (body.has("error")) {
      return mapper.treeToValue(body, JsonRpcError.class);
    }
    throw new IllegalArgumentException("Unrecognized JSON-RPC message");
  }
}
