package com.ripcurl.core.def;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.ripcurl.core.JsonRpcRequest;
import com.ripcurl.core.JsonRpcResponse;
import com.ripcurl.core.JsonRpcService;
import com.ripcurl.core.exception.JsonRpcInvalidRequestException;
import com.ripcurl.core.exception.JsonRpcMethodNotFoundException;
import com.ripcurl.core.spi.JsonRpcMethodHandler;
import com.ripcurl.core.spi.JsonRpcMethodHandlerProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class DefaultJsonRpcService implements JsonRpcService {

// ------------------------------ FIELDS ------------------------------

    public static final String VALID_JSONRPC_VERSION = "2.0";
    private final Map<String, JsonRpcMethodHandler> handlers;

// --------------------------- CONSTRUCTORS ---------------------------

    public DefaultJsonRpcService(List<JsonRpcMethodHandlerProvider> providers) {
        this.handlers = providers.stream()
                .flatMap(provider -> provider.getJsonRpcMethodHandlers().stream())
                .collect(Collectors.toMap(JsonRpcMethodHandler::methodName, m -> m));
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcService ---------------------

    @Override
    public JsonRpcResponse execute(JsonRpcRequest request) {
        if (!VALID_JSONRPC_VERSION.equals(request.jsonrpc())) {
            throw new JsonRpcInvalidRequestException(String.format("jsonrpc value must be \"%s\".", VALID_JSONRPC_VERSION));
        }
        if (StringUtils.isBlank(request.method())) {
            throw new JsonRpcInvalidRequestException("JSON-RPC method name is required.");
        }
        if (request.id() != null && request.id().getNodeType() != JsonNodeType.STRING && request.id().getNodeType() != JsonNodeType.NUMBER) {
            throw new JsonRpcInvalidRequestException(String.format("Invalid id type (%s). Must be a %s or %s.", request.id().getNodeType(), JsonNodeType.STRING, JsonNodeType.NUMBER));
        }

        var result = ofNullable(handlers.get(request.method()))
                .map(m -> m.call(request.params()))
                .orElseThrow(() -> new JsonRpcMethodNotFoundException(request.method()));

        if (request.id() == null || request.id().isNull()) {
            return null;
        }
        return new JsonRpcResponse(VALID_JSONRPC_VERSION, result, request.id());
    }

}
