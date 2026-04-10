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
 * Sealed base for JSON-RPC 2.0 request types. A "Request object" in the spec encompasses both calls
 * (with an id, expecting a response) and notifications (without an id, fire-and-forget). Jackson
 * deserializes this type via the annotated {@link #fromJson(JsonNode)} creator; the concrete
 * subtype is chosen by presence of the {@code id} field.
 */
public sealed interface JsonRpcRequest extends JsonRpcMessage
    permits JsonRpcCall, JsonRpcNotification {

  String method();

  JsonNode params();

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static JsonRpcRequest fromJson(JsonNode body) {
    if (!body.has("method")) {
      throw new IllegalArgumentException("JSON-RPC request is missing 'method' field");
    }
    String jsonrpc = body.path("jsonrpc").asString(null);
    String method = body.path("method").asString(null);
    JsonNode params = body.get("params");
    if (!body.has("id")) {
      return new JsonRpcNotification(jsonrpc, method, params);
    }
    return new JsonRpcCall(jsonrpc, method, params, body.get("id"));
  }
}
