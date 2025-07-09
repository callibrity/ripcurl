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

import com.callibrity.ripcurl.core.JsonRpcService;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodHandlerProviderFactory;
import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodHandlerProviderFactory;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcMethodProvider;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcService;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodHandler;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodHandlerProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
public class RipCurlAutoConfiguration {

// -------------------------- OTHER METHODS --------------------------

    @Bean
    public DefaultJsonRpcMethodProvider defaultJsonRpcMethodProvider(List<JsonRpcMethodHandler> handlers) {
        return new DefaultJsonRpcMethodProvider(handlers);
    }

    @Bean
    public JsonRpcService defaultJsonRpcService(List<JsonRpcMethodHandlerProvider> providers) {
        return new DefaultJsonRpcService(providers);
    }

    @Bean
    public AnnotationJsonRpcMethodHandlerProviderFactory annotationJsonRpcMethodHandlerProviderFactory(ObjectMapper objectMapper) {
        return new DefaultAnnotationJsonRpcMethodHandlerProviderFactory(objectMapper);
    }

    @Bean
    public RipCurlController ripCurlController(JsonRpcService service) {
        return new RipCurlController(service);
    }

}
