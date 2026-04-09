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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public interface JsonRpcDispatcher {

  /**
   * Dispatches a single JSON-RPC request. Returns null for notifications.
   *
   * @param request the JSON-RPC request (call or notification)
   * @return the response, or null for notifications
   */
  JsonRpcResponse dispatch(JsonRpcRequest request);

  /**
   * Dispatches a batch of JSON-RPC requests. Notifications are fired on virtual threads without
   * waiting. Calls are dispatched concurrently via {@code invokeAll} on a virtual-thread-per-task
   * executor. Per the JSON-RPC 2.0 spec, the server MAY process batch items concurrently in any
   * order.
   *
   * @param requests the batch of JSON-RPC requests (must not be empty)
   * @return the list of responses (calls only, no notifications), may be empty if all are
   *     notifications
   * @throws IllegalArgumentException if the batch is empty
   */
  default List<JsonRpcResponse> dispatchBatch(List<JsonRpcRequest> requests) {
    if (requests.isEmpty()) {
      throw new IllegalArgumentException("Batch must not be empty");
    }

    // Fire-and-forget notifications
    requests.stream()
        .filter(JsonRpcNotification.class::isInstance)
        .forEach(req -> Thread.ofVirtual().start(() -> dispatch(req)));

    // Dispatch calls concurrently and collect results
    var callables =
        requests.stream()
            .filter(JsonRpcCall.class::isInstance)
            .<Callable<JsonRpcResponse>>map(req -> () -> dispatch(req))
            .toList();
    if (callables.isEmpty()) {
      return List.of();
    }

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      return executor.invokeAll(callables).stream().map(Future::resultNow).toList();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Batch dispatch interrupted", e);
    }
  }
}
