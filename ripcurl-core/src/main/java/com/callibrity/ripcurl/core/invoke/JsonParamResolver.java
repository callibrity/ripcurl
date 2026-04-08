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
import java.lang.reflect.Parameter;
import org.apache.commons.lang3.reflect.TypeUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Default parameter resolver that extracts values from the JSON-RPC params node. Supports both
 * by-name (object) and by-position (array) parameter styles per the JSON-RPC 2.0 specification.
 * This resolver should be last in the resolver chain.
 */
public class JsonParamResolver implements JsonRpcParamResolver {

  private final ObjectMapper mapper;
  private final Class<?> targetClass;

  public JsonParamResolver(ObjectMapper mapper, Class<?> targetClass) {
    this.mapper = mapper;
    this.targetClass = targetClass;
  }

  @Override
  public Object resolve(Parameter parameter, int index, JsonNode params) {
    if (params == null || params.isNull()) {
      return null;
    }
    JsonNode node = extractNode(parameter, index, params);
    if (node == null || node.isNull()) {
      return null;
    }
    return deserialize(parameter, node);
  }

  private JsonNode extractNode(Parameter parameter, int index, JsonNode params) {
    return switch (params.getNodeType()) {
      case OBJECT -> params.get(parameter.getName());
      case ARRAY -> index < params.size() ? params.get(index) : null;
      default ->
          throw new JsonRpcException(
              JsonRpcException.INVALID_PARAMS,
              String.format("Unsupported JSON-RPC parameters type %s.", params.getNodeType()));
    };
  }

  private Object deserialize(Parameter parameter, JsonNode node) {
    try {
      var type = TypeUtils.getRawType(parameter.getParameterizedType(), targetClass);
      return mapper.readerFor(type).readValue(mapper.treeAsTokens(node));
    } catch (JacksonException e) {
      throw new JsonRpcException(
          JsonRpcException.INVALID_PARAMS,
          String.format(
              "Unable to read parameter \"%s\" value (%s).", parameter.getName(), e.getMessage()));
    }
  }
}
