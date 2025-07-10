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
package com.callibrity.ripcurl.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LazyInitializerTest {

    @Mock
    private Supplier<String> supplier;

    @Test
    void initializerShouldReturnValueFromSupplier() {
        var expected = UUID.randomUUID().toString();
        when(supplier.get()).thenReturn(expected);
        var initializer = LazyInitializer.of(supplier);
        var actual = initializer.get();
        assertThat(actual).isEqualTo(expected);
        verify(supplier, times(1)).get();
    }

    @Test
    void initializerShouldOnlyBeCalledOnce() {
        var expected = UUID.randomUUID().toString();
        when(supplier.get()).thenReturn(expected);
        var initializer = LazyInitializer.of(supplier);
        initializer.get();
        var actual = initializer.get();
        assertThat(actual).isEqualTo(expected);
        verify(supplier, times(1)).get();
    }

    @Test
    void resetShouldReinitialize() {
        var expected = UUID.randomUUID().toString();
        when(supplier.get()).thenReturn(expected);
        var initializer = LazyInitializer.of(supplier);
        initializer.get();
        initializer.reset();
        var actual = initializer.get();
        assertThat(actual).isEqualTo(expected);
        verify(supplier, times(2)).get();
    }
}