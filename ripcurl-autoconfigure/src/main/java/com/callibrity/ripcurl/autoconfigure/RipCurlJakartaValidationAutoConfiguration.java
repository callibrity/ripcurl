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
package com.callibrity.ripcurl.autoconfigure;

import com.callibrity.ripcurl.autoconfigure.aot.RipCurlJakartaValidationRuntimeHints;
import com.callibrity.ripcurl.jakarta.ConstraintViolationExceptionTranslator;
import jakarta.validation.ConstraintViolationException;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Registers a {@link ConstraintViolationExceptionTranslator} so Jakarta validation failures on
 * {@code @JsonRpcMethod} parameters surface as {@code -32602 Invalid params} errors with the
 * per-violation detail in the response's {@code data} field.
 *
 * <p>Triggers only when BOTH {@link ConstraintViolationException} (jakarta.validation-api) and
 * {@link ConstraintViolationExceptionTranslator} (ripcurl-jakarta-validation, an optional
 * dependency of this module) are on the classpath. Checking {@code
 * ConstraintViolationExceptionTranslator} alone would suffice — it implies the API transitively —
 * but listing both makes the intent readable and covers the case of someone depending on
 * jakarta.validation-api without pulling in ripcurl-jakarta-validation.
 */
@AutoConfiguration(before = MethodicalAutoConfiguration.class)
@ConditionalOnClass({
  ConstraintViolationException.class,
  ConstraintViolationExceptionTranslator.class
})
@ImportRuntimeHints(RipCurlJakartaValidationRuntimeHints.class)
public class RipCurlJakartaValidationAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ConstraintViolationExceptionTranslator constraintViolationExceptionTranslator() {
    return new ConstraintViolationExceptionTranslator();
  }
}
