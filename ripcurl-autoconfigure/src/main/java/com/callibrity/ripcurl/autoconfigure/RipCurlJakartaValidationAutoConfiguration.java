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
import jakarta.validation.Validator;
import org.jwcarman.methodical.jakarta.JakartaValidationInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Wires RipCurl's Jakarta validation integration end-to-end:
 *
 * <ul>
 *   <li>a {@link ConstraintViolationExceptionTranslator} that maps {@link
 *       ConstraintViolationException} to {@code -32602 Invalid params} with per-violation detail in
 *       the response's {@code data} field; and
 *   <li>a {@link JakartaValidationCustomizer} that wraps a {@link JakartaValidationInterceptor}
 *       (constructed here from the app's {@code Validator} bean) and attaches it to every
 *       {@code @JsonRpcMethod} handler so validation runs on each dispatch.
 * </ul>
 *
 * <p>Triggers only when {@link ConstraintViolationException}, {@link
 * ConstraintViolationExceptionTranslator}, and {@link JakartaValidationInterceptor} are all on the
 * classpath, and a {@link Validator} bean is in the context.
 */
@AutoConfiguration
@ConditionalOnClass({
  ConstraintViolationException.class,
  ConstraintViolationExceptionTranslator.class,
  JakartaValidationInterceptor.class
})
@ImportRuntimeHints(RipCurlJakartaValidationRuntimeHints.class)
public class RipCurlJakartaValidationAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ConstraintViolationExceptionTranslator constraintViolationExceptionTranslator() {
    return new ConstraintViolationExceptionTranslator();
  }

  @Bean
  @ConditionalOnBean(Validator.class)
  @ConditionalOnMissingBean
  public JakartaValidationCustomizer jakartaValidationCustomizer(Validator validator) {
    return new JakartaValidationCustomizer(new JakartaValidationInterceptor(validator));
  }
}
