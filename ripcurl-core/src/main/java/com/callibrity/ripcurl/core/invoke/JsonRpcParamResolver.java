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

import java.lang.reflect.Parameter;
import tools.jackson.databind.JsonNode;

/**
 * Resolves a method parameter value during JSON-RPC method invocation. Implementations are
 * consulted in order; the first non-null result wins. Return {@code null} to indicate this resolver
 * cannot provide a value for the given parameter.
 */
@FunctionalInterface
public interface JsonRpcParamResolver {

  /**
   * Attempts to resolve a value for the given method parameter.
   *
   * @param parameter the method parameter to resolve
   * @param index the parameter's positional index in the method signature
   * @param params the JSON-RPC params node (may be null, object, or array)
   * @return the resolved value, or {@code null} if this resolver cannot handle it
   */
  Object resolve(Parameter parameter, int index, JsonNode params);
}
