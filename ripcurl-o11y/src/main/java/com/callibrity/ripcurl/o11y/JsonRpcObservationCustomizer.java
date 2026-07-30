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

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerCustomizer;

/**
 * The customizer responsible for attaching <em>the</em> per-dispatch Micrometer observation to
 * every {@code @JsonRpcMethod} handler. Exactly one bean of this type should exist in a context:
 * the autoconfiguration registers {@link DefaultJsonRpcObservationCustomizer} (JSON-RPC
 * semantic-convention attributes, observation name {@code jsonrpc.server}) only when no other
 * {@code JsonRpcObservationCustomizer} bean is present.
 *
 * <p>Applications and frameworks that observe JSON-RPC dispatch under a more specific convention —
 * a domain protocol layered over JSON-RPC, say — implement this interface and register their
 * customizer as a bean. The default then backs off entirely: one observation per dispatch, owned by
 * whichever convention is most specific, rather than two nested observations over the same
 * interval.
 */
public interface JsonRpcObservationCustomizer extends JsonRpcMethodHandlerCustomizer {}
