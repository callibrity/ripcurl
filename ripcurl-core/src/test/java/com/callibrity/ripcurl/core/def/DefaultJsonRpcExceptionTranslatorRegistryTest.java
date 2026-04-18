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
package com.callibrity.ripcurl.core.def;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultJsonRpcExceptionTranslatorRegistryTest {

  @Nested
  class Construction {

    @Test
    void rejects_null_translator_list() {
      assertThatThrownBy(() -> new DefaultJsonRpcExceptionTranslatorRegistry(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("translators");
    }

    @Test
    void accepts_an_empty_translator_list() {
      // With the built-in fallback owned by the registry itself, zero registered translators is
      // a valid configuration — every exception lands on the internal -32603 Internal error path.
      var registry = new DefaultJsonRpcExceptionTranslatorRegistry(List.of());
      JsonRpcErrorDetail detail = registry.translate(new RuntimeException("boom"));
      assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INTERNAL_ERROR);
      // The fallback deliberately doesn't leak exception.getMessage() — the thrown message may
      // contain implementation details the server shouldn't expose to the client.
      assertThat(detail.message()).isEqualTo("Internal error");
    }

    @Test
    void rejects_two_translators_targeting_the_same_exact_exception_type() {
      // "Last registered wins" silently changes with bean-discovery order. Force users to
      // consciously exclude built-ins with @ConditionalOnMissingBean rather than stacking.
      List<JsonRpcExceptionTranslator<?>> duplicates =
          List.of(
              new AlternateIllegalArgumentTranslator(), new IllegalArgumentExceptionTranslator());
      assertThatThrownBy(() -> new DefaultJsonRpcExceptionTranslatorRegistry(duplicates))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(IllegalArgumentException.class.getName());
    }

    @Test
    void rejects_lambda_translator_because_generic_is_erased() {
      // A lambda like `e -> ...` has no captured-subclass signature the reflection can inspect,
      // so the exception type parameter resolves to Object, not a concrete Exception subclass.
      // Guard against this at registration rather than letting a translator sit there unreachable.
      JsonRpcExceptionTranslator<Exception> lambda =
          exception -> new JsonRpcErrorDetail(0, exception.getMessage());
      List<JsonRpcExceptionTranslator<?>> translators = List.of(lambda);
      assertThatThrownBy(() -> new DefaultJsonRpcExceptionTranslatorRegistry(translators))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unable to resolve the exception type parameter");
    }
  }

  @Nested
  class Lookup {

    @Test
    void exact_class_match_uses_its_translator() {
      var registry =
          new DefaultJsonRpcExceptionTranslatorRegistry(
              List.of(
                  new DefaultJsonRpcExceptionTranslator(),
                  new IllegalArgumentExceptionTranslator()));
      JsonRpcErrorDetail detail = registry.translate(new IllegalArgumentException("bad"));
      assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
      assertThat(detail.message()).isEqualTo("bad");
    }

    @Test
    void subclass_walks_up_to_the_nearest_registered_supertype() {
      // NumberFormatException extends IllegalArgumentException. No NumberFormatException
      // translator is registered, so the superclass walk must land on the IAE translator rather
      // than falling all the way through to the built-in fallback.
      var registry =
          new DefaultJsonRpcExceptionTranslatorRegistry(
              List.of(new IllegalArgumentExceptionTranslator()));
      JsonRpcErrorDetail detail = registry.translate(new NumberFormatException("not a number"));
      assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
    }

    @Test
    void unhandled_type_falls_through_to_the_built_in_internal_error() {
      // IOException isn't reachable from any registered specific translator. With no user-supplied
      // catch-all, the registry's own built-in fallback takes over.
      var registry =
          new DefaultJsonRpcExceptionTranslatorRegistry(
              List.of(new IllegalArgumentExceptionTranslator()));
      JsonRpcErrorDetail detail = registry.translate(new IOException("disk gone"));
      assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INTERNAL_ERROR);
      assertThat(detail.message()).isEqualTo("Internal error");
    }

    @Test
    void specific_translator_wins_over_the_built_in_for_the_same_throw() {
      // JsonRpcException is registered explicitly; throwing one must ride its own translator
      // (preserving its code), not the built-in fallback.
      var registry =
          new DefaultJsonRpcExceptionTranslatorRegistry(
              List.of(new DefaultJsonRpcExceptionTranslator()));
      JsonRpcErrorDetail detail =
          registry.translate(new JsonRpcException(-32099, "application-specific"));
      assertThat(detail.code()).isEqualTo(-32099);
      assertThat(detail.message()).isEqualTo("application-specific");
    }

    @Test
    void user_exception_translator_supersedes_the_built_in_fallback() {
      // Registering a JsonRpcExceptionTranslator<Exception> replaces the internal -32603 default
      // — the superclass walk reaches Exception.class before falling through to the built-in, so
      // the user's translator wins for any type not covered by a more-specific registration.
      var registry =
          new DefaultJsonRpcExceptionTranslatorRegistry(List.of(new CustomExceptionTranslator()));
      JsonRpcErrorDetail detail = registry.translate(new RuntimeException("anything"));
      assertThat(detail.code()).isEqualTo(-29999);
      assertThat(detail.message()).isEqualTo("custom catch-all: anything");
    }

    @Test
    void user_supplied_translator_overrides_the_built_in_for_the_same_type() {
      // When a user registers their own IllegalArgumentException translator INSTEAD of the
      // built-in (via @ConditionalOnMissingBean in real life), it must take precedence. In this
      // unit test we simply omit the built-in and register only the user's.
      var registry =
          new DefaultJsonRpcExceptionTranslatorRegistry(
              List.of(new AlternateIllegalArgumentTranslator()));
      JsonRpcErrorDetail detail = registry.translate(new IllegalArgumentException("x"));
      assertThat(detail.code()).isEqualTo(-31000);
    }

    @Test
    void rejects_null_exception() {
      var registry = new DefaultJsonRpcExceptionTranslatorRegistry(List.of());
      assertThatThrownBy(() -> registry.translate(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("exception");
    }
  }

  /** User-provided override used by the override-precedence test. */
  private static class AlternateIllegalArgumentTranslator
      implements JsonRpcExceptionTranslator<IllegalArgumentException> {
    @Override
    public JsonRpcErrorDetail translate(IllegalArgumentException exception) {
      return new JsonRpcErrorDetail(-31000, exception.getMessage());
    }
  }

  /** User-provided catch-all used by the user-catch-all test. */
  private static class CustomExceptionTranslator implements JsonRpcExceptionTranslator<Exception> {
    @Override
    public JsonRpcErrorDetail translate(Exception exception) {
      return new JsonRpcErrorDetail(-29999, "custom catch-all: " + exception.getMessage());
    }
  }
}
