# Changelog

## 0.4.0

### New Features
- `JsonRpcProtocol.VERSION` — public constant for the JSON-RPC 2.0 version string
- `JsonRpcResponse(JsonNode result, JsonNode id)` — convenience constructor that sets `jsonrpc` to `"2.0"` automatically

### Cleanup
- Removed redundant `VALID_JSONRPC_VERSION` constant from `DefaultJsonRpcDispatcher`

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
