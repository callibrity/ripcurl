package com.callibrity.ripcurl.core.exception;

public class JsonRpcMethodNotFoundException extends JsonRpcException {

// --------------------------- CONSTRUCTORS ---------------------------

    public JsonRpcMethodNotFoundException(String methodName) {
        super(String.format("JSON-RPC method \"%s\" not found.", methodName));
    }

}
