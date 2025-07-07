package com.callibrity.ripcurl.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcErrorResponse(String jsonrpc, JsonRpcError error, JsonNode id) {
}
