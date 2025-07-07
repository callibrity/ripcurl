package com.callibrity.ripcurl.core.spi;

import java.util.List;

@FunctionalInterface
public interface JsonRpcMethodHandlerProvider {

// -------------------------- OTHER METHODS --------------------------

    /**
     * Provides a list of JSON-RPC method handlers. Each handler is responsible for
     * handling a specific JSON-RPC method and defining its behavior.
     *
     * @return a list of {@link JsonRpcMethodHandler} instances representing
     *         the JSON-RPC methods that can be handled.
     */
    List<JsonRpcMethodHandler> getJsonRpcMethodHandlers();

}
