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
package com.callibrity.ripcurl.autoconfigure;

import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcService;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;

import java.util.List;

public class JsonRpcServiceMethodProvider implements JsonRpcMethodProvider {

// ------------------------------ FIELDS ------------------------------

    private List<AnnotationJsonRpcMethod> methods;

    private final ApplicationContext ctx;
    private final ObjectMapper mapper;

// --------------------------- CONSTRUCTORS ---------------------------

    public JsonRpcServiceMethodProvider(ApplicationContext ctx, ObjectMapper mapper) {
        this.ctx = ctx;
        this.mapper = mapper;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcMethodProvider ---------------------

    @Override
    public List<JsonRpcMethod> getJsonRpcMethodHandlers() {
        return List.copyOf(methods);
    }

// -------------------------- OTHER METHODS --------------------------

    @PostConstruct
    public void initialize() {
        this.methods = ctx.getBeansWithAnnotation(JsonRpcService.class).values().stream()
                .flatMap(bean -> AnnotationJsonRpcMethod.createMethods(mapper, bean).stream())
                .toList();
    }

}
