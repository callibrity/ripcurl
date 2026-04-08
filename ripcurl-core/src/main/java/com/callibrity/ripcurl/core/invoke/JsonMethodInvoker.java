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
package com.callibrity.ripcurl.core.invoke;

import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

public class JsonMethodInvoker {

  // ------------------------------ FIELDS ------------------------------

  private static final Object[] EMPTY_ARGS = new Object[0];
  private final ObjectMapper mapper;
  private final Object targetObject;
  private final Method method;
  private final List<JsonRpcParamResolver> resolvers;

  // --------------------------- CONSTRUCTORS ---------------------------

  public JsonMethodInvoker(ObjectMapper mapper, Object targetObject, Method method) {
    this(mapper, targetObject, method, List.of());
  }

  public JsonMethodInvoker(
      ObjectMapper mapper,
      Object targetObject,
      Method method,
      List<JsonRpcParamResolver> resolvers) {
    this.mapper = mapper;
    this.targetObject = targetObject;
    this.method = method;
    var allResolvers = new ArrayList<>(resolvers);
    allResolvers.add(new JsonParamResolver(mapper, targetObject.getClass()));
    this.resolvers = List.copyOf(allResolvers);
  }

  // -------------------------- OTHER METHODS --------------------------

  public JsonRpcResponse invoke(JsonRpcRequest request) {
    var arguments = resolveArguments(request.params());
    try {
      var result = method.invoke(targetObject, arguments);
      if (result instanceof JsonRpcResponse response) {
        return response;
      }
      JsonNode jsonResult;
      if (Void.TYPE.equals(method.getReturnType()) || Void.class.equals(method.getReturnType())) {
        jsonResult = NullNode.getInstance();
      } else {
        jsonResult = mapper.valueToTree(result);
      }
      return request.response(jsonResult);
    } catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof JsonRpcException jre) {
        throw jre;
      }
      throw new JsonRpcException(
          JsonRpcException.INTERNAL_ERROR,
          String.format("Method invocation failed for method %s.", method),
          e);
    } catch (ReflectiveOperationException e) {
      throw new JsonRpcException(
          JsonRpcException.INTERNAL_ERROR,
          String.format("Method invocation failed for method %s.", method),
          e);
    }
  }

  private Object[] resolveArguments(JsonNode parameters) {
    var methodParams = method.getParameters();
    if (methodParams.length == 0) {
      return EMPTY_ARGS;
    }
    var args = new Object[methodParams.length];
    for (int i = 0; i < methodParams.length; i++) {
      args[i] = resolveParameter(methodParams[i], i, parameters);
    }
    return args;
  }

  private Object resolveParameter(Parameter parameter, int index, JsonNode params) {
    for (JsonRpcParamResolver resolver : resolvers) {
      Object value = resolver.resolve(parameter, index, params);
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
