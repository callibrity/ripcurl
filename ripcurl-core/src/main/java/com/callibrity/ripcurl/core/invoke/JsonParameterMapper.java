package com.callibrity.ripcurl.core.invoke;

import com.callibrity.ripcurl.core.exception.JsonRpcInvalidParamsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.io.IOException;
import java.lang.reflect.Parameter;

public class JsonParameterMapper {

// ------------------------------ FIELDS ------------------------------

    private final String parameterName;
    private final ObjectMapper mapper;
    private final ObjectReader reader;

// --------------------------- CONSTRUCTORS ---------------------------

    public JsonParameterMapper(ObjectMapper mapper, Object targetObject, Parameter parameter) {
        this.parameterName = parameter.getName();
        this.mapper = mapper;
        this.reader = mapper.readerFor(TypeUtils.getRawType(parameter.getParameterizedType(), targetObject.getClass()));
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getParameterName() {
        return parameterName;
    }

// -------------------------- OTHER METHODS --------------------------

    public Object mapParameter(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        try {
            return reader.readValue(mapper.treeAsTokens(node));
        } catch (IOException e) {
            throw new JsonRpcInvalidParamsException(String.format("Unable to read parameter \"%s\" value (%s).", parameterName, e.getMessage()));
        }
    }

}
