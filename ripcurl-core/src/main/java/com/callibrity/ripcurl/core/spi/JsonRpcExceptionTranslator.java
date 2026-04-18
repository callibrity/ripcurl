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
 * Strategy for translating an {@link Exception} raised during JSON-RPC method invocation into the
 * {@link JsonRpcErrorDetail} the dispatcher returns to the caller. Modeled after JAX-RS {@code
 * ExceptionMapper<E>}: the handled exception type is captured in the generic parameter and resolved
 * reflectively at registration time, so implementers write one method and never have to restate the
 * type.
 *
 * <p>The bound is {@link Exception} rather than {@link Throwable} because the dispatcher only
 * catches {@code Exception} — JVM-fatal {@link Error}s (OOM, StackOverflow, LinkageError) propagate
 * unchanged, since attempting to serialize a JSON-RPC response in that state is unsafe.
 *
 * <p>Register translators as Spring beans — the autoconfigure module picks them up and feeds them
 * into the {@link JsonRpcExceptionTranslatorRegistry}. When multiple translators could match a
 * thrown exception, the one bound to the most specific exception type wins; ties are broken by
 * registration order.
 *
 * <p>Ripcurl ships default translators for {@link Exception} (catch-all returning {@code -32603
 * Internal error}), {@code JsonRpcException}, {@link IllegalArgumentException}, and methodical's
 * {@code ParameterResolutionException}. Applications override any of these by registering their own
 * translator for the same type.
 *
 * @param <E> the exception type this translator handles
 */
@FunctionalInterface
public interface JsonRpcExceptionTranslator<E extends Exception> {

  /**
   * Produces the JSON-RPC error payload for the given exception.
   *
   * @param exception the exception raised during dispatch
   * @return the error detail to return in the JSON-RPC response
   */
  JsonRpcErrorDetail translate(E exception);
}
