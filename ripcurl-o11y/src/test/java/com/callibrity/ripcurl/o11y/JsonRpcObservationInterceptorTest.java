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
package com.callibrity.ripcurl.o11y;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcDispatcher;
import com.callibrity.ripcurl.core.JsonRpcNotification;
import com.callibrity.ripcurl.core.JsonRpcProtocol;
import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslator;
import com.callibrity.ripcurl.core.def.DefaultJsonRpcExceptionTranslatorRegistry;
import com.callibrity.ripcurl.core.def.IllegalArgumentExceptionTranslator;
import com.callibrity.ripcurl.core.exception.JsonRpcException;
import com.callibrity.ripcurl.core.spi.JsonRpcExceptionTranslatorRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.MethodInvocation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.IntNode;

class JsonRpcObservationInterceptorTest {

  private static final String METHOD = "tools/call";

  private TestObservationRegistry registry;
  private JsonRpcExceptionTranslatorRegistry translators;
  private JsonRpcObservationInterceptor interceptor;

  @BeforeEach
  void setUp() {
    registry = TestObservationRegistry.create();
    // Mirrors the default set autowired by RipCurlAutoConfiguration — JsonRpcException passes its
    // own code through, IllegalArgumentException maps to -32602, anything else falls through to
    // the registry's built-in -32603 fallback.
    translators =
        new DefaultJsonRpcExceptionTranslatorRegistry(
            List.of(
                new DefaultJsonRpcExceptionTranslator(), new IllegalArgumentExceptionTranslator()));
    interceptor = new JsonRpcObservationInterceptor(registry, translators, METHOD);
  }

  @Test
  void success_sets_base_attrs_and_omits_status_and_error_type() {
    Object result = interceptor.intercept(invocationReturning("payload"));

    org.assertj.core.api.Assertions.assertThat(result).isEqualTo("payload");
    assertThat(registry)
        .hasSingleObservationThat()
        .hasContextualNameEqualTo(METHOD)
        .hasLowCardinalityKeyValue("rpc.system.name", "jsonrpc")
        .hasLowCardinalityKeyValue("jsonrpc.protocol.version", JsonRpcProtocol.VERSION)
        .doesNotHaveLowCardinalityKeyValueWithKey("rpc.response.status_code")
        .doesNotHaveLowCardinalityKeyValueWithKey("error.type");
  }

  @Test
  void jsonrpc_exception_records_code_from_its_own_translator() {
    var invocation = invocationThrowing(new JsonRpcException(-32602, "bad params"));

    assertThatThrownBy(() -> interceptor.intercept(invocation))
        .isInstanceOf(JsonRpcException.class);

    assertThat(registry)
        .hasSingleObservationThat()
        .hasLowCardinalityKeyValue("rpc.response.status_code", "-32602")
        .hasLowCardinalityKeyValue("error.type", "-32602")
        .hasError();
  }

  @Test
  void illegal_argument_exception_records_translated_code() {
    // IllegalArgumentExceptionTranslator maps IAE to -32602. The observation should carry the
    // translated code — matching what the client sees — not the Java class name.
    var invocation = invocationThrowing(new IllegalArgumentException("bad"));

    assertThatThrownBy(() -> interceptor.intercept(invocation))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(registry)
        .hasSingleObservationThat()
        .hasLowCardinalityKeyValue("rpc.response.status_code", "-32602")
        .hasLowCardinalityKeyValue("error.type", "-32602")
        .hasError();
  }

  @Test
  void unmapped_runtime_exception_falls_back_to_internal_error_code() {
    // No translator matches IllegalStateException → registry's built-in fallback emits -32603.
    var invocation = invocationThrowing(new IllegalStateException("boom"));

    assertThatThrownBy(() -> interceptor.intercept(invocation))
        .isInstanceOf(IllegalStateException.class);

    assertThat(registry)
        .hasSingleObservationThat()
        .hasLowCardinalityKeyValue("rpc.response.status_code", "-32603")
        .hasLowCardinalityKeyValue("error.type", "-32603")
        .hasError();
  }

  @Test
  void request_id_set_when_scopedvalue_bound() {
    var call = new JsonRpcCall(JsonRpcProtocol.VERSION, METHOD, null, IntNode.valueOf(42));

    ScopedValue.where(JsonRpcDispatcher.CURRENT_REQUEST, (JsonRpcRequest) call)
        .run(() -> interceptor.intercept(invocationReturning("ok")));

    assertThat(registry)
        .hasSingleObservationThat()
        .hasHighCardinalityKeyValue("jsonrpc.request.id", "42");
  }

  @Test
  void request_id_absent_when_scopedvalue_unbound() {
    interceptor.intercept(invocationReturning("ok"));

    assertThat(registry)
        .hasSingleObservationThat()
        .doesNotHaveHighCardinalityKeyValueWithKey("jsonrpc.request.id");
  }

  @Test
  void request_id_absent_for_notification() {
    var notification = new JsonRpcNotification(JsonRpcProtocol.VERSION, METHOD, null);

    ScopedValue.where(JsonRpcDispatcher.CURRENT_REQUEST, (JsonRpcRequest) notification)
        .run(() -> interceptor.intercept(invocationReturning(null)));

    assertThat(registry)
        .hasSingleObservationThat()
        .doesNotHaveHighCardinalityKeyValueWithKey("jsonrpc.request.id");
  }

  private static MethodInvocation<JsonNode> invocationReturning(Object result) {
    return new FakeInvocation(() -> result);
  }

  private static MethodInvocation<JsonNode> invocationThrowing(RuntimeException e) {
    return new FakeInvocation(
        () -> {
          throw e;
        });
  }

  /** Plain-Java fake — dodges Mockito's unchecked-generic warning on the interface mock. */
  private record FakeInvocation(Supplier<Object> body) implements MethodInvocation<JsonNode> {
    @Override
    public Method method() {
      return null;
    }

    @Override
    public Object target() {
      return null;
    }

    @Override
    public JsonNode argument() {
      return null;
    }

    @Override
    public Object[] resolvedParameters() {
      return new Object[0];
    }

    @Override
    public Object proceed() {
      return body.get();
    }
  }
}
