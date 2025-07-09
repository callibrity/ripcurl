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

import com.callibrity.ripcurl.core.spi.JsonRpcMethodHandlerProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.List;

public class DefaultAnnotationJsonRpcMethodHandlerProviderFactory implements AnnotationJsonRpcMethodHandlerProviderFactory {

// ------------------------------ FIELDS ------------------------------

    private final ObjectMapper mapper;

// --------------------------- CONSTRUCTORS ---------------------------

    public DefaultAnnotationJsonRpcMethodHandlerProviderFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface AnnotationJsonRpcMethodHandlerProviderFactory ---------------------

    @Override
    public JsonRpcMethodHandlerProvider create(Object targetObject) {
        var handlers = MethodUtils.getMethodsListWithAnnotation(targetObject.getClass(), JsonRpc.class).stream()
                .map(m -> new AnnotationJsonRpcMethodHandler(mapper, targetObject, m))
                .toList();
        return () -> List.copyOf(handlers);
    }

}
