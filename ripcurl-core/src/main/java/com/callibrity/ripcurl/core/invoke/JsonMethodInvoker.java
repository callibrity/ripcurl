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

import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
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
  private final List<JsonParameterMapper> parameterMappers;
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
    this.resolvers = List.copyOf(resolvers);
    this.parameterMappers =
        Arrays.stream(method.getParameters())
            .map(p -> new JsonParameterMapper(mapper, targetObject, p))
            .toList();
  }

  // -------------------------- OTHER METHODS --------------------------

  public JsonNode invoke(JsonNode parameters) {
    var arguments = parseJsonParameters(parameters);
    try {
      var result = method.invoke(targetObject, arguments);
      if (Void.TYPE.equals(method.getReturnType()) || Void.class.equals(method.getReturnType())) {
        return NullNode.getInstance();
      }
      return mapper.valueToTree(result);
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

  private Object[] parseJsonParameters(JsonNode parameters) {
    if (parameterMappers.isEmpty()) {
      return EMPTY_ARGS;
    }
    var methodParams = method.getParameters();
    var args = new Object[parameterMappers.size()];
    for (int i = 0; i < parameterMappers.size(); i++) {
      Object resolved = resolveFromResolvers(methodParams[i]);
      if (resolved != null) {
        args[i] = resolved;
      } else {
        args[i] = resolveFromJson(parameters, i);
      }
    }
    return args;
  }

  private Object resolveFromResolvers(Parameter parameter) {
    for (JsonRpcParamResolver resolver : resolvers) {
      Object value = resolver.resolve(parameter);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private Object resolveFromJson(JsonNode parameters, int index) {
    if (parameters == null || parameters.isNull()) {
      return null;
    }
    return switch (parameters.getNodeType()) {
      case OBJECT ->
          parameterMappers
              .get(index)
              .mapParameter(parameters.get(parameterMappers.get(index).getParameterName()));
      case ARRAY -> {
        if (index < parameters.size()) {
          yield parameterMappers.get(index).mapParameter(parameters.get(index));
        }
        yield parameterMappers.get(index).mapParameter(NullNode.getInstance());
      }
      default ->
          throw new JsonRpcException(
              JsonRpcException.INVALID_PARAMS,
              String.format("Unsupported JSON-RPC parameters type %s.", parameters.getNodeType()));
    };
  }
}
