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