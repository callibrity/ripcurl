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
package com.callibrity.ripcurl.core.def;

import static java.util.Optional.ofNullable;

import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.JsonRpcError;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import com.callibrity.ripcurl.core.util.LazyInitializer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.node.JsonNodeType;

public class DefaultJsonRpcDispatcher implements JsonRpcDispatcher {

  private final LazyInitializer<Map<String, JsonRpcMethod>> methods;

  public DefaultJsonRpcDispatcher(List<JsonRpcMethodProvider> providers) {
    this.methods =
        LazyInitializer.of(
            () ->
                providers.stream()
                    .flatMap(provider -> provider.getJsonRpcMethodHandlers().stream())
                    .collect(Collectors.toMap(JsonRpcMethod::methodName, m -> m)));
  }

  @Override
  public JsonRpcResponse dispatch(JsonRpcRequest request) {
    try {
      validate(request);

      var method =
          ofNullable(methods.get().get(request.method()))
              .orElseThrow(
                  () ->
                      new JsonRpcException(
                          JsonRpcException.METHOD_NOT_FOUND,
                          String.format("JSON-RPC method \"%s\" not found.", request.method())));

      var response = method.call(request);

      // No id field at all → notification, no response
      if (request.id() == null) {
        return null;
      }
      return response;
    } catch (JsonRpcException e) {
      // Notifications get no error response either
      if (request.id() == null) {
        return null;
      }
      return new JsonRpcError(e.getCode(), e.getMessage(), request.id());
    }
  }

  private void validate(JsonRpcRequest request) {
    if (!JsonRpcProtocol.VERSION.equals(request.jsonrpc())) {
      throw new JsonRpcException(
          JsonRpcException.INVALID_REQUEST,
          String.format("jsonrpc value must be \"%s\".", JsonRpcProtocol.VERSION));
    }
    if (StringUtils.isBlank(request.method())) {
      throw new JsonRpcException(
          JsonRpcException.INVALID_REQUEST, "JSON-RPC method name is required.");
    }
    if (request.method().startsWith("rpc.")) {
      throw new JsonRpcException(
          JsonRpcException.METHOD_NOT_FOUND, "Methods starting with \"rpc.\" are reserved.");
    }
    if (request.id() != null
        && !request.id().isNull()
        && request.id().getNodeType() != JsonNodeType.STRING
        && request.id().getNodeType() != JsonNodeType.NUMBER) {
      throw new JsonRpcException(
          JsonRpcException.INVALID_REQUEST,
          String.format(
              "Invalid id type (%s). Must be a %s or %s.",
              request.id().getNodeType(), JsonNodeType.STRING, JsonNodeType.NUMBER));
    }
  }
}
