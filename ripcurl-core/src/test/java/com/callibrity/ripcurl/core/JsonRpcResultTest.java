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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class JsonRpcResultTest {

  private static final StringNode TEST_ID = StringNode.valueOf("1");

  @Test
  void convenienceConstructorSetsVersion() {
    var result = new JsonRpcResult(NullNode.getInstance(), TEST_ID);
    assertThat(result.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(result.result()).isEqualTo(NullNode.getInstance());
    assertThat(result.id()).isEqualTo(TEST_ID);
    assertThat(result.metadata()).isEmpty();
  }

  @Test
  void withMetadataReturnsNewInstance() {
    var original = new JsonRpcResult(NullNode.getInstance(), TEST_ID);
    var updated = original.withMetadata("key", "value");
    assertThat(original.metadata()).isEmpty();
    assertThat(updated.metadata()).containsEntry("key", "value");
  }

  @Test
  void withMetadataPreservesExisting() {
    var result =
        new JsonRpcResult(NullNode.getInstance(), TEST_ID)
            .withMetadata("first", "one")
            .withMetadata("second", "two");
    assertThat(result.metadata()).containsEntry("first", "one").containsEntry("second", "two");
  }

  @Test
  void getMetadataReturnsTypedValue() {
    var result = new JsonRpcResult(NullNode.getInstance(), TEST_ID).withMetadata("count", 42);
    assertThat(result.getMetadata("count", Integer.class)).hasValue(42);
  }

  @Test
  void getMetadataReturnsEmptyForMissing() {
    var result = new JsonRpcResult(NullNode.getInstance(), TEST_ID);
    assertThat(result.getMetadata("missing", String.class)).isEmpty();
  }

  @Test
  void metadataNotSerializedToJson() {
    var result =
        new JsonRpcResult(StringNode.valueOf("hello"), TEST_ID).withMetadata("secret", "hidden");
    var json = new tools.jackson.databind.ObjectMapper().writeValueAsString(result);
    assertThat(json)
        .doesNotContain("secret")
        .doesNotContain("hidden")
        .doesNotContain("metadata")
        .contains("hello");
  }
}
