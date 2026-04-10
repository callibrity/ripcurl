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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.Test;
import org.jwcarman.methodical.ParameterResolutionException;
import org.jwcarman.methodical.param.ParameterInfo;
import org.jwcarman.methodical.reflect.Types;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;

class JsonRpcParamsResolverTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public record Point(int x, int y) {}

  public record Greeting(String message, int count) {}

  public static class Target {
    public void pointMethod(@JsonRpcParams Point point) {}

    public void greetingMethod(@JsonRpcParams Greeting greeting) {}

    public void unannotatedMethod(String name) {}
  }

  @Test
  void supportsAnnotatedParameter() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("pointMethod", 0);

    assertThat(resolver.supports(info)).isTrue();
  }

  @Test
  void doesNotSupportUnannotatedParameter() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("unannotatedMethod", 0);

    assertThat(resolver.supports(info)).isFalse();
  }

  @Test
  void deserializesParamsIntoRecord() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("pointMethod", 0);
    JsonNode params = MAPPER.readTree("{\"x\":1,\"y\":2}");

    Object result = resolver.resolve(info, params);

    assertThat(result).isInstanceOf(Point.class);
    assertThat((Point) result).isEqualTo(new Point(1, 2));
  }

  @Test
  void deserializesComplexRecord() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("greetingMethod", 0);
    JsonNode params = MAPPER.readTree("{\"message\":\"hello\",\"count\":3}");

    Object result = resolver.resolve(info, params);

    assertThat(result).isInstanceOf(Greeting.class);
    assertThat((Greeting) result).isEqualTo(new Greeting("hello", 3));
  }

  @Test
  void nullParamsReturnsNull() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("pointMethod", 0);

    assertThat(resolver.resolve(info, null)).isNull();
  }

  @Test
  void nullNodeParamsReturnsNull() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("pointMethod", 0);

    assertThat(resolver.resolve(info, NullNode.getInstance())).isNull();
  }

  @Test
  void invalidShapeThrowsParameterResolutionException() throws Exception {
    var resolver = new JsonRpcParamsResolver(MAPPER);
    ParameterInfo info = paramInfo("pointMethod", 0);
    JsonNode params = MAPPER.readTree("{\"x\":\"not-a-number\",\"y\":2}");

    assertThatThrownBy(() -> resolver.resolve(info, params))
        .isInstanceOf(ParameterResolutionException.class)
        .hasMessageContaining("@JsonRpcParams");
  }

  private static ParameterInfo paramInfo(String methodName, int index) throws Exception {
    Method method = Target.class.getMethod(methodName, parameterTypes(methodName));
    Parameter parameter = method.getParameters()[index];
    Class<?> resolvedType = Types.resolveParameterType(parameter, Target.class);
    return ParameterInfo.of(parameter, index, resolvedType, parameter.getParameterizedType());
  }

  private static Class<?>[] parameterTypes(String methodName) {
    return switch (methodName) {
      case "pointMethod" -> new Class<?>[] {Point.class};
      case "greetingMethod" -> new Class<?>[] {Greeting.class};
      case "unannotatedMethod" -> new Class<?>[] {String.class};
      default -> throw new IllegalArgumentException(methodName);
    };
  }
}
