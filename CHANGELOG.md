# Changelog

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
