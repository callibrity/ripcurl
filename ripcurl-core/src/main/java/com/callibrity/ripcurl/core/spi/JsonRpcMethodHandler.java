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
package com.callibrity.ripcurl.core.spi;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcMethodHandler {
    /**
     * Returns the name of the JSON-RPC method.
     *
     * @return the name of the JSON-RPC method
     */
    String methodName();

    /**
     * Executes the JSON-RPC method with the given parameters.
     *
     * @param params the parameters to be passed to the JSON-RPC method, represented as a JsonNode
     * @return the result of the JSON-RPC method execution, represented as a JsonNode
     */
    JsonNode call(JsonNode params);
}
