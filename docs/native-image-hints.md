# GraalVM native-image hints for ripcurl

## Context

Exercised against `cowork-connector-example` (Spring Boot 4.0.5, Java 25) using the GraalVM tracing agent (`-agentlib:native-image-agent`). This document records what ripcurl ships to make consuming apps native-image-ready and why.

Reference companions in this family: `mocapi/docs/native-image-hints.md`, `substrate/docs/native-image-hints.md`, `odyssey/docs/native-image-hints.md`, `methodical/docs/native-image-hints.md`, `codec/docs/native-image-hints.md`.

## Agent-captured surface (16 entries)

Running the cowork-connector-example under the tracing agent surfaced these ripcurl types:

- **Core message types** (5) — `JsonRpcMessage`, `JsonRpcResponse`, `JsonRpcResult`, `JsonRpcError`, `JsonRpcErrorDetail`
- **Annotations** (2) — `@JsonRpcMethod`, `@JsonRpcService`
- **SPI ifaces + default impls** (7) — `JsonRpcDispatcher` + `DefaultJsonRpcDispatcher`, `JsonRpcMethodProvider`, `JsonRpcServiceMethodProvider`, `AnnotationJsonRpcMethodProviderFactory` + `DefaultAnnotationJsonRpcMethodProviderFactory`, `JsonRpcParamsResolver`
- **Auto-configs** (2) — `RipCurlAutoConfiguration`, `RipCurlResolversAutoConfiguration`

## How coverage works

Two contributions in `ripcurl-autoconfigure/src/main/resources/META-INF/spring/aot.factories`:

```
org.springframework.beans.factory.aot.BeanRegistrationAotProcessor=\
com.callibrity.ripcurl.autoconfigure.aot.JsonRpcServiceBeanAotProcessor

org.springframework.aot.hint.RuntimeHintsRegistrar=\
com.callibrity.ripcurl.autoconfigure.aot.RipCurlRuntimeHints
```

### `JsonRpcServiceBeanAotProcessor`

For every Spring bean annotated with `@JsonRpcService`, walks its declared methods. On each `@JsonRpcMethod`:

- `ExecutableMode.INVOKE` hint on the method itself.
- `BindingReflectionHints` on every parameter type annotated with `@JsonRpcParams`.
- `BindingReflectionHints` on the non-`void` return type.

Non-matching beans are skipped. No-op for JIT builds.

This handles **user code automatically** — downstream apps don't write hints for their JSON-RPC handler signatures.

### `RipCurlRuntimeHints`

Explicitly registers `BindingReflectionHints` on the eight public message types in the `JsonRpcMessage` hierarchy:

- `JsonRpcMessage`, `JsonRpcRequest`, `JsonRpcResponse`, `JsonRpcCall`, `JsonRpcNotification`, `JsonRpcResult`, `JsonRpcError`, `JsonRpcErrorDetail`

**Why explicit instead of a package scan:** these types cross a codec boundary without appearing in `@JsonRpcMethod` signatures — e.g., when a consumer journals raw `JsonRpcMessage` values through an external store, or dispatches envelopes manually rather than through a `@JsonRpcService` bean. The list is short and stable, so enumeration is clearer than a scan. Any new message types added to the hierarchy must be registered here.

### Exception translator SPI (2.6.0+)

`DefaultJsonRpcExceptionTranslatorRegistry` reads each registered translator's generic interface at bean construction — `TypeRef.of(translator.getClass()).typeArgument(JsonRpcExceptionTranslator.class, 0)` — to extract the handled exception type. Under native-image, generic-signature metadata is stripped unless the classes are explicitly registered, which would surface at startup as `"Unable to resolve the exception type parameter"` and fail registry creation.

`RipCurlRuntimeHints` covers this by registering:

- `JsonRpcExceptionTranslator` — the SPI interface; `TypeRef` reads its type parameters to drive resolution.
- `DefaultJsonRpcExceptionTranslator`, `IllegalArgumentExceptionTranslator`, `ParameterResolutionExceptionTranslator` — the three built-in translators shipped in `ripcurl-core`.

Class-level registration (no `MemberCategory`) is sufficient because `getGenericInterfaces()` is intrinsic metadata on the `Class` object and doesn't require field/method introspection.

User-written translators registered as Spring beans are covered automatically by Spring's bean-type AOT registrar — no additional hints needed.

### Jakarta Validation hints (2.6.0+)

`ConstraintViolationExceptionTranslator` lives in the optional `ripcurl-jakarta-validation` module. Its hints are registered by `RipCurlJakartaValidationRuntimeHints` via `@ImportRuntimeHints` on `RipCurlJakartaValidationAutoConfiguration` rather than `aot.factories` — so they only apply when the optional module is actually on the classpath. Otherwise Spring's AOT processor would `NoClassDefFoundError` on the translator class.

### What Spring AOT handles (no explicit hints needed)

- `RipCurlAutoConfiguration` + `RipCurlResolversAutoConfiguration` — Spring Boot AOT generates the binding code.
- `DefaultJsonRpcDispatcher`, `JsonRpcServiceMethodProvider`, `DefaultAnnotationJsonRpcMethodProviderFactory`, `JsonRpcParamsResolver` — Spring beans, replaced by generated factory code in native.
- `@JsonRpcMethod` / `@JsonRpcService` annotation discovery — Spring's merged-annotation machinery is pre-computed at AOT time.

## Tests

Any added test for this area should mirror mocapi's `MocapiRuntimeHintsTest` pattern: build a `RuntimeHints`, feed it through `RipCurlRuntimeHints`, and assert `TypeReference` coverage on each of the eight message types. For the processor, build a `RegisteredBean` around a `@JsonRpcService` fixture and assert `INVOKE` + binding hints land on the expected methods/params/returns.

## Verification

The cowork-connector-example at `~/IdeaProjects/cowork-connector-example` is the reference consumer (it uses ripcurl indirectly via mocapi's `mocapi-transport-streamable-http`). After publishing a ripcurl candidate:

1. Bump the ripcurl version in mocapi's `mocapi-transport-streamable-http` pom, or pin it via `<dependencyManagement>` in the cowork app.
2. `mvn -Pnative spring-boot:build-image -DBP_NATIVE_IMAGE=true`.
3. Exercise the MCP protocol end-to-end: `initialize`, `tools/list`, `tools/call`, `prompts/get`, and at least one error path (unknown tool name returns JSON-RPC `-32602`).

If any call errors with `MissingReflectionRegistrationError` on a `JsonRpc*` type, extend `RipCurlRuntimeHints` to cover it. If the error is on a consumer's handler signature, that points to a `JsonRpcServiceBeanAotProcessor` bug — likely a missing `@JsonRpcParams` annotation on the offending parameter.
