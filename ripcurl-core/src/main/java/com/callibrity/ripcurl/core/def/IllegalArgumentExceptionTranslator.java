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
package com.callibrity.ripcurl.core.def;

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;

/**
 * Translates {@link IllegalArgumentException} to {@code -32602 Invalid params}. The Java spec
 * dedicates this exception to "an illegal or inappropriate argument passed to a method," which is
 * exactly the contract of {@code -32602} — so handler code that does {@code throw new
 * IllegalArgumentException("name is required")} gets a correctly-coded JSON-RPC response without
 * having to know about {@code JsonRpcException}.
 */
public class IllegalArgumentExceptionTranslator
    implements JsonRpcExceptionTranslator<IllegalArgumentException> {

  @Override
  public JsonRpcErrorDetail translate(IllegalArgumentException exception) {
    return new JsonRpcErrorDetail(JsonRpcProtocol.INVALID_PARAMS, exception.getMessage());
  }
}
