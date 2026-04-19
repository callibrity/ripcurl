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
package com.callibrity.ripcurl.core.annotation;

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResult;
import java.lang.reflect.Method;
import org.jwcarman.methodical.MethodInvoker;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

/**
 * One built JSON-RPC method handler: a resolved name, the target {@link Method} + bean, and the
 * {@link MethodInvoker} wired with resolvers and the per-handler interceptor chain.
 *
 * <p>Construction goes through {@link JsonRpcMethodHandlers#build}; the constructor is
 * package-private.
 */
public final class JsonRpcMethodHandler {

  private final String name;
  private final Method method;
  private final Object bean;
  private final MethodInvoker<JsonNode> invoker;
  private final ObjectMapper mapper;

  JsonRpcMethodHandler(
      String name,
      Method method,
      Object bean,
      MethodInvoker<JsonNode> invoker,
      ObjectMapper mapper) {
    this.name = name;
    this.method = method;
    this.bean = bean;
    this.invoker = invoker;
    this.mapper = mapper;
  }

  public String name() {
    return name;
  }

  public Method method() {
    return method;
  }

  public Object bean() {
    return bean;
  }

  public JsonRpcResult call(JsonRpcRequest request) {
    var result = invoker.invoke(request.params());
    if (result instanceof JsonRpcResult jsonRpcResult) {
      return jsonRpcResult;
    }
    JsonNode id = request instanceof JsonRpcCall call ? call.id() : null;
    JsonNode jsonResult = result == null ? NullNode.getInstance() : mapper.valueToTree(result);
    return new JsonRpcResult(jsonResult, id);
  }
}
