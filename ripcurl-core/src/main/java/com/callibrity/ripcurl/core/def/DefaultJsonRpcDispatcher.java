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
package com.callibrity.ripcurl.core.def;

import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;
import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.exception.JsonRpcInvalidRequestException;
import com.callibrity.ripcurl.core.exception.JsonRpcMethodNotFoundException;
import com.callibrity.ripcurl.core.spi.JsonRpcMethod;
import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class DefaultJsonRpcDispatcher implements JsonRpcDispatcher {

// ------------------------------ FIELDS ------------------------------

    public static final String VALID_JSONRPC_VERSION = "2.0";
    private final AtomicReference<Map<String,JsonRpcMethod>> methods = new AtomicReference<>();
    private final List<JsonRpcMethodProvider> providers;

// --------------------------- CONSTRUCTORS ---------------------------

    public DefaultJsonRpcDispatcher(List<JsonRpcMethodProvider> providers) {
        this.providers = providers;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcDispatcher ---------------------

    @Override
    public JsonRpcResponse dispatch(JsonRpcRequest request) {
        if (!VALID_JSONRPC_VERSION.equals(request.jsonrpc())) {
            throw new JsonRpcInvalidRequestException(String.format("jsonrpc value must be \"%s\".", VALID_JSONRPC_VERSION));
        }
        if (StringUtils.isBlank(request.method())) {
            throw new JsonRpcInvalidRequestException("JSON-RPC method name is required.");
        }
        if (request.id() != null && request.id().getNodeType() != JsonNodeType.STRING && request.id().getNodeType() != JsonNodeType.NUMBER) {
            throw new JsonRpcInvalidRequestException(String.format("Invalid id type (%s). Must be a %s or %s.", request.id().getNodeType(), JsonNodeType.STRING, JsonNodeType.NUMBER));
        }

        var result = ofNullable(getMethods().get(request.method()))
                .map(m -> m.call(request.params()))
                .orElseThrow(() -> new JsonRpcMethodNotFoundException(request.method()));

        if (request.id() == null) {
            return null;
        }
        return new JsonRpcResponse(VALID_JSONRPC_VERSION, result, request.id());
    }

// -------------------------- OTHER METHODS --------------------------

    private Map<String,JsonRpcMethod> getMethods() {
        final var current = methods.get();
        if(current != null) {
            return current;
        }
        final var initialized = providers.stream()
                .flatMap(provider -> provider.getJsonRpcMethodHandlers().stream())
                .collect(Collectors.toMap(JsonRpcMethod::methodName, m -> m));
        methods.compareAndSet(null, initialized);
        return methods.get();
    }

}
