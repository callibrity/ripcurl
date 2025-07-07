package com.callibrity.ripcurl.core;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcRequest(String jsonrpc, String method, JsonNode params, JsonNode id) {
}
