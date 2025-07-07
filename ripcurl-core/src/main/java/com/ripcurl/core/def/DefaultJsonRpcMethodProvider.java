package com.ripcurl.core.def;

import com.ripcurl.core.spi.JsonRpcMethodHandler;
import com.ripcurl.core.spi.JsonRpcMethodHandlerProvider;

import java.util.List;

public class DefaultJsonRpcMethodProvider implements JsonRpcMethodHandlerProvider {

// ------------------------------ FIELDS ------------------------------

    private final List<JsonRpcMethodHandler> beans;

// --------------------------- CONSTRUCTORS ---------------------------

    public DefaultJsonRpcMethodProvider(List<JsonRpcMethodHandler> beans) {
        this.beans = beans;
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcMethodHandlerProvider ---------------------

    @Override
    public List<JsonRpcMethodHandler> getJsonRpcMethodHandlers() {
        return List.copyOf(beans);
    }

}
