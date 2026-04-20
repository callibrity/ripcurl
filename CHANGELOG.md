# Changelog

## 2.10.0

### Added

- **`JsonRpcDispatcher.CURRENT_REQUEST`** — public `ScopedValue<JsonRpcRequest>` bound by `DefaultJsonRpcDispatcher` for the duration of each call/notification dispatch. Method interceptors and downstream code can read the full envelope — method name, params, `id` (via `JsonRpcCall` pattern match), `jsonrpc` version — even though the methodical `MethodInvocation` argument is only the params `JsonNode`. Unbound outside a dispatch scope; guard with `ScopedValue#isBound()` before calling `get()`. Enables downstream observability modules (e.g. mocapi-o11y) to emit JSON-RPC span attributes like `jsonrpc.request.id` without threading the envelope through every interceptor signature.

## 2.9.0

### Breaking changes

- **Bumped methodical `0.7.0` → `0.8.0`.** Methodical shifted to a fluent builder: `MethodInvoker.builder(method, target, type).resolver(...).interceptor(...).build()` replaces the old `MethodInvokerFactory.create(...)` + `Consumer<MethodInvokerConfig>` pattern. RipCurl's `JsonRpcMethodHandlers.build(...)` now takes four args instead of five (no more `MethodInvokerFactory` parameter) — callers wiring handlers manually outside of Spring auto-configuration must drop that argument.
- **Package flatten.** Methodical collapsed `org.jwcarman.methodical.param.*` and `org.jwcarman.methodical.intercept.*` into `org.jwcarman.methodical.*`. Imports of `ParameterResolver`, `ParameterInfo`, `ParameterResolver.Binding`, `MethodInterceptor`, and `MethodInvocation` all move to the root package. Pure source break; no behavior change.
- **`methodical-autoconfigure` dependency removed.** Methodical no longer ships an autoconfigure module or a Spring Boot starter — it's a plain library now. RipCurl's own autoconfigs own their Spring wiring end-to-end:
  - `RipCurlAutoConfiguration` drops `@AutoConfiguration(after = MethodicalAutoConfiguration.class)` (no such class exists anymore) and no longer autowires a `MethodInvokerFactory` bean.
  - `RipCurlJakartaValidationAutoConfiguration` now constructs its own `JakartaValidationInterceptor` from an autowired `Validator` bean, gated `@ConditionalOnBean(Validator.class)` + `@ConditionalOnClass(JakartaValidationInterceptor.class)`. Previously it picked up the interceptor bean registered by Methodical's removed autoconfigure.

### Changed

- **`JsonRpcMethodHandlers.build(...)` uses the fluent builder internally.** Construction reads top-to-bottom: `JsonRpcParamsResolver` → customizer resolvers → `Jackson3ParameterResolver` → customizer interceptors, then `build()`. Same chain semantics as 2.8.0; cleaner source.

### Migration

**Custom resolver imports:**
```diff
-import org.jwcarman.methodical.param.ParameterResolver;
-import org.jwcarman.methodical.param.ParameterInfo;
-import org.jwcarman.methodical.intercept.MethodInterceptor;
+import org.jwcarman.methodical.ParameterResolver;
+import org.jwcarman.methodical.ParameterInfo;
+import org.jwcarman.methodical.MethodInterceptor;
```

**Manual handler wiring (outside Spring auto-config):**
```diff
-JsonRpcMethodHandlers.build(bean, method, mapper, invokerFactory, customizers);
+JsonRpcMethodHandlers.build(bean, method, mapper, customizers);
```

Apps that only wire RipCurl via `@JsonRpcMethod` on beans need no code changes; the customizer SPI and `@JsonRpcParams` behavior are unchanged.

## 2.8.0

### Breaking changes

