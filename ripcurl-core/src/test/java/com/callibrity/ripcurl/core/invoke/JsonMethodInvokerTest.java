package com.callibrity.ripcurl.core.invoke;

import com.callibrity.ripcurl.core.exception.JsonRpcInternalErrorException;
import com.callibrity.ripcurl.core.exception.JsonRpcInvalidParamsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMethodInvokerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    public abstract static class EchoService<T> {
        public T echo(T original) {
            return original;
        }
    }

    public static class StringEchoService extends EchoService<String> {

    }

    public static class IntegerEchoService extends EchoService<Integer> {

    }

    static class DummyService {
        public String echo(String input) {
            return input;
        }

        public String concat(String a, String b) {
            return a + b;
        }

        public void doNothingPrimitive() {
            // Do nothing
        }

        public Void doNothingObject() {
            return null;
        }

        public String evilMethod(String input) {
            throw new IllegalArgumentException(String.format("Invalid input: %s", input));
        }
    }

    @Test
    void testInvokeMethodWithTypeParameters() {
        var service = new StringEchoService();
        var method = MethodUtils.getMatchingMethod(StringEchoService.class, "echo", String.class);
        var invoker = new JsonMethodInvoker(mapper, service, method);

        var params = JsonNodeFactory.instance.objectNode();
        params.put("original", "Hello");

        var result = invoker.invoke(params);

        assertThat(result.isTextual()).isTrue();
    }

    @Test
    void testInvokeWithMethodThrowingException() throws Exception {
        var service = new DummyService();
        var method = DummyService.class.getDeclaredMethod("evilMethod", String.class);
        var invoker = new JsonMethodInvoker(mapper, service, method);
        var params = JsonNodeFactory.instance.objectNode();
        params.put("input", "Hello");

        assertThatThrownBy(() -> invoker.invoke(params))
                .isExactlyInstanceOf(JsonRpcInternalErrorException.class);
    }

    @Test
    void testInvokeWithNamedParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("concat", String.class, String.class);

        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("a", "Hello ");
        params.put("b", "World");

        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(params);

        assertEquals("Hello World", result.asText());
    }

    @Test
    void testInvokeWithInvalidNamedParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("concat", String.class, String.class);

        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.putObject("a");
        params.putObject("b");

        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        assertThatThrownBy(() -> invoker.invoke(params))
                .isExactlyInstanceOf(JsonRpcInvalidParamsException.class);
    }

    @Test
    void testInvokeWithPositionalParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("concat", String.class, String.class);

        ArrayNode params = JsonNodeFactory.instance.arrayNode();
        params.add("Foo");
        params.add("Bar");

        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(params);

        assertEquals("FooBar", result.asText());
    }


    @Test
    void testInvokeWithJsonNullMissingParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("echo", String.class);
        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(NullNode.getInstance());
        assertTrue(result.isNull());
    }

    @Test
    void testInvokeWithNullMissingParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("echo", String.class);
        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(null);
        assertTrue(result.isNull());
    }

    @Test
    void testInvokeWithMissingNamedParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("concat", String.class, String.class);

        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("a", "Hello ");

        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(params);

        assertEquals("Hello null", result.asText());
    }

    @Test
    void testInvokeWithInvalidParametersType() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("echo", String.class);
        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        var parameters = TextNode.valueOf("Hello World");
        assertThatThrownBy(() -> invoker.invoke(parameters))
                .isExactlyInstanceOf(JsonRpcInvalidParamsException.class);
    }

    @Test
    void testInvokeWithTooFewPositionalParameters() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("concat", String.class, String.class);

        ArrayNode params = JsonNodeFactory.instance.arrayNode();
        params.add("Hello"); // Only one parameter

        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(params);

        assertEquals("Hellonull", result.asText());
    }

    @Test
    void testInvokeWithVoidPrimitiveReturnType() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("doNothingPrimitive");
        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(null);
        assertTrue(result.isNull());
    }

    @Test
    void testInvokeWithVoidObjectReturnType() throws Exception {
        DummyService service = new DummyService();
        Method method = DummyService.class.getMethod("doNothingObject");
        JsonMethodInvoker invoker = new JsonMethodInvoker(mapper, service, method);
        JsonNode result = invoker.invoke(null);
        assertTrue(result.isNull());
    }
}