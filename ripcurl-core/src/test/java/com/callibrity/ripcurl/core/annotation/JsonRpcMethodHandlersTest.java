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
package com.callibrity.ripcurl.core.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.callibrity.ripcurl.core.JsonRpcCall;
import com.callibrity.ripcurl.core.JsonRpcNotification;
import com.callibrity.ripcurl.core.JsonRpcResult;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.MethodInvocation;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

class JsonRpcMethodHandlersTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static class NamedService {
    @JsonRpcMethod("custom.name")
    public String withExplicitName() {
      return "hi";
    }

    @JsonRpcMethod
    public String withDefaultName() {
      return "hi";
    }

    @JsonRpcMethod("   ")
    public String withBlankName() {
      return "hi";
    }
  }

  @Test
  void explicitAnnotationValueWins() {
    var handler = build(NamedService.class, "withExplicitName");
    assertThat(handler.name()).isEqualTo("custom.name");
  }

  @Test
  void blankAnnotationValueFallsBackToClassDotMethod() {
    var handler = build(NamedService.class, "withDefaultName");
    assertThat(handler.name()).isEqualTo("NamedService.withDefaultName");
  }

  @Test
  void whitespaceAnnotationValueIsTreatedAsBlank() {
    var handler = build(NamedService.class, "withBlankName");
    assertThat(handler.name()).isEqualTo("NamedService.withBlankName");
  }

  @Test
  void methodAndBeanAccessorsExposeSource() throws Exception {
    var bean = new NamedService();
    var method = NamedService.class.getMethod("withExplicitName");
    var handler = JsonRpcMethodHandlers.build(bean, method, MAPPER, List.of());
    assertThat(handler.bean()).isSameAs(bean);
    assertThat(handler.method()).isSameAs(method);
  }

  @Test
  void customizerSeesHandlerMetadataOnTheConfig() throws Exception {
    // The config passed to each customizer exposes the handler's name, method, and bean so the
    // customizer can bake per-handler state (e.g., metric tags) into the interceptor at
    // construction time.
    var bean = new NamedService();
    var method = NamedService.class.getMethod("withExplicitName");
    var seenNames = new ArrayList<String>();
    var seenMethods = new ArrayList<Method>();
    var seenBeans = new ArrayList<Object>();
    JsonRpcMethodHandlerCustomizer customizer =
        config -> {
          seenNames.add(config.name());
          seenMethods.add(config.method());
          seenBeans.add(config.bean());
        };

    JsonRpcMethodHandlers.build(bean, method, MAPPER, List.of(customizer));

    assertThat(seenNames).containsExactly("custom.name");
    assertThat(seenMethods).containsExactly(method);
    assertThat(seenBeans).containsExactly(bean);
  }

  @Test
  void customizerCanAttachInterceptorScopedToThisHandler() throws Exception {
    var bean = new NamedService();
    var method = NamedService.class.getMethod("withExplicitName");
    var count = new AtomicInteger();
    JsonRpcMethodHandlerCustomizer customizer =
        config -> config.interceptor(invocation -> intercept(count, invocation));

    var handler = JsonRpcMethodHandlers.build(bean, method, MAPPER, List.of(customizer));
    handler.call(new JsonRpcCall("2.0", "custom.name", null, StringNode.valueOf("1")));

    assertThat(count).hasValue(1);
  }

  @Test
  void customizerCanAttachResolverScopedToThisHandler() throws Exception {
    // Register a resolver that supports any parameter and injects a fixed String. Slotted between
    // the built-in @JsonRpcParams resolver and the Jackson name/index catch-all.
    var bean = new EchoService();
    var method = EchoService.class.getMethod("echo", String.class);
    var captured = new ArrayList<String>();
    JsonRpcMethodHandlerCustomizer customizer =
        config ->
            config.resolver(
                info -> {
                  if (!info.accepts(String.class)) {
                    return java.util.Optional.empty();
                  }
                  return java.util.Optional.of(
                      params -> {
                        captured.add("resolver-hit");
                        return "resolved-by-customizer";
                      });
                });

    var handler = JsonRpcMethodHandlers.build(bean, method, MAPPER, List.of(customizer));
    var response =
        handler.call(
            new JsonRpcCall("2.0", "echo", MAPPER.createObjectNode(), StringNode.valueOf("1")));

    assertThat(captured).containsExactly("resolver-hit");
    assertThat(response).isInstanceOf(JsonRpcResult.class);
    assertThat(response.result().asString()).isEqualTo("resolved-by-customizer");
  }

  @Test
  void notificationInvocationTakesTheNullIdBranch() throws Exception {
    // For notifications the handler should still produce a JsonRpcResult, but with a null id —
    // the `JsonRpcRequest instanceof JsonRpcCall` check is false, so id defaults to null. The
    // dispatcher discards this result for notifications; we only verify the id-derivation branch.
    var bean = new NamedService();
    var method = NamedService.class.getMethod("withExplicitName");
    var handler = JsonRpcMethodHandlers.build(bean, method, MAPPER, List.of());

    JsonRpcResult result = handler.call(new JsonRpcNotification("2.0", "custom.name", null));

    assertThat(result.id()).isNull();
    assertThat(result.result().asString()).isEqualTo("hi");
  }

  public static class EchoService {
    @JsonRpcMethod("echo")
    public String echo(String value) {
      return value;
    }
  }

  private static JsonRpcMethodHandler build(Class<?> beanClass, String methodName) {
    try {
      var bean = beanClass.getDeclaredConstructor().newInstance();
      Method method = findMethod(bean, methodName);
      return JsonRpcMethodHandlers.build(bean, method, MAPPER, List.of());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Method findMethod(Object bean, String methodName) {
    return MethodUtils.getMethodsListWithAnnotation(bean.getClass(), JsonRpcMethod.class).stream()
        .filter(m -> m.getName().equals(methodName))
        .findFirst()
        .orElseThrow();
  }

  private static Object intercept(AtomicInteger count, MethodInvocation<?> invocation) {
    count.incrementAndGet();
    return invocation.proceed();
  }
}
