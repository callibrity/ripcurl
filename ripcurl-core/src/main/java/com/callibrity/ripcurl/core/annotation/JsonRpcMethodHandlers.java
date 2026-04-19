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

import static java.util.Optional.ofNullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.jwcarman.methodical.MethodInvoker;
import org.jwcarman.methodical.MethodInvokerFactory;
import org.jwcarman.methodical.intercept.MethodInterceptor;
import org.jwcarman.methodical.jackson3.Jackson3ParameterResolver;
import org.jwcarman.methodical.param.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Pure-Java factory that builds one {@link JsonRpcMethodHandler} from a {@code
 * (bean, @JsonRpcMethod method)} pair. Bean discovery happens in the autoconfiguration; this helper
 * only constructs.
 *
 * <p><b>Resolver chain.</b> RipCurl's resolver chain is fixed at the boundaries: {@link
 * JsonRpcParamsResolver} is always first (so {@code @JsonRpcParams}-annotated parameters
 * deserialize the whole params blob into a typed record), and {@link Jackson3ParameterResolver} is
 * always last (the name/index catch-all that binds plain parameters by field name or array
 * position). Customizer-contributed resolvers slot between those two, in customizer-iteration
 * order. This ordering is enforced by the builder; there is no way to insert before the
 * {@code @JsonRpcParams} resolver or after the Jackson catch-all.
 */
public final class JsonRpcMethodHandlers {

  private static final Logger log = LoggerFactory.getLogger(JsonRpcMethodHandlers.class);

  private JsonRpcMethodHandlers() {}

  /**
   * Builds one handler for the given {@code (bean, method)} pair. Runs every supplied customizer
   * against a fresh config so customizers may append resolvers and interceptors scoped to this
   * handler. The final invoker chain is assembled as: {@code JsonRpcParamsResolver} (built-in
   * head), customizer-added resolvers, {@code Jackson3ParameterResolver} (built-in tail), then
   * customizer-added interceptors in iteration order. Methodical's {@code @Argument} tail resolver
   * still runs after the Jackson catch-all.
   */
  public static JsonRpcMethodHandler build(
      Object bean,
      Method method,
      ObjectMapper mapper,
      MethodInvokerFactory invokerFactory,
      List<JsonRpcMethodHandlerCustomizer> customizers) {
    var annotation = method.getAnnotation(JsonRpcMethod.class);
    String className = ClassUtils.getSimpleName(bean);
    String methodName = method.getName();
    String name =
        ofNullable(StringUtils.trimToNull(annotation.value()))
            .orElseGet(() -> String.format("%s.%s", className, methodName));

    var config = new MutableConfig(name, method, bean);
    customizers.forEach(c -> c.customize(config));

    var paramsResolver = new JsonRpcParamsResolver(mapper);
    var jacksonResolver = new Jackson3ParameterResolver(mapper);
    List<ParameterResolver<? super JsonNode>> userResolvers = config.freezeResolvers();
    List<MethodInterceptor<? super JsonNode>> interceptors = config.freezeInterceptors();

    MethodInvoker<JsonNode> invoker =
        invokerFactory.create(
            method,
            bean,
            JsonNode.class,
            cfg -> {
              cfg.resolver(paramsResolver);
              userResolvers.forEach(cfg::resolver);
              cfg.resolver(jacksonResolver);
              interceptors.forEach(cfg::interceptor);
            });

    log.debug("Built @JsonRpcMethod handler '{}' on {}.{}", name, className, methodName);
    return new JsonRpcMethodHandler(name, method, bean, invoker, mapper);
  }

  private static final class MutableConfig implements JsonRpcMethodHandlerConfig {
    private final String name;
    private final Method method;
    private final Object bean;
    private final List<ParameterResolver<? super JsonNode>> resolvers = new ArrayList<>();
    private final List<MethodInterceptor<? super JsonNode>> interceptors = new ArrayList<>();

    MutableConfig(String name, Method method, Object bean) {
      this.name = name;
      this.method = method;
      this.bean = bean;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Method method() {
      return method;
    }

    @Override
    public Object bean() {
      return bean;
    }

    @Override
    public JsonRpcMethodHandlerConfig resolver(ParameterResolver<? super JsonNode> resolver) {
      resolvers.add(resolver);
      return this;
    }

    @Override
    public JsonRpcMethodHandlerConfig interceptor(MethodInterceptor<? super JsonNode> interceptor) {
      interceptors.add(interceptor);
      return this;
    }

    List<ParameterResolver<? super JsonNode>> freezeResolvers() {
      return List.copyOf(resolvers);
    }

    List<MethodInterceptor<? super JsonNode>> freezeInterceptors() {
      return List.copyOf(interceptors);
    }
  }
}
