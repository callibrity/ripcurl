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

import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.JsonRpcResponse;

public interface JsonRpcMethod {

  /**
   * Returns the name of the JSON-RPC method.
   *
   * @return the name of the JSON-RPC method
   */
  String methodName();

  /**
   * Executes the JSON-RPC method with the given request.
   *
   * @param request the JSON-RPC request
   * @return the JSON-RPC response
   */
  JsonRpcResponse call(JsonRpcRequest request);
}
