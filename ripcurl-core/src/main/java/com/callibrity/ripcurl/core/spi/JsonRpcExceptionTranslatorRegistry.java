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

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;

/**
 * Lookup facade over the registered {@link JsonRpcExceptionTranslator} beans. The dispatcher asks
 * this registry for the appropriate error payload whenever an invocation throws.
 *
 * <p>Implementations must always return a non-null {@link JsonRpcErrorDetail}. Because ripcurl
 * ships a catch-all translator bound to {@link Exception}, no thrown exception can go unmatched
 * unless that default is deliberately removed by the application.
 */
public interface JsonRpcExceptionTranslatorRegistry {

  /**
   * Finds the most-specific translator for {@code exception} and invokes it.
   *
   * @param exception the exception raised during dispatch
   * @return the error detail to return in the JSON-RPC response
   */
  JsonRpcErrorDetail translate(Exception exception);
}
