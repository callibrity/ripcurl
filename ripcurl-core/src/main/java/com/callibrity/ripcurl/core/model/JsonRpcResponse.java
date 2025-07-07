package com.callibrity.ripcurl.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    public String jsonrpc = "2.0";
    public Object result;
    public Error error;
    public Object id;

    public static class Error {
        public int code;
        public String message;
        public Object data;
    }
}
