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

import java.util.Objects;
import tools.jackson.databind.JsonNode;

/** A JSON-RPC 2.0 request with an id — expects a response. */
public record JsonRpcCall(String jsonrpc, String method, JsonNode params, JsonNode id)
    implements JsonRpcRequest {

  public JsonRpcCall {
    Objects.requireNonNull(method, "method must not be null");
    Objects.requireNonNull(
        id, "id must not be null; use JsonRpcNotification for requests without an id");
  }

  /** Creates a JSON-RPC call with the version set automatically. */
  public static JsonRpcCall of(String method, JsonNode params, JsonNode id) {
    return new JsonRpcCall(JsonRpcProtocol.VERSION, method, params, id);
  }

  /** Creates a successful JSON-RPC response for this call, echoing the id. */
  public JsonRpcResult result(JsonNode result) {
    return new JsonRpcResult(result, this.id);
  }

  /** Creates an error JSON-RPC response for this call, echoing the id. */
  public JsonRpcError error(int code, String message) {
    return new JsonRpcError(code, message, this.id);
  }
}
