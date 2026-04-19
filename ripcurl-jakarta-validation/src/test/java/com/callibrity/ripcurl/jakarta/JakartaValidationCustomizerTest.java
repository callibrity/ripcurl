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

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethodHandlerConfig;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.jakarta.JakartaValidationInterceptor;

class JakartaValidationCustomizerTest {

  @Test
  void customizeAttachesInterceptor() {
    var interceptor =
        new JakartaValidationInterceptor(Validation.buildDefaultValidatorFactory().getValidator());
    var config = mock(JsonRpcMethodHandlerConfig.class);
    var customizer = new JakartaValidationCustomizer(interceptor);

    customizer.customize(config);

    verify(config).interceptor(interceptor);
    verifyNoMoreInteractions(config);
  }

  @Test
  void constructorRejectsNullInterceptor() {
    assertThatNullPointerException()
        .isThrownBy(() -> new JakartaValidationCustomizer(null))
        .withMessage("interceptor");
  }
}
