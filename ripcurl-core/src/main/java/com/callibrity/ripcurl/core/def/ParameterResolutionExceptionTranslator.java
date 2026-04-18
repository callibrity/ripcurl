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
import org.jwcarman.methodical.ParameterResolutionException;

/**
 * Translates methodical's {@link ParameterResolutionException} — thrown when a parameter resolver
 * cannot produce a value for an invocation argument — to {@code -32602 Invalid params}. The
 * exception's own message is passed through, since resolvers typically include the parameter name
 * and the reason the value couldn't be produced.
 */
public class ParameterResolutionExceptionTranslator
    implements JsonRpcExceptionTranslator<ParameterResolutionException> {

  @Override
  public JsonRpcErrorDetail translate(ParameterResolutionException exception) {
    return new JsonRpcErrorDetail(JsonRpcProtocol.INVALID_PARAMS, exception.getMessage());
  }
}
