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
package com.callibrity.ripcurl.core.annotation;

import com.callibrity.ripcurl.core.spi.JsonRpcMethodProvider;

public interface AnnotationJsonRpcMethodProviderFactory {
    /**
     * Creates a {@link JsonRpcMethodProvider} for the given target object. All methods annotated with
     * {@link JsonRpc} will be registered as JSON-RPC methods.
     *
     * @param targetObject the object containing methods annotated with {@link JsonRpc}
     * @return a {@link JsonRpcMethodProvider} that provides access to the JSON-RPC methods defined in the target object
     */
    JsonRpcMethodProvider create(Object targetObject);
}
