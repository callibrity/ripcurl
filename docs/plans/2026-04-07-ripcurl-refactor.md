# RipCurl Refactor: Collapse Exceptions, Add Param Resolvers, Remove Controller

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Simplify RipCurl's exception hierarchy to a single `JsonRpcException` with error code constants, add pluggable `JsonRpcParamResolver` support to `JsonMethodInvoker`, and remove the controller so consumers (like Mocapi) provide their own HTTP layer.

**Architecture:** Collapse four exception subclasses into one `JsonRpcException` with a `code` field and static constants. Add a `JsonRpcParamResolver` interface that the invoker consults before falling back to JSON parameter resolution. Remove `RipCurlController` and its error/response records from autoconfigure — consumers write their own controllers.

**Tech Stack:** Java 24, Spring Boot 3.5.3, Jackson (com.fasterxml.jackson), Apache Commons Lang3, JUnit 5, AssertJ

---

### Task 1: Collapse Exception Hierarchy

**Files:**
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/exception/JsonRpcException.java`
- Delete: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/exception/JsonRpcInvalidRequestException.java`
- Delete: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/exception/JsonRpcMethodNotFoundException.java`
- Delete: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/exception/JsonRpcInvalidParamsException.java`
- Delete: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/exception/JsonRpcInternalErrorException.java`
- Delete: `ripcurl-core/src/test/java/com/callibrity/ripcurl/core/exception/JsonRpcInternalErrorExceptionTest.java`
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/def/DefaultJsonRpcDispatcher.java`
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/invoke/JsonMethodInvoker.java`
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/invoke/JsonParameterMapper.java`
- Modify: `ripcurl-core/src/test/java/com/callibrity/ripcurl/core/def/DefaultJsonRpcDispatcherTest.java`
- Modify: `ripcurl-core/src/test/java/com/callibrity/ripcurl/core/invoke/JsonMethodInvokerTest.java`

**Step 1: Rewrite `JsonRpcException`**

Make it concrete with a `code` field and standard JSON-RPC error code constants:

```java
package com.callibrity.ripcurl.core.exception;

public class JsonRpcException extends RuntimeException {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    private final int code;

    public JsonRpcException(int code, String message) {
        super(message);
        this.code = code;
    }

    public JsonRpcException(int code, String message, Exception cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

**Step 2: Delete the four subclass files and the `JsonRpcInternalErrorExceptionTest`**

Remove:
- `JsonRpcInvalidRequestException.java`
- `JsonRpcMethodNotFoundException.java`
- `JsonRpcInvalidParamsException.java`
- `JsonRpcInternalErrorException.java`
- `JsonRpcInternalErrorExceptionTest.java`

**Step 3: Update `DefaultJsonRpcDispatcher`**

Replace all subclass references:

```java
// Line 58: was JsonRpcInvalidRequestException
throw new JsonRpcException(JsonRpcException.INVALID_REQUEST,
    String.format("jsonrpc value must be \"%s\".", VALID_JSONRPC_VERSION));

// Line 61: was JsonRpcInvalidRequestException
throw new JsonRpcException(JsonRpcException.INVALID_REQUEST,
    "JSON-RPC method name is required.");

// Line 64: was JsonRpcInvalidRequestException
throw new JsonRpcException(JsonRpcException.INVALID_REQUEST,
    String.format("Invalid id type (%s). Must be a %s or %s.",
        request.id().getNodeType(), JsonNodeType.STRING, JsonNodeType.NUMBER));

// Line 69: was JsonRpcMethodNotFoundException
throw new JsonRpcException(JsonRpcException.METHOD_NOT_FOUND,
    String.format("JSON-RPC method \"%s\" not found.", request.method()));
```

Update imports: remove all subclass imports, add `JsonRpcException` if not already imported.

**Step 4: Update `JsonMethodInvoker`**

```java
// Line 63: was checking for JsonRpcException subclass — still works, checks base class
if (e.getTargetException() instanceof JsonRpcException jre) {
    throw jre;
}

// Line 66: was JsonRpcInternalErrorException
throw new JsonRpcException(JsonRpcException.INTERNAL_ERROR,
    String.format("Method invocation failed for method %s.", method), e);

// Line 68: was JsonRpcInternalErrorException
throw new JsonRpcException(JsonRpcException.INTERNAL_ERROR,
    String.format("Method invocation failed for method %s.", method), e);

