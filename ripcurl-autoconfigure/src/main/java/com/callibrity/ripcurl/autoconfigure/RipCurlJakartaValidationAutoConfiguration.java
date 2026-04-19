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
import com.callibrity.ripcurl.jakarta.JakartaValidationCustomizer;
import jakarta.validation.ConstraintViolationException;
import org.jwcarman.methodical.autoconfigure.MethodicalAutoConfiguration;
import org.jwcarman.methodical.jakarta.JakartaValidationInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Wires two pieces of the Jakarta validation story:
 *
 * <ul>
 *   <li>a {@link ConstraintViolationExceptionTranslator} that maps {@link
 *       ConstraintViolationException} to {@code -32602 Invalid params} with per-violation detail in
 *       the response's {@code data} field; and
 *   <li>a {@link JakartaValidationCustomizer} that attaches Methodical's {@link
 *       JakartaValidationInterceptor} to every {@code @JsonRpcMethod} handler so the validation
 *       actually runs on each dispatch.
 * </ul>
 *
 * <p>Triggers only when {@link ConstraintViolationException} (jakarta.validation-api) and {@link
 * ConstraintViolationExceptionTranslator} (ripcurl-jakarta-validation, an optional dependency of
 * this module) are both on the classpath, and a {@link JakartaValidationInterceptor} bean is in the
 * context (contributed by Methodical's {@code JakartaValidationAutoConfiguration} when a {@code
 * Validator} bean is available).
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

  @Bean
  @ConditionalOnBean(JakartaValidationInterceptor.class)
  @ConditionalOnMissingBean
  public JakartaValidationCustomizer jakartaValidationCustomizer(
      JakartaValidationInterceptor interceptor) {
    return new JakartaValidationCustomizer(interceptor);
  }
}