- **`@JsonRpcService` class-level annotation removed.** Register handler beans however your app normally does (`@Component`, `@Bean`, or explicit registration); the method-level `@JsonRpcMethod` annotation is the opt-in. Migration: replace `@JsonRpcService` with `@Component` (or leave the bean alone if it's already registered some other way).
- **`JsonRpcMethod` and `JsonRpcMethodProvider` SPI interfaces removed** along with `AnnotationJsonRpcMethod`, `AnnotationJsonRpcMethodProviderFactory`, `DefaultAnnotationJsonRpcMethodProviderFactory`, and `JsonRpcServiceMethodProvider`. One concrete `JsonRpcMethodHandler` (in `core.annotation`) replaces the entire stack, built pure-Java from a `(bean, @JsonRpcMethod method)` pair via `JsonRpcMethodHandlers.build(...)`.
- **`DefaultJsonRpcDispatcher` constructor takes `List<JsonRpcMethodHandler>`** instead of `List<JsonRpcMethodProvider>`. The dispatcher builds its name-indexed map eagerly in the constructor (the old `LazyInitializer` indirection and the util class itself are gone).
- **All per-handler extension moves to a single customizer SPI.** Register a `JsonRpcMethodHandlerCustomizer` bean; its `customize(JsonRpcMethodHandlerConfig)` hook receives the handler's name, method, and bean, and appends either resolvers or interceptors scoped to that handler. The 2.7.0 bean-level autowiring paths — both `List<ParameterResolver<? super JsonNode>>` and `List<MethodInterceptor<? super JsonNode>>` — are gone. This replaces a footgun (any resolver or interceptor bean on the classpath joining every RipCurl pipeline by structural coincidence) with an intentional, typed SPI.
- **`RipCurlResolversAutoConfiguration` deleted.** `JsonRpcParamsResolver` and `Jackson3ParameterResolver` are no longer Spring beans. Both are constructed inline by `JsonRpcMethodHandlers.build(...)` — `JsonRpcParamsResolver` at the head of every handler's resolver chain, `Jackson3ParameterResolver` at the tail. Customizer-added resolvers slot between them. Methodical's `@Argument` tail resolver still runs after the Jackson catch-all.
- **AOT processor renamed** `JsonRpcServiceBeanAotProcessor` → `JsonRpcMethodAotProcessor`. It now keys on "any bean class with at least one `@JsonRpcMethod` method" instead of the `@JsonRpcService` marker. Registered under the same `aot.factories` key; no user action needed for apps that don't reference the processor class directly.
- **Bumped methodical `0.6.0` → `0.7.0`.** Methodical's `ParameterResolver` contract changed from `supports(ParameterInfo)` + `resolve(ParameterInfo, A)` to a single `bind(ParameterInfo) → Optional<Binding<A>>`. The returned `Binding` is per-parameter and can cache derived artifacts (e.g., `ObjectReader` for a known target type) for reuse across every dispatch. RipCurl's `JsonRpcParamsResolver` implements the new shape and caches the `ObjectReader` in the `Binding`, eliminating the per-call `mapper.readerFor(type)` allocation. Users who write custom resolvers against the customizer SPI must migrate to the new `bind`-based shape.

### Changed

- **Jakarta validation integration re-wired as a customizer.** `ripcurl-jakarta-validation` now ships a `JakartaValidationCustomizer` that attaches Methodical's `JakartaValidationInterceptor` to every `@JsonRpcMethod` handler. `RipCurlJakartaValidationAutoConfiguration` registers the customizer bean when the jakarta module and the interceptor bean are both present. The `ConstraintViolationException` → `-32602 Invalid params` translator is unchanged.
- **Handler discovery no longer instantiates beans without `@JsonRpcMethod` methods.** The scan in `RipCurlAutoConfiguration.jsonRpcMethodHandlers(...)` uses `ConfigurableListableBeanFactory.getBeanNamesForType(Object.class, false, false)` + `getType(name, false)` so lazy / prototype / `FactoryBean` beans that don't host handler methods are never instantiated. Proxies are unwrapped via `AopUtils.getTargetClass(...)` for annotation discovery; invocation still targets the proxy so Spring AOP advice (e.g. `@Transactional`) runs around handler calls.

### Migration

**Handler classes:**
```diff
-@JsonRpcService
+@Component
 public class Calculator {
     @JsonRpcMethod("subtract")
     public int subtract(int a, int b) { return a - b; }
 }
```

**Custom interceptors:**
```diff
-@Bean
-MethodInterceptor<JsonNode> timingInterceptor() {
-  return invocation -> { /* ... */ };
-}
+@Bean
+JsonRpcMethodHandlerCustomizer timingCustomizer() {
+  return config -> config.interceptor(invocation -> { /* ... */ });
+}
```

**Custom resolvers:**
```diff
-@Bean
-ParameterResolver<JsonNode> myResolver() {
-  return new MyResolver();
-}
+@Bean
+JsonRpcMethodHandlerCustomizer myResolverCustomizer() {
+  return config -> config.resolver(new MyResolver());
+}
```

The customizer can read `config.name()` / `config.method()` / `config.bean()` to decide whether to attach the resolver/interceptor at all — behavior that was impossible via the old bean-list paths.

## 2.7.0

### Changed
- **Bumped methodical `0.5.0` → `0.6.0`.** Methodical's invocation model changed: resolvers and interceptors are now configured per-invoker via a `Consumer<MethodInvokerConfig>` lambda on `MethodInvokerFactory.create(...)` rather than held as a frozen list on the factory. `@Argument` is now a built-in tail resolver with a type-compatibility guard, so unbindable parameters fail loudly instead of silently deserializing into nonsense. Jakarta validation moves from a separate `MethodValidatorFactory` track to a regular `MethodInterceptor<Object>` (`JakartaValidationInterceptor`), unifying the extension model.
- **`DefaultAnnotationJsonRpcMethodProviderFactory` constructor** now takes a `List<ParameterResolver<? super JsonNode>>` (and optionally a `List<MethodInterceptor<? super JsonNode>>`) alongside the `MethodInvokerFactory`. `JsonRpcServiceMethodProvider` and `RipCurlAutoConfiguration` thread the same lists through to invoker creation.
- **Extension point**: downstream apps or starters contribute custom behavior by registering beans of type `ParameterResolver<? super JsonNode>` or `MethodInterceptor<? super JsonNode>`. Spring's generic-aware list injection filters by the wildcard, so interceptors parameterized for unrelated root types (e.g., a Mocapi `MethodInterceptor<McpRequest>` on the same classpath) are correctly excluded from the RipCurl pipeline. Bean order (`@Order`) determines resolver/interceptor traversal order — first-added is first-applied.

### Notes
- No user-facing API break for handler code: `@JsonRpcMethod`-annotated methods with plain parameters still resolve by name/index through the Jackson resolver (still auto-registered by Methodical's `Jackson3AutoConfiguration`) and the `@Argument` tail. `@JsonRpcParams` behavior is unchanged.
- The construction-site signatures of `DefaultAnnotationJsonRpcMethodProviderFactory` and `JsonRpcServiceMethodProvider` changed. Callers wiring these outside of Spring auto-configuration (e.g., test fixtures) must pass the resolver list explicitly.

## 2.6.0

### New Features
- **Exception-to-JSON-RPC-error translator SPI** — new `JsonRpcExceptionTranslator<E extends Exception>` and `JsonRpcExceptionTranslatorRegistry` in `ripcurl-core`. Modeled after JAX-RS `ExceptionMapper<E>`: translators are named classes implementing the interface with a concrete exception type; the handled type is resolved reflectively via Specular's `TypeRef` at bean construction, so implementers write one method and never restate the type. The default registry keys translators by exact exception class and resolves at dispatch by walking the thrown exception's superclass chain (`<E extends Exception>` means E is a class, not an interface, so a plain `getSuperclass()` walk suffices). When no match is found, a built-in fallback returns `-32603 Internal error` with a deliberately-generic message to avoid leaking implementation details, and logs the full exception at ERROR for operators. Applications can replace the fallback by registering their own `JsonRpcExceptionTranslator<Exception>`. Duplicate registrations for the same type are rejected at construction.
- **Built-in translators registered as beans via `ripcurl-autoconfigure`**, each `@ConditionalOnMissingBean` so apps override individually without replacing the whole registry:
  - `DefaultJsonRpcExceptionTranslator` preserves a `JsonRpcException`'s self-declared code and message.
  - `IllegalArgumentExceptionTranslator` maps Java's built-in "bad argument" exception to `-32602 Invalid params`, so handlers can `throw new IllegalArgumentException(...)` without needing to know about `JsonRpcException`.
  - `ParameterResolutionExceptionTranslator` maps methodical's `ParameterResolutionException` to `-32602`.
- **New `ripcurl-jakarta-validation` module** — ships `ConstraintViolationExceptionTranslator` which maps Jakarta Bean Validation's `ConstraintViolationException` to `-32602 Invalid params` with per-violation detail (`field`, `message`) emitted as a JSON array in the error response's `data` field. `invalidValue` is deliberately omitted to avoid leaking sensitive inputs through error responses. The module is Spring-free; Spring wiring lives in `ripcurl-autoconfigure` as `RipCurlJakartaValidationAutoConfiguration`, gated by `@ConditionalOnClass` on both `ConstraintViolationException` (the API) and `ConstraintViolationExceptionTranslator` (our implementation) so apps depending on `jakarta.validation-api` from an unrelated path don't trigger a `NoClassDefFoundError`.
- **Native-image hints** for the exception translator SPI and all built-in translators, so `TypeRef` can resolve each translator's `<E>` under GraalVM `native-image` without losing the generic-signature metadata. Jakarta validation hints are registered via `@ImportRuntimeHints` on the jakarta autoconfig (not `aot.factories`) so they only apply when the optional `ripcurl-jakarta-validation` module is on the classpath.

### Changed
- **Bumped methodical `0.3.0` → `0.5.0`.** Transitively pulls Specular (`org.jwcarman.specular`) into the classpath. `ParameterInfo.of` signature changed to take a single `TypeRef<?>` instead of `(Class, Type)`; `org.jwcarman.methodical.reflect.Types` was removed. Only one test fixture in `ripcurl-core` needed updating — main code reads `info.resolvedType()` which is unchanged.
- **Dispatcher's outer catch narrowed from `Throwable` to `Exception`.** JVM-fatal `Error`s (OOM, StackOverflow, LinkageError) now propagate unchanged — matching Spring MVC, JAX-RS, and Spring Security practice. Attempting to serialize a JSON-RPC response during a fatal JVM condition is unsafe; operators get a clean stack trace instead of a swallowed failure. The `JsonRpcExceptionTranslator` SPI bound matches (`<E extends Exception>`).

### Notes
- The new SPI is additive — no breaking changes. `DefaultJsonRpcDispatcher` gains a two-arg constructor accepting a `JsonRpcExceptionTranslatorRegistry`; the single-arg form still works and wires a registry populated with the built-in translators by default.
- Applications that relied on the old `DefaultJsonRpcDispatcher` behavior of returning the raw exception message on the catch-all path will now see `"Internal error"` instead. The thrown message is still available in server logs; the change is specifically to stop leaking implementation detail to clients. Apps that want the old behavior can register a `JsonRpcExceptionTranslator<Exception>` that passes `exception.getMessage()` through.

## 2.5.0

### New Features
- **Built-in runtime hints for the JSON-RPC message types** — `RipCurlRuntimeHints` (registered via `META-INF/spring/aot.factories` as a `RuntimeHintsRegistrar`) registers Jackson binding hints for the eight wire-level types: `JsonRpcMessage`, `JsonRpcRequest`, `JsonRpcResponse`, `JsonRpcCall`, `JsonRpcNotification`, `JsonRpcResult`, `JsonRpcError`, `JsonRpcErrorDetail`. These types use `@JsonCreator(DELEGATING)` factories that select a concrete subtype by structural sniffing, which Spring's binding-hints walker cannot discover transitively from the sealed interface alone. With this registrar in place, any RipCurl-based application can deserialize JSON-RPC messages in a GraalVM `native-image` build without hand-rolling these hints.

### Notes
- Native-image support is best-effort. The hint registration logic is unit-tested, but end-to-end validation requires building a native image of a real RipCurl application — currently exercised in downstream applications, not in this repository's CI. Bug reports welcome if you hit a missing hint in your own native build.

## 2.4.0

### New Features
- **AOT runtime hints for native image builds** — `JsonRpcServiceBeanAotProcessor` (registered as a `BeanRegistrationAotProcessor` via `META-INF/spring/aot.factories`) walks every `@JsonRpcService` bean at build time and registers the reflection metadata needed to dispatch JSON-RPC calls in a GraalVM native image without hand-written hints:
  - An `ExecutableMode.INVOKE` reflection hint on each `@JsonRpcMethod` so the dispatcher can invoke it reflectively.
  - Jackson binding hints on every `@JsonRpcParams` parameter type so typed params records can be deserialized.
  - Jackson binding hints on the method return type so results can be serialized (skipped for `void` and `Void`).

  Non-`@JsonRpcService` beans are ignored, and the processor is a no-op for plain JVM builds.

## 2.3.0

### Breaking Changes
- **Removed `JsonRpcResult.metadata` field**, along with `withMetadata(name, value)` and `getMetadata(name, type)`. This was a non-spec transport-hint bag that is no longer needed. Handlers that previously attached transport state to a result should use a sidechannel (method argument, `ThreadLocal`, or a wrapper type owned by the transport layer).

## 2.2.0

### Breaking Changes
- **`JsonRpcMessage.parse(JsonNode)` is deleted.** Use Jackson directly:
  `objectMapper.treeToValue(node, JsonRpcMessage.class)` or
  `objectMapper.readerFor(JsonRpcMessage.class).readValue(json)`. The
  same call style works for `JsonRpcRequest` and `JsonRpcResponse` as
  polymorphic entry points.

### New Features
- **Jackson-polymorphic sealed hierarchies** — `JsonRpcMessage`,
  `JsonRpcRequest`, and `JsonRpcResponse` are now directly
  deserializable via Jackson without a hand-rolled `parse()` call. Each
  sealed interface exposes a `@JsonCreator(mode = DELEGATING)` static
  factory method that structurally sniffs the incoming tree and
  dispatches to the correct concrete subtype (`JsonRpcCall`,
  `JsonRpcNotification`, `JsonRpcResult`, `JsonRpcError`):
  ```java
  JsonRpcMessage msg = mapper.treeToValue(node, JsonRpcMessage.class);
  JsonRpcResponse resp = mapper.readerFor(JsonRpcResponse.class).readValue(json);
  ```
- This is the key enabler for downstream libraries (e.g., mocapi) that
  want to use typed mailboxes such as `Mailbox<JsonRpcResponse>` with
  serializing backends like Redis — Jackson can now reconstruct the
  correct sealed subtype on the receiver side.

### Why `@JsonCreator` instead of `@JsonTypeInfo(Id.DEDUCTION)`
`Id.DEDUCTION` was evaluated and found unsuitable. Jackson's deduction
algorithm requires each subtype to have at least one property that no
sibling has, and `JsonRpcNotification`'s property set
(`{jsonrpc, method, params}`) is a strict subset of `JsonRpcCall`'s
(`{jsonrpc, method, params, id}`). For notification-shaped JSON
without an `id` field, deduction reports "2 candidates match" and
fails. The `@JsonCreator` approach does explicit structural
discrimination and handles every JSON-RPC 2.0 message shape correctly.

### Migration
| 2.1.x | 2.2.0 |
|---|---|
| `JsonRpcMessage.parse(body)` | `mapper.treeToValue(body, JsonRpcMessage.class)` |
| `try { ... } catch (IllegalArgumentException e) { ... }` | Unchanged — creator throws `IllegalArgumentException`, Jackson wraps in `DatabindException` whose root cause is the original `IllegalArgumentException`. Use `assertThatThrownBy(...).rootCause().isInstanceOf(IllegalArgumentException.class)` in tests. |

## 2.1.0

### New Features
- **`@JsonRpcParams` annotation** — Mark a parameter to receive the entire JSON-RPC `params` object deserialized into a typed record. Useful for strongly-typed request params:
  ```java
  @JsonRpcMethod("tools/call")
  public CallToolResult callTool(@JsonRpcParams CallToolRequestParams params) {
      // params is the deserialized record
  }
  ```
- `JsonRpcParamsResolver` registered with `HIGHEST_PRECEDENCE` so `@JsonRpcParams` is checked before any other resolver.
- New `RipCurlResolversAutoConfiguration` that runs before `MethodicalAutoConfiguration` to register RipCurl-provided parameter resolvers.

### Changes
- Upgraded Methodical to 0.3.0 (for `@Argument` annotation support)

## 2.0.0

### Breaking Changes
- `JsonRpcRequest` is now a sealed interface (was a record) — the base type for all incoming JSON-RPC messages
- Requests with an `id` are now `JsonRpcCall` (was `JsonRpcRequest`)
- `JsonRpcNotification` is a proper type under `JsonRpcRequest` (was under `JsonRpcMessage`)
- `JsonRpcRequest.response()` renamed to `JsonRpcCall.result()`
- `JsonRpcRequest.error()` moved to `JsonRpcCall.error()`
- `JsonRpcRequest.request()` factory removed — use `new JsonRpcCall(...)` or `JsonRpcCall.of()`
- `JsonRpcRequest.notification()` factory removed — use `new JsonRpcNotification(...)` or `JsonRpcNotification.of()`
- `JsonRpcCall` requires non-null `method` and `id` (enforced via compact constructor)
- `JsonRpcNotification` requires non-null `method` (enforced via compact constructor)

### New Features
- **Sealed request hierarchy** — dispatcher accepts `JsonRpcRequest` and pattern-matches on `JsonRpcCall` vs `JsonRpcNotification`
- **`params` type validation** — rejects non-Object/Array params per JSON-RPC 2.0 spec
- **SLF4J logging** — method registration at INFO, annotation discovery at DEBUG, notification failures at WARN

### Type Hierarchy
```
JsonRpcMessage (sealed)
├── JsonRpcRequest (sealed)
│   ├── JsonRpcCall (method + params + id)
│   └── JsonRpcNotification (method + params, no id)
└── JsonRpcResponse (sealed)
    ├── JsonRpcResult
    └── JsonRpcError
```

### Migration Guide
| 1.x | 2.0 |
|---|---|
| `new JsonRpcRequest("2.0", "method", params, id)` | `new JsonRpcCall("2.0", "method", params, id)` |
| `new JsonRpcRequest("2.0", "method", params, null)` | `new JsonRpcNotification("2.0", "method", params)` |
| `JsonRpcRequest.request("method", params, id)` | `JsonRpcCall.of("method", params, id)` |
| `JsonRpcRequest.notification("method", params)` | `JsonRpcNotification.of("method", params)` |
| `request.response(result)` | `call.result(result)` |
| `dispatch(JsonRpcRequest)` | `dispatch(JsonRpcRequest)` (unchanged — accepts both types) |

## 1.1.0

### New Features
- Batch request support via `dispatchBatch(List<JsonRpcRequest>)` — dispatches requests concurrently on virtual threads, fire-and-forgets notifications
- Error code constants moved to `JsonRpcProtocol` (PARSE_ERROR, INVALID_REQUEST, METHOD_NOT_FOUND, INVALID_PARAMS, INTERNAL_ERROR)
- `ParameterResolutionException` handling — deserialization failures return INVALID_PARAMS
- Comprehensive JSON-RPC 2.0 compliance test suite

### Changes
- Depends on Methodical 0.2.0 (ParameterResolutionException hierarchy)
- Error code constants removed from `JsonRpcException` (use `JsonRpcProtocol.*` instead)

## 1.0.0

### Breaking Changes
- `@JsonRpc` annotation renamed to `@JsonRpcMethod`
- `JsonRpcResponse` is now a sealed interface with `JsonRpcResult` and `JsonRpcError` subtypes
- `JsonRpcMethod.call()` returns `JsonRpcResult` (not `JsonRpcResponse`)
- Dispatcher catches `JsonRpcException` and returns `JsonRpcError` — never throws
- Method invocation delegated to Methodical library — `JsonMethodInvoker`, `JsonParamResolver`, `JsonRpcParamResolver` deleted
- Custom parameter resolvers are now Methodical `ParameterResolver<A>` beans

### New Features
- `JsonRpcResult` — success response with `@JsonIgnore`d metadata for transport hints
- `JsonRpcError` — error response with `JsonRpcErrorDetail` (code, message, optional data)
- `JsonRpcRequest.response(result)` — creates correlated `JsonRpcResult`
- `JsonRpcRequest.error(code, message)` — creates correlated `JsonRpcError`
- `"id": null` (NullNode) treated as request with null id, not notification
- Methods starting with `rpc.` rejected per JSON-RPC 2.0 spec
- Depends on Methodical 0.1.0 for pluggable reflection-based method invocation

## 0.7.0

### Breaking Changes
- `JsonRpcMethod.call()` now takes `JsonRpcRequest` and returns `JsonRpcResponse` (was `JsonNode call(JsonNode params)`)
- `JsonMethodInvoker.invoke()` now takes `JsonRpcRequest` and returns `JsonRpcResponse`
- Handlers returning `JsonRpcResponse` directly are passed through without re-wrapping

### New Features
- Handlers can return `JsonRpcResponse` directly to control the full response including metadata
- `JsonRpcResponse` metadata — `@JsonIgnore`d metadata map for transport-level hints. Access via `getMetadata(name, type)` returning `Optional<T>`. Build immutably with `withMetadata(name, value)`.
- `JsonRpcRequest.request(method, params, id)` — static factory for requests
- `JsonRpcRequest.notification(method, params)` — static factory for notifications
- `JsonRpcResponse(result, id)` — convenience constructor with version set automatically
- `JsonRpcProtocol.VERSION` — public constant for the JSON-RPC 2.0 version string

## 0.3.0

### Breaking Changes
- `JsonRpcParamResolver.resolve()` signature changed to `resolve(Parameter, int index, JsonNode params)`
- `JsonParameterMapper` removed, replaced by `JsonParamResolver`

### Improvements
- JSON parameter resolution extracted into `JsonParamResolver`
- `JsonMethodInvoker` treats all resolvers uniformly
- Removed dead `@Autowired(required=false)` null checks in auto-configuration

## 0.2.0

### Breaking Changes
- Exception hierarchy collapsed into single `JsonRpcException` with error code constants
- `RipCurlController` removed — consumers provide their own HTTP layer

### New Features
- `JsonRpcException` error code constants: `PARSE_ERROR`, `INVALID_REQUEST`, `METHOD_NOT_FOUND`, `INVALID_PARAMS`, `INTERNAL_ERROR`
- `JsonRpcParamResolver` SPI for pluggable non-JSON parameter injection

### Improvements
- Upgraded to Java 25, Spring Boot 4.0.5, Jackson 3
- Added Spotless (Google Java Format)
- Moved JaCoCo to `ci` profile
- Added SonarCloud plugin, Spring Boot annotation processors
- Updated CI workflows and plugin versions

## 0.1.0

Initial release — JSON-RPC 2.0 dispatching framework for Spring Boot 4.
