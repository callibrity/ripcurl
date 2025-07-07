package com.callibrity.ripcurl.core.spi;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcMethodHandler {
    /**
     * Returns the name of the JSON-RPC method.
     *
     * @return the name of the JSON-RPC method
     */
    String methodName();

    /**
     * Executes the JSON-RPC method with the given parameters.
     *
     * @param params the parameters to be passed to the JSON-RPC method, represented as a JsonNode
     * @return the result of the JSON-RPC method execution, represented as a JsonNode
     */
    JsonNode call(JsonNode params);
}
