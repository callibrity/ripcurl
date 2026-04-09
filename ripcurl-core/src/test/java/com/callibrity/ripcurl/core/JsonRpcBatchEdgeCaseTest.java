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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodProviderFactory;
import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcDispatcher;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.def.DefaultMethodInvokerFactory;
import org.jwcarman.methodical.jackson3.Jackson3ParameterResolver;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.IntNode;

class JsonRpcBatchEdgeCaseTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void batchShouldThrowWhenCallingThreadIsInterrupted() {
    var handlerStarted = new CountDownLatch(1);
    var blockingLatch = new CountDownLatch(1);
    var service = new BlockingService(handlerStarted, blockingLatch);
    var dispatcher = createDispatcher(service);
    var thrown = new AtomicReference<Throwable>();

    // Run dispatch on a separate thread so we can interrupt it
    var dispatchThread =
        Thread.ofVirtual()
            .start(
                () -> {
                  try {
                    dispatcher.dispatchBatch(
                        List.of(new JsonRpcRequest("2.0", "block", null, IntNode.valueOf(1))));
                  } catch (Throwable t) {
                    thrown.set(t);
                  }
                });

    // Wait for the handler to actually be blocked, then interrupt the dispatch thread
    await().atMost(Duration.ofSeconds(2)).until(() -> handlerStarted.getCount() == 0);
    dispatchThread.interrupt();
    blockingLatch.countDown(); // release the handler so everything cleans up

    // Wait for the dispatch thread to finish and verify the exception
    await()
        .atMost(Duration.ofSeconds(2))
        .untilAsserted(
            () -> {
              assertThat(thrown.get()).isInstanceOf(IllegalStateException.class);
              assertThat(thrown.get().getMessage()).contains("interrupted");
            });
  }

  @Test
  void notificationsAreProcessedAsynchronously() {
    var processed = new AtomicBoolean(false);
    var service = new TrackingService(processed);
    var dispatcher = createDispatcher(service);

    dispatcher.dispatchBatch(
        List.of(
            new JsonRpcRequest("2.0", "track", null, null),
            new JsonRpcRequest("2.0", "track", null, IntNode.valueOf(1))));

    await().atMost(Duration.ofSeconds(2)).untilTrue(processed);
  }

  private JsonRpcDispatcher createDispatcher(Object service) {
    var factory =
        new DefaultAnnotationJsonRpcMethodProviderFactory(
            MAPPER,
            new DefaultMethodInvokerFactory(List.of(new Jackson3ParameterResolver(MAPPER))));
    return new DefaultJsonRpcDispatcher(List.of(factory.create(service)));
  }

  public static class BlockingService {
    private final CountDownLatch handlerStarted;
    private final CountDownLatch blockingLatch;

    public BlockingService(CountDownLatch handlerStarted, CountDownLatch blockingLatch) {
      this.handlerStarted = handlerStarted;
      this.blockingLatch = blockingLatch;
    }

    @JsonRpcMethod("block")
    public String block() throws InterruptedException {
      handlerStarted.countDown();
      blockingLatch.await();
      return "done";
    }
  }

  public static class TrackingService {
    private final AtomicBoolean processed;

    public TrackingService(AtomicBoolean processed) {
      this.processed = processed;
    }

    @JsonRpcMethod("track")
    public String track() {
      processed.set(true);
      return "tracked";
    }
  }
}
