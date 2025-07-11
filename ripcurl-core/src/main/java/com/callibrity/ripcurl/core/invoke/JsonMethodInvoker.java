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
import com.callibrity.ripcurl.core.exception.JsonRpcInternalErrorException;
import com.callibrity.ripcurl.core.exception.JsonRpcInvalidParamsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class JsonMethodInvoker {

// ------------------------------ FIELDS ------------------------------

    private static final Object[] EMPTY_ARGS = new Object[0];
    private final ObjectMapper mapper;
    private final Object targetObject;
    private final Method method;
    private final List<JsonParameterMapper> parameterMappers;

// --------------------------- CONSTRUCTORS ---------------------------

    public JsonMethodInvoker(ObjectMapper mapper, Object targetObject, Method method) {
        this.mapper = mapper;
        this.targetObject = targetObject;
        this.method = method;
        this.parameterMappers = Arrays.stream(method.getParameters())
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
            if(e.getTargetException() instanceof JsonRpcException jre) {
                throw jre;
            }
            throw new JsonRpcInternalErrorException(String.format("Method invocation failed for method %s.", method), e);
        } catch(ReflectiveOperationException e) {
            throw new JsonRpcInternalErrorException(String.format("Method invocation failed for method %s.", method), e);
        }
    }

    private Object[] parseJsonParameters(JsonNode parameters) {
        if (parameterMappers.isEmpty()) {
            return EMPTY_ARGS;
        }
        if (parameters == null || parameters.isNull()) {
            return new Object[parameterMappers.size()];
        }
        return switch (parameters.getNodeType()) {
            case OBJECT -> parameterMappers.stream()
                    .map(m -> m.mapParameter(parameters.get(m.getParameterName())))
                    .toArray(Object[]::new);
            case ARRAY -> IntStream.range(0, parameterMappers.size())
                    .mapToObj(i -> {
                        if (i < parameters.size()) {
                            return parameterMappers.get(i).mapParameter(parameters.get(i));
                        }
                        return parameterMappers.get(i).mapParameter(NullNode.getInstance());
                    })
                    .toArray(Object[]::new);
            default ->
                    throw new JsonRpcInvalidParamsException(String.format("Unsupported JSON-RPC parameters type %s.", parameters.getNodeType()));
        };
    }

}
