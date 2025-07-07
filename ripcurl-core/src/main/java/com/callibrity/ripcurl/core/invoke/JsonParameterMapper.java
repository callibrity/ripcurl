/*
 * Copyright © 2025 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
