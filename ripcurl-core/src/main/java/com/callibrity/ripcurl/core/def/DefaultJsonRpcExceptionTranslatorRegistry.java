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
package com.callibrity.ripcurl.core.def;

import com.callibrity.ripcurl.core.JsonRpcErrorDetail;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jwcarman.specular.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link JsonRpcExceptionTranslatorRegistry} that keys translators by their exact exception
 * class and resolves at dispatch time by walking the thrown exception's superclass chain. Because
 * {@link JsonRpcExceptionTranslator}'s type parameter is bounded by {@link Exception} (a class, not
 * an interface), a plain {@code getSuperclass()} walk is sufficient — no need to fan out across
 * interfaces.
 *
 * <p>Each translator's handled exception type is resolved once at construction time via Specular's
 * {@link TypeRef}; per-invocation cost is at most {@code O(hierarchy depth)} hash lookups.
 *
 * <p>When the walk finds no match, the registry returns a built-in {@code -32603 Internal error}
 * payload carrying the exception's message. Applications can override this fallback by registering
 * their own {@link JsonRpcExceptionTranslator JsonRpcExceptionTranslator&lt;Exception&gt;} — the
 * walk lands on {@link Exception} before giving up, so a user-registered translator for that type
 * supersedes the built-in fallback without any plumbing work.
 *
 * <p>Duplicate registrations for the same exact exception type are rejected at construction time.
 * Applications override a built-in translator by excluding the default bean (e.g.
 * {@code @ConditionalOnMissingBean}) rather than registering a second one for the same type — this
 * avoids silent "last wins" surprises when bean-discovery order changes.
 */
public class DefaultJsonRpcExceptionTranslatorRegistry
    implements JsonRpcExceptionTranslatorRegistry {

  private static final Logger log =
      LoggerFactory.getLogger(DefaultJsonRpcExceptionTranslatorRegistry.class);

  private final Map<Class<? extends Exception>, Function<Exception, JsonRpcErrorDetail>>
      byExactType;

  public DefaultJsonRpcExceptionTranslatorRegistry(
      List<? extends JsonRpcExceptionTranslator<?>> translators) {
    Objects.requireNonNull(translators, "translators");
    Map<Class<? extends Exception>, Function<Exception, JsonRpcErrorDetail>> map =
        new LinkedHashMap<>();
    for (JsonRpcExceptionTranslator<?> translator : translators) {
      Entry entry = bind(translator);
      Function<Exception, JsonRpcErrorDetail> existing =
          map.put(entry.exceptionType, entry.invoker);
      if (existing != null) {
        throw new IllegalStateException(
            "Multiple JsonRpcExceptionTranslator instances registered for "
                + entry.exceptionType.getName()
                + "; exclude the built-in (e.g. @ConditionalOnMissingBean) before registering a"
                + " replacement.");
      }
    }
    this.byExactType = Map.copyOf(map);
  }

  @Override
  public JsonRpcErrorDetail translate(Exception exception) {
    Objects.requireNonNull(exception, "exception");
    for (Class<?> c = exception.getClass(); c != null; c = c.getSuperclass()) {
      Function<Exception, JsonRpcErrorDetail> invoker = byExactType.get(c);
      if (invoker != null) {
        return invoker.apply(exception);
      }
    }
    return fallback(exception);
  }

  /**
   * Default {@code -32603 Internal error} payload used when no registered translator matches the
   * thrown exception's type hierarchy. Applications that want different catch-all behavior register
   * their own {@code JsonRpcExceptionTranslator<Exception>} — the superclass walk reaches {@link
   * Exception} before falling through here, so a user-registered translator supersedes this.
   *
   * <p>The message is intentionally generic: by the time an exception reaches this fallback, the
   * server has no curated response for it, and {@code exception.getMessage()} could leak
   * implementation details (SQL fragments, file paths, stack-identifying strings) to the client.
   * The dispatcher logs the full exception at {@code ERROR} level before calling the registry, so
   * the detail remains in server-side logs where operators can find it.
   */
  private static JsonRpcErrorDetail fallback(Exception exception) {
    log.error(
        "No JsonRpcExceptionTranslator matched {}; returning generic internal error.",
        exception.getClass().getName(),
        exception);
    return new JsonRpcErrorDetail(JsonRpcProtocol.INTERNAL_ERROR, "Internal error");
  }

  /**
   * Captures the translator's wildcard type into method parameter {@code E} so the subsequent
   * {@code exceptionType.cast(exception)} call restores what erasure stripped — no unchecked cast
   * required at this level. The single unavoidable cast (reflective {@link Class} to {@code
   * Class<E>}) lives in {@link #eraseTo}.
   */
  private static <E extends Exception> Entry bind(JsonRpcExceptionTranslator<E> translator) {
    Class<E> exceptionType = resolveExceptionType(translator);
    Function<Exception, JsonRpcErrorDetail> invoker =
        exception -> translator.translate(exceptionType.cast(exception));
    return new Entry(exceptionType, invoker);
  }

  private static <E extends Exception> Class<E> resolveExceptionType(
      JsonRpcExceptionTranslator<E> translator) {
    // The generic bound <E extends Exception> enforces the Exception subtype at the declaration
    // site, so any value TypeRef returns is already guaranteed assignable to Exception — no
    // defensive re-check needed. If resolution fails entirely (e.g. lambda erases the type),
    // orElseThrow fires below.
    Class<?> raw =
        TypeRef.of(translator.getClass())
            .typeArgument(JsonRpcExceptionTranslator.class, 0)
            .map(TypeRef::getRawType)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unable to resolve the exception type parameter for translator "
                            + translator.getClass().getName()
                            + ". Declare translators as named classes implementing "
                            + "JsonRpcExceptionTranslator<SomeException> — lambdas and raw "
                            + "anonymous classes erase the type parameter."));
    return eraseTo(raw.asSubclass(Exception.class));
  }

  /**
   * The single unchecked cast needed to bridge reflectively-resolved type information to Java's
   * compile-time generics. Safe because {@code raw} was just extracted from the translator's own
   * {@code JsonRpcExceptionTranslator<E>} declaration — see {@link #resolveExceptionType}.
   */
  private static <E extends Exception> Class<E> eraseTo(Class<? extends Exception> raw) {
    @SuppressWarnings("unchecked")
    Class<E> cast = (Class<E>) raw;
    return cast;
  }

  private record Entry(
      Class<? extends Exception> exceptionType, Function<Exception, JsonRpcErrorDetail> invoker) {}
}
