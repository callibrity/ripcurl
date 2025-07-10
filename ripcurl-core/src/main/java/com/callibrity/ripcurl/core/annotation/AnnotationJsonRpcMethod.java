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

import com.callibrity.ripcurl.core.invoke.JsonMethodInvoker;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class AnnotationJsonRpcMethod implements JsonRpcMethod {

// ------------------------------ FIELDS ------------------------------

    private final String name;
    private final JsonMethodInvoker invoker;

// -------------------------- STATIC METHODS --------------------------

    public static List<AnnotationJsonRpcMethod> createMethods(ObjectMapper mapper, Object targetObject) {
        return MethodUtils.getMethodsListWithAnnotation(targetObject.getClass(), JsonRpc.class).stream()
                .map(method -> new AnnotationJsonRpcMethod(mapper, targetObject, method))
                .toList();
    }

// --------------------------- CONSTRUCTORS ---------------------------

    AnnotationJsonRpcMethod(ObjectMapper mapper, Object targetObject, Method method) {
        this.invoker = new JsonMethodInvoker(mapper, targetObject, method);
        var annotation = method.getAnnotation(JsonRpc.class);
        this.name = ofNullable(StringUtils.trimToNull(annotation.value()))
                .orElseGet(() -> String.format("%s.%s", ClassUtils.getSimpleName(targetObject), method.getName()));
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcMethod ---------------------

    @Override
    public String methodName() {
        return name;
    }

    @Override
    public JsonNode call(JsonNode params) {
        return invoker.invoke(params);
    }

}
