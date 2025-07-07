package com.callibrity.ripcurl.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonRpcInternalErrorExceptionTest {

    @Test
    void constructorShouldSetMessage() {
        var message = "An internal error occurred";
        var exception = new JsonRpcInternalErrorException(message);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void constructorShouldSetMessageAndCause() {
        var message = "An internal error occurred";
        var cause = new RuntimeException("Root cause");
        var exception = new JsonRpcInternalErrorException(message, cause);

        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}