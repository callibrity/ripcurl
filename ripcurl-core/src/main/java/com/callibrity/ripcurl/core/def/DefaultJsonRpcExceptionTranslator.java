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
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;

/**
 * Translates {@link JsonRpcException} to a {@link JsonRpcErrorDetail} using the exception's
 * self-declared JSON-RPC error code. Applications throw {@code JsonRpcException} from method
 * handlers when they want to control the error code returned to the client; this translator
 * preserves that contract.
 */
public class DefaultJsonRpcExceptionTranslator
    implements JsonRpcExceptionTranslator<JsonRpcException> {

  @Override
  public JsonRpcErrorDetail translate(JsonRpcException exception) {
    return new JsonRpcErrorDetail(exception.getCode(), exception.getMessage());
  }
}
