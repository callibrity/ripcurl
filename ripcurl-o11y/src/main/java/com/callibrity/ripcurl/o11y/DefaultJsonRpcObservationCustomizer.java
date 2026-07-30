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
package com.callibrity.ripcurl.o11y;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerConfig;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import io.micrometer.observation.ObservationRegistry;

/**
 * Default {@link JsonRpcObservationCustomizer}: attaches a {@link JsonRpcObservationInterceptor} to
 * every {@code @JsonRpcMethod} handler, producing one {@code jsonrpc.server} observation per
 * dispatch with OpenTelemetry JSON-RPC semantic-convention attributes. Registered by the
 * autoconfiguration only when no other {@link JsonRpcObservationCustomizer} bean exists.
 */
public final class DefaultJsonRpcObservationCustomizer implements JsonRpcObservationCustomizer {

  private final ObservationRegistry registry;
  private final JsonRpcExceptionTranslatorRegistry translators;

  public DefaultJsonRpcObservationCustomizer(
      ObservationRegistry registry, JsonRpcExceptionTranslatorRegistry translators) {
    this.registry = registry;
    this.translators = translators;
  }

  @Override
  public void customize(JsonRpcMethodHandlerConfig config) {
    config.interceptor(new JsonRpcObservationInterceptor(registry, translators, config.name()));
  }

  @Override
  public String toString() {
    return "Attaches the default '"
        + JsonRpcObservationInterceptor.OBSERVATION_NAME
        + "' observation (OpenTelemetry JSON-RPC semconv) to every @JsonRpcMethod handler";
  }
}
