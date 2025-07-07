package com.ripcurl.core.exception;

public class JsonRpcInternalErrorException extends JsonRpcException {

// --------------------------- CONSTRUCTORS ---------------------------

    public JsonRpcInternalErrorException(String message) {
        super(message);
    }

    public JsonRpcInternalErrorException(String message, Exception cause) {
        super(message, cause);
    }

}
