# Changelog

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
