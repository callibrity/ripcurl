package com.callibrity.ripcurl.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.ripcurl.core.JsonRpcRequest;
import com.ripcurl.core.JsonRpcResponse;
import com.ripcurl.core.JsonRpcService;
import com.ripcurl.core.exception.JsonRpcInternalErrorException;
import com.ripcurl.core.exception.JsonRpcInvalidParamsException;
import com.ripcurl.core.exception.JsonRpcInvalidRequestException;
import com.ripcurl.core.exception.JsonRpcMethodNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${ripcurl.endpoint:/jsonrpc}")
public class RipCurlController {

    public static final String JSON_RPC_ID_ATTR = "JSON_RPC_ID";
    private final JsonRpcService jsonRpcService;

    public RipCurlController(JsonRpcService jsonRpcService) {
        this.jsonRpcService = jsonRpcService;
    }

    @PostMapping
    public ResponseEntity<JsonRpcResponse> jsonRpc(@RequestBody JsonRpcRequest request, HttpServletRequest httpRequest) {
        httpRequest.setAttribute(JSON_RPC_ID_ATTR, request.id());
        var response = jsonRpcService.execute(request);
        if (response == null) {
            return ResponseEntity.noContent().build(); // 204 No Content for notifications
        }
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(JsonRpcInvalidParamsException.class)
    public ResponseEntity<JsonRpcErrorResponse> handleInvalidParams(JsonRpcInvalidParamsException ex, HttpServletRequest httpRequest) {
        var error = new JsonRpcError(-32602, "Invalid params", ex.getMessage());
        return ResponseEntity.ok(new JsonRpcErrorResponse("2.0", error, (JsonNode) httpRequest.getAttribute(JSON_RPC_ID_ATTR)));
    }

    @ExceptionHandler(JsonRpcInvalidRequestException.class)
    public ResponseEntity<JsonRpcErrorResponse> handleInvalidRequest(JsonRpcInvalidRequestException ex, HttpServletRequest httpRequest) {
        var error = new JsonRpcError(-32600, "Invalid Request", ex.getMessage());
        return ResponseEntity.ok(new JsonRpcErrorResponse("2.0", error, (JsonNode) httpRequest.getAttribute(JSON_RPC_ID_ATTR)));
    }

    @ExceptionHandler(JsonRpcMethodNotFoundException.class)
    public ResponseEntity<JsonRpcErrorResponse> handleMethodNotFound(JsonRpcMethodNotFoundException ex, HttpServletRequest httpRequest) {
        var error = new JsonRpcError(-32601, "Method not found", ex.getMessage());
        return ResponseEntity.ok(new JsonRpcErrorResponse("2.0", error, (JsonNode) httpRequest.getAttribute(JSON_RPC_ID_ATTR)));
    }

    @ExceptionHandler(JsonRpcInternalErrorException.class)
    public ResponseEntity<JsonRpcErrorResponse> handleInternalError(JsonRpcInternalErrorException ex, HttpServletRequest httpRequest) {
        var id = (JsonNode) httpRequest.getAttribute(JSON_RPC_ID_ATTR);
        if (id == null || id.isNull()) {
            return ResponseEntity.noContent().build();
        }
        var error = new JsonRpcError(-32603, "Internal error", ex.getMessage());
        return ResponseEntity.ok(new JsonRpcErrorResponse("2.0", error, id));
    }
}
