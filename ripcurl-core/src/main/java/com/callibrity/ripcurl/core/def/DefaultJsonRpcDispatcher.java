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

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.JsonRpcError;
import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcNotification;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandler;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.JsonNodeType;

public class DefaultJsonRpcDispatcher implements JsonRpcDispatcher {

  private static final Logger log = LoggerFactory.getLogger(DefaultJsonRpcDispatcher.class);

  private final Map<String, JsonRpcMethodHandler> methods;
  private final JsonRpcExceptionTranslatorRegistry translators;

  /**
   * Convenience constructor that wires a {@link DefaultJsonRpcExceptionTranslatorRegistry}
   * populated with the built-in translators: {@link DefaultJsonRpcExceptionTranslator}, {@link
   * IllegalArgumentExceptionTranslator}, and {@link ParameterResolutionExceptionTranslator}. The
   * registry's own built-in fallback handles any uncaught exception as {@code -32603 Internal
   * error}. Suitable when no custom exception-to-JSON-RPC-error mapping is needed.
   */
  public DefaultJsonRpcDispatcher(List<JsonRpcMethodHandler> handlers) {
    this(
        handlers,
        new DefaultJsonRpcExceptionTranslatorRegistry(
            List.of(
                new DefaultJsonRpcExceptionTranslator(),
                new IllegalArgumentExceptionTranslator(),
                new ParameterResolutionExceptionTranslator())));
  }

  public DefaultJsonRpcDispatcher(
      List<JsonRpcMethodHandler> handlers, JsonRpcExceptionTranslatorRegistry translators) {
    this.translators = Objects.requireNonNull(translators, "translators");
    this.methods =
        handlers.stream().collect(Collectors.toUnmodifiableMap(JsonRpcMethodHandler::name, h -> h));
    this.methods.keySet().forEach(name -> log.info("Registered JSON-RPC method: {}", name));
  }

  @Override
  public JsonRpcResponse dispatch(JsonRpcRequest request) {
    return switch (request) {
      case JsonRpcCall call -> dispatchCall(call);
      case JsonRpcNotification notification -> dispatchNotification(notification);
    };
  }

  private JsonRpcResponse dispatchCall(JsonRpcCall call) {
    try {
      validate(call);
      var method =
          ofNullable(methods.get(call.method()))
              .orElseThrow(
                  () ->
                      new JsonRpcException(
                          JsonRpcProtocol.METHOD_NOT_FOUND,
                          String.format("JSON-RPC method \"%s\" not found.", call.method())));
      return ScopedValue.where(JsonRpcDispatcher.CURRENT_REQUEST, call)
          .call(() -> method.call(call));
    } catch (Exception e) {
      JsonRpcErrorDetail detail = translators.translate(e);
      return new JsonRpcError(detail, call.id());
    }
  }

  private JsonRpcResponse dispatchNotification(JsonRpcNotification notification) {
    try {
      validate(notification);
      var method = methods.get(notification.method());
      if (method != null) {
        ScopedValue.where(JsonRpcDispatcher.CURRENT_REQUEST, notification)
            .run(() -> method.call(notification));
      }
    } catch (Exception e) {
      log.warn("Notification '{}' failed: {}", notification.method(), e.getMessage(), e);
    }
    return null;
  }

  private void validate(JsonRpcRequest request) {
    if (!JsonRpcProtocol.VERSION.equals(request.jsonrpc())) {
      throw new JsonRpcException(
          JsonRpcProtocol.INVALID_REQUEST,
          String.format("jsonrpc value must be \"%s\".", JsonRpcProtocol.VERSION));
    }
    if (StringUtils.isBlank(request.method())) {
      throw new JsonRpcException(
          JsonRpcProtocol.INVALID_REQUEST, "JSON-RPC method name is required.");
    }
    if (request.method().startsWith("rpc.")) {
      throw new JsonRpcException(
          JsonRpcProtocol.METHOD_NOT_FOUND, "Methods starting with \"rpc.\" are reserved.");
    }
    if (request.params() != null && !request.params().isObject() && !request.params().isArray()) {
      throw new JsonRpcException(
          JsonRpcProtocol.INVALID_REQUEST,
          String.format(
              "Invalid params type (%s). Must be an Object or Array.",
              request.params().getNodeType()));
    }
    if (request instanceof JsonRpcCall call) {
      validateId(call);
    }
  }

  private void validateId(JsonRpcCall call) {
    if (!call.id().isNull()
        && call.id().getNodeType() != JsonNodeType.STRING
        && call.id().getNodeType() != JsonNodeType.NUMBER) {
      throw new JsonRpcException(
          JsonRpcProtocol.INVALID_REQUEST,
          String.format(
              "Invalid id type (%s). Must be a %s or %s.",
              call.id().getNodeType(), JsonNodeType.STRING, JsonNodeType.NUMBER));
    }
  }
}
