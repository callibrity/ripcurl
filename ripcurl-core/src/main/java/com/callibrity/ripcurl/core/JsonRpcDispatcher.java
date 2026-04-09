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

import com.callibrity.ripcurl.core.exception.JsonRpcException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public interface JsonRpcDispatcher {

  /**
   * Dispatches a single JSON-RPC request. Returns null for notifications.
   *
   * @param request the JSON-RPC request
   * @return the response, or null for notifications
   */
  JsonRpcResponse dispatch(JsonRpcRequest request);

  /**
   * Dispatches a batch of JSON-RPC requests. Notifications are fired on virtual threads without
   * waiting. Requests are dispatched concurrently on virtual threads and joined before returning.
   * Per the JSON-RPC 2.0 spec, the server MAY process batch items concurrently in any order.
   *
   * @param requests the batch of JSON-RPC requests (must not be empty)
   * @return the list of responses (requests only, no notifications), may be empty if all are
   *     notifications
   * @throws IllegalArgumentException if the batch is empty
   */
  default List<JsonRpcResponse> dispatchBatch(List<JsonRpcRequest> requests) {
    if (requests.isEmpty()) {
      throw new IllegalArgumentException("Batch must not be empty");
    }

    // Fire-and-forget notifications
    requests.stream()
        .filter(req -> req.id() == null)
        .forEach(req -> Thread.ofVirtual().start(() -> dispatch(req)));

    // Dispatch requests concurrently and collect results
    var requestsWithId = requests.stream().filter(req -> req.id() != null).toList();
    if (requestsWithId.isEmpty()) {
      return List.of();
    }

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures =
          requestsWithId.stream().map(req -> executor.submit(() -> dispatch(req))).toList();
      return futures.stream()
          .map(
              f -> {
                try {
                  return f.get();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  throw new IllegalStateException("Batch dispatch interrupted", e);
                } catch (ExecutionException e) {
                  if (e.getCause() instanceof RuntimeException re) {
                    throw re;
                  }
                  throw new JsonRpcException(
                      JsonRpcProtocol.INTERNAL_ERROR, e.getCause().getMessage());
                }
              })
          .toList();
    }
  }
}
