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
