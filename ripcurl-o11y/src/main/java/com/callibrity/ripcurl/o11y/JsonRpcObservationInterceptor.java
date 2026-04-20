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

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.jwcarman.methodical.MethodInterceptor;
import org.jwcarman.methodical.MethodInvocation;
import tools.jackson.databind.JsonNode;

/**
 * Wraps a single JSON-RPC dispatch in a Micrometer {@link Observation} carrying OpenTelemetry
 * JSON-RPC semantic-convention attributes per
 * https://opentelemetry.io/docs/specs/semconv/rpc/json-rpc/.
 *
 * <p>Attached once per {@code @JsonRpcMethod} handler via {@code JsonRpcMethodHandlerCustomizer} —
 * the JSON-RPC method name is closed over at construction so the hot path does no reflection and no
 * per-call branching.
 *
 * <p>Observation name: {@code jsonrpc.server} — becomes the histogram metric {@code
 * jsonrpc.server.duration}. Contextual (span) name: the JSON-RPC method (e.g. {@code tools/call},
 * {@code echo}).
 *
 * <p>Attributes emitted:
 *
 * <ul>
 *   <li>{@code rpc.system.name=jsonrpc} — always
 *   <li>{@code jsonrpc.protocol.version=2.0} — always (ripcurl is 2.0-only)
 *   <li>{@code jsonrpc.request.id} — high-cardinality; emitted when {@link
 *       JsonRpcDispatcher#CURRENT_REQUEST} is bound AND the request is a {@link JsonRpcCall}
 *       (notifications have no id)
 *   <li>{@code rpc.response.status_code} — failure only; the JSON-RPC error code as a string (e.g.
 *       {@code "-32602"}). Per the JSON-RPC semconv definition this attribute carries the {@code
 *       error.code} from the response, so it's unset on success
 *   <li>{@code error.type} — failure only; the same JSON-RPC error code string. Per semconv, {@code
 *       error.type} MUST NOT be set when the operation succeeded
 * </ul>
 *
 * <p>The JSON-RPC error code is obtained by running the caught exception through ripcurl's {@link
 * JsonRpcExceptionTranslatorRegistry} — the same registry the dispatcher uses to build the
 * wire-level {@link com.callibrity.ripcurl.core.JsonRpcError}. That way the code on the observation
 * always matches the code the client sees, regardless of which Java exception the handler threw.
 *
 * <p>Known gap: span kind is the default {@code INTERNAL}, not {@code SERVER}. Setting {@code
 * SERVER} kind via Micrometer Observation requires a {@code ReceiverContext} subclass and
 * tracing-specific plumbing; deferring until a real consumer needs it. The ambient HTTP span (when
 * ripcurl is behind Spring MVC) already carries {@code SERVER} kind.
 */
public final class JsonRpcObservationInterceptor implements MethodInterceptor<JsonNode> {

  /** Observation name — becomes the histogram metric {@code jsonrpc.server.duration}. */
  public static final String OBSERVATION_NAME = "jsonrpc.server";

  private final ObservationRegistry registry;
  private final JsonRpcExceptionTranslatorRegistry translators;
  private final String jsonRpcMethod;

  public JsonRpcObservationInterceptor(
      ObservationRegistry registry,
      JsonRpcExceptionTranslatorRegistry translators,
      String jsonRpcMethod) {
    this.registry = registry;
    this.translators = translators;
    this.jsonRpcMethod = jsonRpcMethod;
  }

  @Override
  public Object intercept(MethodInvocation<? extends JsonNode> invocation) {
    Observation observation =
        Observation.createNotStarted(OBSERVATION_NAME, registry)
            .contextualName(jsonRpcMethod)
            .lowCardinalityKeyValue("rpc.system.name", "jsonrpc")
            .lowCardinalityKeyValue("jsonrpc.protocol.version", JsonRpcProtocol.VERSION);

    if (JsonRpcDispatcher.CURRENT_REQUEST.isBound()
        && JsonRpcDispatcher.CURRENT_REQUEST.get() instanceof JsonRpcCall call) {
      observation.highCardinalityKeyValue("jsonrpc.request.id", call.id().asString());
    }

    observation.start();
    try (Observation.Scope ignored = observation.openScope()) {
      return invocation.proceed();
    } catch (RuntimeException e) {
      JsonRpcErrorDetail detail = translators.translate(e);
      String code = Integer.toString(detail.code());
      observation.lowCardinalityKeyValue("rpc.response.status_code", code);
      observation.lowCardinalityKeyValue("error.type", code);
      observation.error(e);
      throw e;
    } finally {
      observation.stop();
    }
  }
}
