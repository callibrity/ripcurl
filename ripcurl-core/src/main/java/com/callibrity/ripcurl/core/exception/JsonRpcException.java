package com.callibrity.ripcurl.core.exception;

public abstract class JsonRpcException extends RuntimeException {

// --------------------------- CONSTRUCTORS ---------------------------

    protected JsonRpcException(String message) {
        super(message);
    }

    protected JsonRpcException(String message, Exception cause) {
        super(message, cause);
    }

}
