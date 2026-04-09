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
package com.callibrity.ripcurl.core;

import tools.jackson.databind.JsonNode;

/**
 * Sealed base for JSON-RPC 2.0 request types. A "Request object" in the spec encompasses both calls
 * (with an id, expecting a response) and notifications (without an id, fire-and-forget).
 */
public sealed interface JsonRpcRequest extends JsonRpcMessage
    permits JsonRpcCall, JsonRpcNotification {

  String method();

  JsonNode params();
}
