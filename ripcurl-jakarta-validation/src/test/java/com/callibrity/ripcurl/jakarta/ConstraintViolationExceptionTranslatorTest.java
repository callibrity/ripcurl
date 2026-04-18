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
package com.callibrity.ripcurl.jakarta;

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Exercises the translator against real {@link ConstraintViolation}s produced by Hibernate
 * Validator — hand-rolling a fake ConstraintViolation implementation would miss the per-field
 * formatting differences Jakarta's providers generate, which are the whole reason this integration
 * exists.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConstraintViolationExceptionTranslatorTest {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  record Signup(@NotBlank String name, @Min(0) int age) {}

  @Test
  void maps_to_invalid_params_and_emits_per_violation_data_array() {
    Set<ConstraintViolation<Signup>> violations = VALIDATOR.validate(new Signup("", -1));
    var exception = new ConstraintViolationException(violations);

    JsonRpcErrorDetail detail = new ConstraintViolationExceptionTranslator().translate(exception);

    assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
    assertThat(detail.message()).isEqualTo("Invalid params");
    assertThat(detail.data()).isNotNull();
    assertThat(detail.data().isArray()).isTrue();
    assertThat(detail.data().size()).isEqualTo(2);
  }

  @Test
  void each_violation_entry_carries_field_and_message_only() {
    Set<ConstraintViolation<Signup>> violations = VALIDATOR.validate(new Signup("", 10));
    var exception = new ConstraintViolationException(violations);

    JsonRpcErrorDetail detail = new ConstraintViolationExceptionTranslator().translate(exception);

    var entry = detail.data().get(0);
    assertThat(entry.get("field").asString()).isEqualTo("name");
    assertThat(entry.get("message").asString()).isEqualTo("must not be blank");
    // invalidValue is deliberately omitted — the rejected input may be sensitive (passwords,
    // tokens, PII), and reflecting it back risks leaking secrets through error responses.
    assertThat(entry.get("invalidValue")).isNull();
  }

  @Test
  void empty_violation_set_produces_an_empty_data_array() {
    // Contrived — a ConstraintViolationException is normally only thrown when violations exist —
    // but a defensive null/empty handling is cheap and prevents surprise NPEs if a caller
    // constructs one manually.
    var exception = new ConstraintViolationException(Set.of());

    JsonRpcErrorDetail detail = new ConstraintViolationExceptionTranslator().translate(exception);

    assertThat(detail.code()).isEqualTo(JsonRpcProtocol.INVALID_PARAMS);
    assertThat(detail.data().isArray()).isTrue();
    assertThat(detail.data().size()).isZero();
  }
}
