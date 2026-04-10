# RipCurl

![Maven Central Version](https://img.shields.io/maven-central/v/com.callibrity.ripcurl/ripcurl)
![GitHub License](https://img.shields.io/github/license/callibrity/ripcurl)

[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=callibrity_ripcurl&metric=coverage)](https://sonarcloud.io/summary/new_code?id=callibrity_ripcurl)

A [JSON-RPC 2.0](https://www.jsonrpc.org/specification) compliant dispatching framework for Spring Boot 4. RipCurl handles method routing, parameter resolution, and error handling — you provide the HTTP layer.

## Quick Start

Add the starter:

```xml
<dependency>
    <groupId>com.callibrity.ripcurl</groupId>
    <artifactId>ripcurl-spring-boot-starter</artifactId>
    <version>2.2.0</version>
</dependency>
```

And the Jackson 3 resolver (included with Spring Boot 4):

```xml
<dependency>
    <groupId>org.jwcarman.methodical</groupId>
    <artifactId>methodical-jackson3</artifactId>
    <version>${methodical.version}</version>
</dependency>
```

## Defining Methods

Annotate a bean with `@JsonRpcService` and its methods with `@JsonRpcMethod`:

```java
@JsonRpcService
public class MathService {

    @JsonRpcMethod("subtract")
    public int subtract(int minuend, int subtrahend) {
        return minuend - subtrahend;
    }

    @JsonRpcMethod("ping")
    public String ping() {
        return "pong";
    }
}
```

RipCurl discovers all `@JsonRpcService` beans and registers their `@JsonRpcMethod` methods with the dispatcher. Parameters are resolved by name from the JSON-RPC `params` object (or by position from a JSON array).

## Message Types

RipCurl models the JSON-RPC 2.0 message types as a sealed hierarchy:

```
JsonRpcMessage (sealed)
├── JsonRpcRequest (sealed)
│   ├── JsonRpcCall (method + params + id) — expects a response
│   └── JsonRpcNotification (method + params) — fire-and-forget
└── JsonRpcResponse (sealed)
    ├── JsonRpcResult (result + id) — success
    └── JsonRpcError (error + id) — failure
```

Deserialize incoming JSON directly into the appropriate type via Jackson —
every sealed interface has a `@JsonCreator` that structurally dispatches to
the right concrete subtype:

```java
JsonRpcMessage message = objectMapper.treeToValue(body, JsonRpcMessage.class);
return switch (message) {
    case JsonRpcCall call -> dispatcher.dispatch(call);
    case JsonRpcNotification notification -> handleNotification(notification);
    case JsonRpcResult result -> handleClientResult(result);
    case JsonRpcError error -> handleClientError(error);
};
```

Spring controllers can also take the sealed type directly as `@RequestBody`,
letting Spring's message converter do the deserialization:

```java
@PostMapping
public ResponseEntity<?> handle(@RequestBody JsonRpcMessage message) {
    return switch (message) {
        case JsonRpcCall call -> ResponseEntity.ok(dispatcher.dispatch(call));
        case JsonRpcNotification n -> { dispatcher.dispatch(n); yield ResponseEntity.accepted().build(); }
        case JsonRpcResult r -> handleClientResult(r);
        case JsonRpcError e -> handleClientError(e);
    };
}
```

## Writing a Controller

RipCurl doesn't include a controller — you write your own. This gives you full control over HTTP concerns (headers, auth, content types):

```java
@RestController
@RequestMapping("/rpc")
public class JsonRpcController {

    private final JsonRpcDispatcher dispatcher;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> handle(@RequestBody JsonRpcRequest request) {
        JsonRpcResponse response = dispatcher.dispatch(request);
        if (response == null) {
            return ResponseEntity.noContent().build(); // notification
        }
        return ResponseEntity.ok(response);
    }
}
```

The dispatcher accepts both `JsonRpcCall` and `JsonRpcNotification` (via `JsonRpcRequest`). It never throws — it returns either a `JsonRpcResult` (success), `JsonRpcError` (failure), or `null` (notification).

For notifications, the dispatcher invokes the method but always returns `null` — per the spec, the server must not reply. Pattern match on the request type if you need different HTTP handling:

```java
return switch (request) {
    case JsonRpcCall call -> ResponseEntity.ok(dispatcher.dispatch(call));
    case JsonRpcNotification notification -> {
        dispatcher.dispatch(notification);
        yield ResponseEntity.accepted().build();
    }
};
```

## Batch Requests

JSON-RPC 2.0 supports batch requests (an array of requests). Use `dispatchBatch()`:

```java
List<JsonRpcRequest> requests = /* parse array */;
List<JsonRpcResponse> responses = dispatcher.dispatchBatch(requests);
if (responses.isEmpty()) {
    return ResponseEntity.noContent().build(); // all notifications
}
return ResponseEntity.ok(responses);
```

`dispatchBatch()` dispatches calls concurrently on virtual threads via `invokeAll`. Notifications are fire-and-forget — they don't block the batch response.

## Response Types

`JsonRpcResponse` is a sealed interface:

- **`JsonRpcResult`** — success. Has `result` (JsonNode), `id`, and `@JsonIgnore` metadata for transport hints.
- **`JsonRpcError`** — failure. Has `error` (code + message + optional data) and `id`.

Result and error are mutually exclusive per the JSON-RPC 2.0 spec — enforced by the type system.

### Returning a Custom Response

Handlers can return `JsonRpcResult` directly to attach metadata (e.g., SSE emitters for streaming):

```java
@JsonRpcMethod("tools/call")
public JsonRpcResult streamingCall(JsonRpcCall call) {
    SseEmitter emitter = setupStreaming();
    return call.result(null).withMetadata("emitter", emitter);
}
```

### Creating Correlated Responses

`JsonRpcCall` has factory methods that echo the request `id`:

```java
call.result(resultNode);          // JsonRpcResult with matching id
call.error(-32601, "Not found");  // JsonRpcError with matching id
```

## Error Handling

The dispatcher catches all exceptions and returns appropriate `JsonRpcError` responses:

| Error Code | Constant | When |
|---|---|---|
| -32700 | `JsonRpcProtocol.PARSE_ERROR` | Malformed JSON (controller concern) |
| -32600 | `JsonRpcProtocol.INVALID_REQUEST` | Bad jsonrpc version, missing method, invalid id/params type |
| -32601 | `JsonRpcProtocol.METHOD_NOT_FOUND` | Unknown method, `rpc.*` prefix |
| -32602 | `JsonRpcProtocol.INVALID_PARAMS` | Parameter deserialization failure |
| -32603 | `JsonRpcProtocol.INTERNAL_ERROR` | Unhandled runtime exception |

Handlers can throw `JsonRpcException` with a specific code:

```java
throw new JsonRpcException(JsonRpcProtocol.INVALID_PARAMS, "Name is required");
```

## Method Invocation

RipCurl uses [Methodical](https://github.com/jwcarman/methodical) for pluggable reflection-based parameter resolution. Custom `ParameterResolver<A>` beans are automatically picked up — register them as Spring beans with `@Order` to control priority.

## Requirements

- Java 25+
- Spring Boot 4.x
- [Methodical](https://github.com/jwcarman/methodical) 0.2.0+ with a JSON resolver module
