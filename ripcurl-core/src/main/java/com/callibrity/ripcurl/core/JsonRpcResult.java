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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import java.util.Optional;
import tools.jackson.databind.JsonNode;

/** A successful JSON-RPC 2.0 response. */
public record JsonRpcResult(
    String jsonrpc, JsonNode result, JsonNode id, @JsonIgnore Map<String, Object> metadata)
    implements JsonRpcResponse {

  public JsonRpcResult(JsonNode result, JsonNode id) {
    this(JsonRpcProtocol.VERSION, result, id, Map.of());
  }

  public JsonRpcResult withMetadata(String name, Object value) {
    var copy = new java.util.HashMap<>(metadata);
    copy.put(name, value);
    return new JsonRpcResult(jsonrpc, result, id, Map.copyOf(copy));
  }

  public <T> Optional<T> getMetadata(String name, Class<T> type) {
    return Optional.ofNullable(metadata.get(name)).map(type::cast);
  }
}