// Line 92: was JsonRpcInvalidParamsException
throw new JsonRpcException(JsonRpcException.INVALID_PARAMS,
    String.format("Unsupported JSON-RPC parameters type %s.", parameters.getNodeType()));
```

Update imports: remove subclass imports.

**Step 5: Update `JsonParameterMapper`**

```java
// Line 59: was JsonRpcInvalidParamsException
throw new JsonRpcException(JsonRpcException.INVALID_PARAMS,
    String.format("Unable to read parameter \"%s\" value (%s).", parameterName, e.getMessage()));
```

Update imports: replace `JsonRpcInvalidParamsException` with `JsonRpcException`.

**Step 6: Update `DefaultJsonRpcDispatcherTest`**

All assertions that check `.isExactlyInstanceOf(JsonRpcInvalidRequestException.class)` or
`.isExactlyInstanceOf(JsonRpcMethodNotFoundException.class)` change to check
`.isExactlyInstanceOf(JsonRpcException.class)` and verify the code:

```java
// For INVALID_REQUEST checks:
assertThatThrownBy(() -> service.dispatch(request))
    .isExactlyInstanceOf(JsonRpcException.class)
    .extracting("code").isEqualTo(JsonRpcException.INVALID_REQUEST);

// For METHOD_NOT_FOUND checks:
assertThatThrownBy(() -> service.dispatch(request))
    .isExactlyInstanceOf(JsonRpcException.class)
    .extracting("code").isEqualTo(JsonRpcException.METHOD_NOT_FOUND);
```

**Step 7: Update `JsonMethodInvokerTest`**

Replace all subclass references:

```java
// was: .isExactlyInstanceOf(JsonRpcInternalErrorException.class)
.isExactlyInstanceOf(JsonRpcException.class)
.extracting("code").isEqualTo(JsonRpcException.INTERNAL_ERROR);

// was: .isExactlyInstanceOf(JsonRpcInvalidParamsException.class)
.isExactlyInstanceOf(JsonRpcException.class)
.extracting("code").isEqualTo(JsonRpcException.INVALID_PARAMS);

// The test that throws JsonRpcInvalidParamsException inside the method:
// DummyService.jsonRpcException() — update to throw JsonRpcException directly:
public String jsonRpcException(String input) {
    throw new JsonRpcException(JsonRpcException.INVALID_PARAMS, "I don't like you!");
}

// And its assertion:
.isExactlyInstanceOf(JsonRpcException.class)
.hasMessage("I don't like you!");
```

**Step 8: Run tests**

Run: `cd /Users/jcarman/IdeaProjects/ripcurl && mvn verify -pl ripcurl-core`
Expected: All core tests pass. Autoconfigure will fail (still references old classes) — that's fine, we fix it in Task 2.

**Step 9: Commit**

```bash
git add -A
git commit -m "refactor: collapse exception hierarchy into single JsonRpcException with error codes"
```

---

### Task 2: Remove Controller and Related Classes

**Files:**
- Delete: `ripcurl-autoconfigure/src/main/java/com/callibrity/ripcurl/autoconfigure/RipCurlController.java`
- Delete: `ripcurl-autoconfigure/src/main/java/com/callibrity/ripcurl/autoconfigure/JsonRpcError.java`
- Delete: `ripcurl-autoconfigure/src/main/java/com/callibrity/ripcurl/autoconfigure/JsonRpcErrorResponse.java`
- Delete: `ripcurl-autoconfigure/src/test/java/com/callibrity/ripcurl/autoconfigure/RipCurlControllerTest.java`
- Delete: `ripcurl-autoconfigure/src/test/java/com/callibrity/ripcurl/autoconfigure/TestRpcConfiguration.java`
- Modify: `ripcurl-autoconfigure/src/main/java/com/callibrity/ripcurl/autoconfigure/RipCurlAutoConfiguration.java`
- Modify: `ripcurl-autoconfigure/pom.xml`

**Step 1: Delete controller, error records, and controller test**

Remove the 5 files listed above.

**Step 2: Remove controller bean from `RipCurlAutoConfiguration`**

Remove the `ripCurlController` bean method:

```java
// DELETE this method:
@Bean
public RipCurlController ripCurlController(JsonRpcDispatcher service) {
    return new RipCurlController(service);
}
```

Remove the import for `RipCurlController` if present.

**Step 3: Remove web dependencies from `ripcurl-autoconfigure/pom.xml`**

Remove these dependencies since they were only needed for the controller:

```xml
<!-- REMOVE -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
</dependency>
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
</dependency>
```

Keep `spring-boot-autoconfigure`, `spring-context`, `jakarta.annotation-api`, and test dependencies.

**Step 4: Run tests**

Run: `cd /Users/jcarman/IdeaProjects/ripcurl && mvn verify`
Expected: BUILD SUCCESS. All remaining tests pass.

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor: remove RipCurlController — consumers provide their own HTTP layer"
```

