package com.ripcurl.core.annotation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ripcurl.core.spi.JsonRpcMethodHandler;
import com.ripcurl.core.spi.JsonRpcMethodHandlerProvider;

import java.util.Arrays;
import java.util.List;

public class AnnotationJsonRpcMethodProvider implements JsonRpcMethodHandlerProvider {

// ------------------------------ FIELDS ------------------------------

    private final List<AnnotationJsonRpcMethodHandler> handlers;

// --------------------------- CONSTRUCTORS ---------------------------

    public AnnotationJsonRpcMethodProvider(ObjectMapper mapper, Object targetObject) {
        this.handlers = Arrays.stream(targetObject.getClass().getMethods())
                .filter(m -> m.isAnnotationPresent(JsonRpc.class))
                .map(m -> new AnnotationJsonRpcMethodHandler(mapper, targetObject, m))
                .toList();
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcMethodHandlerProvider ---------------------

    @Override
    public List<JsonRpcMethodHandler> getJsonRpcMethodHandlers() {
        return List.copyOf(handlers);
    }

}
