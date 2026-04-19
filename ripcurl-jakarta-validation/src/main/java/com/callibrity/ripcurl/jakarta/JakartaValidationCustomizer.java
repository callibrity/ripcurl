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
package com.callibrity.ripcurl.jakarta;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerConfig;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerCustomizer;
import java.util.Objects;
import org.jwcarman.methodical.jakarta.JakartaValidationInterceptor;

/**
 * Attaches Methodical's {@link JakartaValidationInterceptor} to every {@code @JsonRpcMethod}
 * handler so Jakarta Bean Validation constraints on handler parameters and return values fire on
 * every dispatch. A {@code ConstraintViolationException} thrown by the interceptor is translated to
 * a {@code -32602 Invalid params} response by {@link ConstraintViolationExceptionTranslator}.
 */
public final class JakartaValidationCustomizer implements JsonRpcMethodHandlerCustomizer {

  private final JakartaValidationInterceptor interceptor;

  public JakartaValidationCustomizer(JakartaValidationInterceptor interceptor) {
    this.interceptor = Objects.requireNonNull(interceptor, "interceptor");
  }

  @Override
  public void customize(JsonRpcMethodHandlerConfig config) {
    config.interceptor(interceptor);
  }
}