---

### Task 3: Add `JsonRpcParamResolver` Interface

**Files:**
- Create: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/invoke/JsonRpcParamResolver.java`
- Test: `ripcurl-core/src/test/java/com/callibrity/ripcurl/core/invoke/JsonRpcParamResolverTest.java`

**Step 1: Write the test**

```java
package com.callibrity.ripcurl.core.invoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Parameter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRpcParamResolverTest {

    private final ObjectMapper mapper = new ObjectMapper();

    public static class ServiceWithContext {
        public String greet(String name, StringBuilder context) {
            context.append("called");
            return "Hello, " + name + "!";
        }
    }

    @Test
    void resolverShouldProvideNonJsonParameters() {
        var service = new ServiceWithContext();
        var method = MethodUtils.getMatchingMethod(
            ServiceWithContext.class, "greet", String.class, StringBuilder.class);
        var context = new StringBuilder();

        JsonRpcParamResolver contextResolver = parameter ->
            StringBuilder.class.equals(parameter.getType()) ? context : null;

        var invoker = new JsonMethodInvoker(mapper, service, method, List.of(contextResolver));
        var params = JsonNodeFactory.instance.objectNode().put("name", "World");
        var result = invoker.invoke(params);

        assertThat(result.asText()).isEqualTo("Hello, World!");
        assertThat(context.toString()).isEqualTo("called");
    }

    @Test
    void resolverReturningNullShouldFallBackToJson() {
        var service = new ServiceWithContext();
        var method = MethodUtils.getMatchingMethod(
            ServiceWithContext.class, "greet", String.class, StringBuilder.class);

        // Resolver that never resolves anything
        JsonRpcParamResolver noopResolver = parameter -> null;

        var invoker = new JsonMethodInvoker(mapper, service, method, List.of(noopResolver));
        var params = JsonNodeFactory.instance.objectNode().put("name", "World");
        var result = invoker.invoke(params);

        assertThat(result.asText()).isEqualTo("Hello, null!");
    }

    @Test
    void firstResolvingResolverWins() {
        var service = new ServiceWithContext();
        var method = MethodUtils.getMatchingMethod(
            ServiceWithContext.class, "greet", String.class, StringBuilder.class);
        var first = new StringBuilder("first");
        var second = new StringBuilder("second");

        JsonRpcParamResolver firstResolver = parameter ->
            StringBuilder.class.equals(parameter.getType()) ? first : null;
        JsonRpcParamResolver secondResolver = parameter ->
            StringBuilder.class.equals(parameter.getType()) ? second : null;

        var invoker = new JsonMethodInvoker(mapper, service, method,
            List.of(firstResolver, secondResolver));
        var params = JsonNodeFactory.instance.objectNode().put("name", "World");
        invoker.invoke(params);

        assertThat(first.toString()).isEqualTo("firstcalled");
        assertThat(second.toString()).isEqualTo("second");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd /Users/jcarman/IdeaProjects/ripcurl && mvn test -pl ripcurl-core -Dtest=JsonRpcParamResolverTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — `JsonRpcParamResolver` doesn't exist, `JsonMethodInvoker` doesn't accept resolvers.

**Step 3: Create `JsonRpcParamResolver` interface**

```java
package com.callibrity.ripcurl.core.invoke;

import java.lang.reflect.Parameter;

/**
 * Resolves method parameters that are not present in the JSON-RPC params.
 * Implementations are consulted in order before falling back to JSON
 * parameter resolution. Return {@code null} to indicate this resolver
 * cannot provide a value for the given parameter.
 */
@FunctionalInterface
public interface JsonRpcParamResolver {

    /**
     * Attempts to resolve a value for the given method parameter.
     *
     * @param parameter the method parameter to resolve
     * @return the resolved value, or {@code null} if this resolver cannot handle it
     */
    Object resolve(Parameter parameter);
}
```

**Step 4: Update `JsonMethodInvoker` to accept resolvers**

Add a second constructor that accepts resolvers. Keep the original constructor for backwards compatibility (passes empty list). Update `parseJsonParameters` to consult resolvers first.

```java
private final List<JsonRpcParamResolver> resolvers;

// New constructor
public JsonMethodInvoker(ObjectMapper mapper, Object targetObject, Method method,
                         List<JsonRpcParamResolver> resolvers) {
    this.mapper = mapper;
    this.targetObject = targetObject;
    this.method = method;
    this.resolvers = List.copyOf(resolvers);
    this.parameterMappers = Arrays.stream(method.getParameters())
            .map(p -> new JsonParameterMapper(mapper, targetObject, p))
            .toList();
}

// Existing constructor — delegates with empty resolvers
public JsonMethodInvoker(ObjectMapper mapper, Object targetObject, Method method) {
    this(mapper, targetObject, method, List.of());
}
```

Update `parseJsonParameters` to check resolvers for each parameter before using JSON:

```java
private Object[] parseJsonParameters(JsonNode parameters) {
    if (parameterMappers.isEmpty()) {
        return EMPTY_ARGS;
    }
    var methodParams = method.getParameters();
    var args = new Object[parameterMappers.size()];
    for (int i = 0; i < parameterMappers.size(); i++) {
        Object resolved = resolveFromResolvers(methodParams[i]);
        if (resolved != null) {
            args[i] = resolved;
        } else {
            args[i] = resolveFromJson(parameters, i);
        }
    }
    return args;
}

private Object resolveFromResolvers(Parameter parameter) {
    for (JsonRpcParamResolver resolver : resolvers) {
        Object value = resolver.resolve(parameter);
        if (value != null) {
            return value;
        }
    }
    return null;
}

private Object resolveFromJson(JsonNode parameters, int index) {
    if (parameters == null || parameters.isNull()) {
        return null;
    }
    return switch (parameters.getNodeType()) {
        case OBJECT -> parameterMappers.get(index)
            .mapParameter(parameters.get(parameterMappers.get(index).getParameterName()));
        case ARRAY -> {
            if (index < parameters.size()) {
                yield parameterMappers.get(index).mapParameter(parameters.get(index));
            }
            yield parameterMappers.get(index).mapParameter(NullNode.getInstance());
        }
        default -> throw new JsonRpcException(JsonRpcException.INVALID_PARAMS,
            String.format("Unsupported JSON-RPC parameters type %s.", parameters.getNodeType()));
    };
}
```

**Step 5: Run tests**

Run: `cd /Users/jcarman/IdeaProjects/ripcurl && mvn verify -pl ripcurl-core`
Expected: All tests pass, including new `JsonRpcParamResolverTest`.

**Step 6: Commit**

```bash
git add -A
git commit -m "feat: add JsonRpcParamResolver for pluggable non-JSON parameter injection"
```

---

### Task 4: Wire Resolvers Through Annotation Layer and AutoConfiguration

**Files:**
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/annotation/AnnotationJsonRpcMethod.java`
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/annotation/DefaultAnnotationJsonRpcMethodProviderFactory.java`
- Modify: `ripcurl-core/src/main/java/com/callibrity/ripcurl/core/annotation/AnnotationJsonRpcMethodProviderFactory.java`
- Modify: `ripcurl-autoconfigure/src/main/java/com/callibrity/ripcurl/autoconfigure/RipCurlAutoConfiguration.java`
- Modify: `ripcurl-autoconfigure/src/main/java/com/callibrity/ripcurl/autoconfigure/JsonRpcServiceMethodProvider.java`

**Step 1: Update `AnnotationJsonRpcMethod` to accept resolvers**

```java
// Update createMethods to accept resolvers
public static List<AnnotationJsonRpcMethod> createMethods(
        ObjectMapper mapper, Object targetObject, List<JsonRpcParamResolver> resolvers) {
    return MethodUtils.getMethodsListWithAnnotation(targetObject.getClass(), JsonRpc.class)
            .stream()
            .map(method -> new AnnotationJsonRpcMethod(mapper, targetObject, method, resolvers))
            .toList();
}

// Update constructor to pass resolvers to invoker
AnnotationJsonRpcMethod(ObjectMapper mapper, Object targetObject, Method method,
                        List<JsonRpcParamResolver> resolvers) {
    this.invoker = new JsonMethodInvoker(mapper, targetObject, method, resolvers);
    var annotation = method.getAnnotation(JsonRpc.class);
    this.name = ofNullable(StringUtils.trimToNull(annotation.value()))
            .orElseGet(() -> String.format("%s.%s",
                ClassUtils.getSimpleName(targetObject), method.getName()));
}
```

**Step 2: Update `AnnotationJsonRpcMethodProviderFactory`**

The interface stays the same — the resolvers are injected into the factory implementation, not passed per-call.

**Step 3: Update `DefaultAnnotationJsonRpcMethodProviderFactory`**

```java
public class DefaultAnnotationJsonRpcMethodProviderFactory
        implements AnnotationJsonRpcMethodProviderFactory {

    private final ObjectMapper mapper;
    private final List<JsonRpcParamResolver> resolvers;

    public DefaultAnnotationJsonRpcMethodProviderFactory(ObjectMapper mapper,
            List<JsonRpcParamResolver> resolvers) {
        this.mapper = mapper;
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public JsonRpcMethodProvider create(Object targetObject) {
        var methods = AnnotationJsonRpcMethod.createMethods(mapper, targetObject, resolvers);
        return () -> List.copyOf(methods);
    }
}
```

**Step 4: Update `JsonRpcServiceMethodProvider`**

```java
public class JsonRpcServiceMethodProvider implements JsonRpcMethodProvider {

    private List<AnnotationJsonRpcMethod> methods;
    private final ApplicationContext ctx;
    private final ObjectMapper mapper;
    private final List<JsonRpcParamResolver> resolvers;

    public JsonRpcServiceMethodProvider(ApplicationContext ctx, ObjectMapper mapper,
            List<JsonRpcParamResolver> resolvers) {
        this.ctx = ctx;
        this.mapper = mapper;
        this.resolvers = resolvers;
    }

    @Override
    public List<JsonRpcMethod> getJsonRpcMethodHandlers() {
        return List.copyOf(methods);
    }

    @PostConstruct
    public void initialize() {
        this.methods = ctx.getBeansWithAnnotation(JsonRpcService.class).values().stream()
                .flatMap(bean -> AnnotationJsonRpcMethod.createMethods(mapper, bean, resolvers)
                    .stream())
                .toList();
    }
}
```

**Step 5: Update `RipCurlAutoConfiguration`**

Inject resolvers from the Spring context (empty list if none registered):

```java
@Bean
public JsonRpcServiceMethodProvider jsonRpcServiceMethodProvider(
        ApplicationContext ctx, ObjectMapper mapper,
        @Autowired(required = false) List<JsonRpcParamResolver> resolvers) {
    return new JsonRpcServiceMethodProvider(ctx, mapper,
        resolvers != null ? resolvers : List.of());
}

@Bean
public AnnotationJsonRpcMethodProviderFactory annotationJsonRpcMethodHandlerProviderFactory(
        ObjectMapper objectMapper,
        @Autowired(required = false) List<JsonRpcParamResolver> resolvers) {
    return new DefaultAnnotationJsonRpcMethodProviderFactory(objectMapper,
        resolvers != null ? resolvers : List.of());
}
```

Add import for `JsonRpcParamResolver` and `org.springframework.beans.factory.annotation.Autowired`.

**Step 6: Run tests**

Run: `cd /Users/jcarman/IdeaProjects/ripcurl && mvn verify`
Expected: BUILD SUCCESS.

**Step 7: Commit**

```bash
git add -A
git commit -m "feat: wire JsonRpcParamResolver through annotation layer and autoconfiguration"
```

---

### Task 5: Final Verification

**Step 1: Run full build**

Run: `cd /Users/jcarman/IdeaProjects/ripcurl && mvn verify`
Expected: BUILD SUCCESS with all tests passing.

**Step 2: Verify no references to deleted classes remain**

Run: `grep -r "JsonRpcInvalidRequestException\|JsonRpcMethodNotFoundException\|JsonRpcInvalidParamsException\|JsonRpcInternalErrorException\|RipCurlController\|JsonRpcError\b\|JsonRpcErrorResponse" /Users/jcarman/IdeaProjects/ripcurl/ripcurl-core/src /Users/jcarman/IdeaProjects/ripcurl/ripcurl-autoconfigure/src`
Expected: No matches.

**Step 3: Commit if any cleanup needed**

```bash
git add -A
git commit -m "chore: final cleanup after RipCurl refactor"
```
