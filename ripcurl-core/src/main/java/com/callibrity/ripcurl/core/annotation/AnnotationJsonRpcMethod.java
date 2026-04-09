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

import static java.util.Optional.ofNullable;

import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jwcarman.methodical.MethodInvoker;
import org.jwcarman.methodical.MethodInvokerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

public class AnnotationJsonRpcMethod implements com.callibrity.ripcurl.core.spi.JsonRpcMethod {

  // ------------------------------ FIELDS ------------------------------

  private final String name;
  private final MethodInvoker<JsonNode> invoker;
  private final ObjectMapper mapper;

  // -------------------------- STATIC METHODS --------------------------

  public static List<AnnotationJsonRpcMethod> createMethods(
      ObjectMapper mapper, Object targetObject, MethodInvokerFactory invokerFactory) {
    return MethodUtils.getMethodsListWithAnnotation(targetObject.getClass(), JsonRpcMethod.class)
        .stream()
        .map(method -> new AnnotationJsonRpcMethod(mapper, targetObject, method, invokerFactory))
        .toList();
  }

  // --------------------------- CONSTRUCTORS ---------------------------

  AnnotationJsonRpcMethod(
      ObjectMapper mapper,
      Object targetObject,
      Method method,
      MethodInvokerFactory invokerFactory) {
    this.mapper = mapper;
    this.invoker = invokerFactory.create(method, targetObject, JsonNode.class);
    var annotation = method.getAnnotation(JsonRpcMethod.class);
    this.name =
        ofNullable(StringUtils.trimToNull(annotation.value()))
            .orElseGet(
                () ->
                    String.format(
                        "%s.%s", ClassUtils.getSimpleName(targetObject), method.getName()));
  }

  // ------------------------ INTERFACE METHODS ------------------------

  // --------------------- Interface JsonRpcMethod ---------------------

  @Override
  public String methodName() {
    return name;
  }

  @Override
  public JsonRpcResponse call(JsonRpcRequest request) {
    var result = invoker.invoke(request.params());
    if (result instanceof JsonRpcResponse response) {
      return response;
    }
    if (result == null) {
      return request.response(NullNode.getInstance());
    }
    JsonNode jsonResult = mapper.valueToTree(result);
    return request.response(jsonResult);
  }
}
