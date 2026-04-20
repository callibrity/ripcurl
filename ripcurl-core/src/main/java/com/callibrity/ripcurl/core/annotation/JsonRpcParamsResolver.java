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

import java.util.Optional;
import org.jwcarman.methodical.ParameterResolutionException;
import org.jwcarman.methodical.param.ParameterInfo;
import org.jwcarman.methodical.param.ParameterResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;

/**
 * Resolves {@code @JsonRpcParams}-annotated parameters by deserializing the entire JSON-RPC {@code
 * params} object into the parameter's type. The {@link ObjectReader} for that type is resolved once
 * in {@link #bind} and captured by the returned {@link Binding}, so each dispatch only pays for the
 * tree-to-value read — not for reader lookup.
 */
public class JsonRpcParamsResolver implements ParameterResolver<JsonNode> {

  private final ObjectMapper mapper;

  public JsonRpcParamsResolver(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Optional<Binding<JsonNode>> bind(ParameterInfo info) {
    if (!info.hasAnnotation(JsonRpcParams.class)) {
      return Optional.empty();
    }
    ObjectReader reader = mapper.readerFor(info.resolvedType());
    String paramName = info.name();
    return Optional.of(params -> deserialize(params, reader, paramName));
  }

  private Object deserialize(JsonNode params, ObjectReader reader, String paramName) {
    if (params == null || params.isNull()) {
      return null;
    }
    try {
      return reader.readValue(mapper.treeAsTokens(params));
    } catch (JacksonException e) {
      throw new ParameterResolutionException(
          String.format(
              "Unable to deserialize @JsonRpcParams parameter \"%s\": %s",
              paramName, e.getMessage()),
          e);
    }
  }
}
