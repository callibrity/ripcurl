package com.ripcurl.core;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcResponse(String jsonrpc, JsonNode result, JsonNode id) {
}
