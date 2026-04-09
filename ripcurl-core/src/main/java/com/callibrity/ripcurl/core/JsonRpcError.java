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

/** A JSON-RPC 2.0 error response. */
public record JsonRpcError(String jsonrpc, JsonRpcErrorDetail error, JsonNode id)
    implements JsonRpcResponse {

  public JsonRpcError(JsonRpcErrorDetail error, JsonNode id) {
    this(JsonRpcProtocol.VERSION, error, id);
  }

  public JsonRpcError(int code, String message, JsonNode id) {
    this(new JsonRpcErrorDetail(code, message), id);
  }
}
