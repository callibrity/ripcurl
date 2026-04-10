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

import org.jwcarman.methodical.ParameterResolutionException;
import org.jwcarman.methodical.param.ParameterInfo;
import org.jwcarman.methodical.param.ParameterResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves {@code @JsonRpcParams}-annotated parameters by deserializing the entire JSON-RPC {@code
 * params} object into the parameter's type.
 */
public class JsonRpcParamsResolver implements ParameterResolver<JsonNode> {

  private final ObjectMapper mapper;

  public JsonRpcParamsResolver(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public boolean supports(ParameterInfo info) {
    return info.parameter().isAnnotationPresent(JsonRpcParams.class);
  }

  @Override
  public Object resolve(ParameterInfo info, JsonNode params) {
    if (params == null || params.isNull()) {
      return null;
    }
    try {
      return mapper.readerFor(info.resolvedType()).readValue(mapper.treeAsTokens(params));
    } catch (JacksonException e) {
      throw new ParameterResolutionException(
          String.format(
              "Unable to deserialize @JsonRpcParams parameter \"%s\": %s",
              info.name(), e.getMessage()),
          e);
    }
  }
}
