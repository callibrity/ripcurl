package com.callibrity.ripcurl.core.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class LazyInitializer<T> {

// ------------------------------ FIELDS ------------------------------

    private final AtomicReference<T> ref = new AtomicReference<>();
    private final Supplier<T> supplier;

// -------------------------- STATIC METHODS --------------------------

    public static <T> LazyInitializer<T> of(Supplier<T> initializer) {
        return new LazyInitializer<>(initializer);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public LazyInitializer(Supplier<T> supplier) {
        this.supplier = supplier;
    }

// -------------------------- OTHER METHODS --------------------------

    public T get() {
        final var current = ref.get();
        if (current != null) {
            return current;
        }
        ref.compareAndSet(null, supplier.get());
        return ref.get();
    }

    public void reset() {
        ref.set(null);
    }

}
