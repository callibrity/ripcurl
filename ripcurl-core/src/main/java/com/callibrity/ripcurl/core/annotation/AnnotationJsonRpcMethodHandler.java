package com.callibrity.ripcurl.core.annotation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ripcurl.core.invoke.JsonMethodInvoker;
import com.ripcurl.core.spi.JsonRpcMethodHandler;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

import static java.util.Optional.ofNullable;

public class AnnotationJsonRpcMethodHandler implements JsonRpcMethodHandler {

// ------------------------------ FIELDS ------------------------------

    private final String name;
    private final JsonMethodInvoker invoker;

// --------------------------- CONSTRUCTORS ---------------------------

    AnnotationJsonRpcMethodHandler(ObjectMapper mapper, Object targetObject, Method method) {
        this.invoker = new JsonMethodInvoker(mapper, targetObject, method);
        var annotation = method.getAnnotation(JsonRpc.class);
        this.name = ofNullable(annotation.value())
                .map(StringUtils::stripToNull)
                .orElseGet(() -> String.format("%s.%s", ClassUtils.getSimpleName(targetObject), method.getName()));
    }

// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface JsonRpcMethodHandler ---------------------

    @Override
    public String methodName() {
        return name;
    }

    @Override
    public JsonNode call(JsonNode params) {
        return invoker.invoke(params);
    }

}
