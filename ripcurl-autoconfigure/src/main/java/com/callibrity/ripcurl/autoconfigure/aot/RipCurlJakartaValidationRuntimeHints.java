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
package com.callibrity.ripcurl.autoconfigure.aot;

import com.callibrity.ripcurl.jakarta.ConstraintViolationExceptionTranslator;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Native-image hints for the Jakarta Validation translator. Registered via
 * {@code @ImportRuntimeHints} on {@link
 * com.callibrity.ripcurl.autoconfigure.RipCurlJakartaValidationAutoConfiguration} rather than
 * {@code aot.factories} so the hint registration only fires when the optional {@code
 * ripcurl-jakarta-validation} module is actually on the classpath — otherwise Spring's AOT
 * processor would NoClassDefFoundError on {@link ConstraintViolationExceptionTranslator}.
 *
 * <p>Like the built-in translators in {@link RipCurlRuntimeHints}, the class's generic interface
 * signature must be preserved so Specular's {@code TypeRef} can extract the bound exception type at
 * registry construction.
 */
public class RipCurlJakartaValidationRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    hints
        .reflection()
        .registerType(ConstraintViolationExceptionTranslator.class, MemberCategory.PUBLIC_CLASSES);
  }
}
