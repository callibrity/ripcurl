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

import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcDispatcher;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class RipCurlAutoConfiguration {

// -------------------------- OTHER METHODS --------------------------

    @Bean
    public JsonRpcServiceMethodProvider jsonRpcServiceMethodProvider(ApplicationContext ctx, ObjectMapper mapper) {
        return new JsonRpcServiceMethodProvider(ctx, mapper);
    }

    @Bean
    public JsonRpcMethodProvider defaultJsonRpcMethodProvider(List<JsonRpcMethod> methods) {
        return () -> methods;
    }

    @Bean
    public JsonRpcDispatcher jsonRpcDispatcher(List<JsonRpcMethodProvider> providers) {
        return new DefaultJsonRpcDispatcher(providers);
    }

    @Bean
    public AnnotationJsonRpcMethodProviderFactory annotationJsonRpcMethodHandlerProviderFactory(ObjectMapper objectMapper) {
        return new DefaultAnnotationJsonRpcMethodProviderFactory(objectMapper);
    }

    @Bean
    public RipCurlController ripCurlController(JsonRpcDispatcher service) {
        return new RipCurlController(service);
    }

}
