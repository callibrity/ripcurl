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
 * Sealed base for JSON-RPC 2.0 response types. Jackson deserializes this type via the annotated
 * {@link #fromJson(JsonNode)} creator; the concrete subtype is chosen by presence of the {@code
 * result} or {@code error} field.
 */
public sealed interface JsonRpcResponse extends JsonRpcMessage permits JsonRpcResult, JsonRpcError {
  String jsonrpc();

  JsonNode id();

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static JsonRpcResponse fromJson(JsonNode body) {
    JsonNode id = body.get("id");
    if (body.has("result")) {
      return new JsonRpcResult(body.get("result"), id);
    }
    if (body.has("error")) {
      JsonNode errorNode = body.get("error");
      return new JsonRpcError(
          new JsonRpcErrorDetail(
              errorNode.path("code").intValue(), errorNode.path("message").asString(null)),
          id);
    }
    throw new IllegalArgumentException("Unrecognized JSON-RPC response");
  }
}
