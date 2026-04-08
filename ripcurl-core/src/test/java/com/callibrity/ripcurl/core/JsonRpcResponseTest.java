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

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class JsonRpcResponseTest {

  private static final StringNode TEST_ID = StringNode.valueOf("1");

  @Test
  void convenienceConstructorSetsVersion() {
    var response = new JsonRpcResponse(NullNode.getInstance(), TEST_ID);

    assertThat(response.jsonrpc()).isEqualTo(JsonRpcProtocol.VERSION);
    assertThat(response.result()).isEqualTo(NullNode.getInstance());
    assertThat(response.id()).isEqualTo(TEST_ID);
    assertThat(response.metadata()).isEmpty();
  }

  @Test
  void threeArgConstructorSetsEmptyMetadata() {
    var response = new JsonRpcResponse("2.0", NullNode.getInstance(), TEST_ID);

    assertThat(response.metadata()).isEmpty();
  }

  @Test
  void withMetadataReturnsNewInstanceWithEntry() {
    var original = new JsonRpcResponse(NullNode.getInstance(), TEST_ID);
    var updated = original.withMetadata("key", "value");

    assertThat(original.metadata()).isEmpty();
    assertThat(updated.metadata()).containsEntry("key", "value");
    assertThat(updated.result()).isEqualTo(original.result());
    assertThat(updated.id()).isEqualTo(original.id());
  }

  @Test
  void withMetadataPreservesExistingEntries() {
    var response =
        new JsonRpcResponse(NullNode.getInstance(), TEST_ID)
            .withMetadata("first", "one")
            .withMetadata("second", "two");

    assertThat(response.metadata()).containsEntry("first", "one");
    assertThat(response.metadata()).containsEntry("second", "two");
  }

  @Test
  void getMetadataReturnsTypedValue() {
    var response = new JsonRpcResponse(NullNode.getInstance(), TEST_ID).withMetadata("count", 42);

    assertThat(response.getMetadata("count", Integer.class)).hasValue(42);
  }

  @Test
  void getMetadataReturnsEmptyForMissingKey() {
    var response = new JsonRpcResponse(NullNode.getInstance(), TEST_ID);

    assertThat(response.getMetadata("missing", String.class)).isEmpty();
  }

  @Test
  void getMetadataReturnsEmptyForNullValue() {
    var response = new JsonRpcResponse("2.0", NullNode.getInstance(), TEST_ID, Map.of());

    assertThat(response.getMetadata("anything", String.class)).isEmpty();
  }

  @Test
  void metadataIsNotSerializedToJson() {
    var response =
        new JsonRpcResponse(StringNode.valueOf("hello"), TEST_ID).withMetadata("secret", "hidden");

    var mapper = new tools.jackson.databind.ObjectMapper();
    var json = mapper.writeValueAsString(response);

    assertThat(json)
        .doesNotContain("secret")
        .doesNotContain("hidden")
        .doesNotContain("metadata")
        .contains("hello");
  }
}
