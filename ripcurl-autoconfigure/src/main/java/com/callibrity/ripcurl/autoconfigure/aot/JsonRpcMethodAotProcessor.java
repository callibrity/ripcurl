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
package com.callibrity.ripcurl.autoconfigure.aot;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcParams;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.ClassUtils;

/**
 * Registers AOT reflection hints for every {@link JsonRpcMethod}-annotated method on any bean. For
 * each annotated method: an {@link ExecutableMode#INVOKE} hint on the method itself so the runtime
 * dispatcher can invoke it reflectively, plus Jackson binding hints on each {@link JsonRpcParams}
 * parameter type and on the method's return type (skipped for {@code void} / {@code Void}).
 *
 * <p>Beans without any {@code @JsonRpcMethod} methods are ignored. No-op for non-native (JIT)
 * builds.
 */
public class JsonRpcMethodAotProcessor implements BeanRegistrationAotProcessor {

  private static final BindingReflectionHintsRegistrar BINDING =
      new BindingReflectionHintsRegistrar();

  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> userClass = ClassUtils.getUserClass(registeredBean.getBeanClass());
    if (MethodUtils.getMethodsListWithAnnotation(userClass, JsonRpcMethod.class).isEmpty()) {
      return null;
    }
    return (generationContext, beanRegistrationCode) ->
        registerHints(generationContext.getRuntimeHints(), userClass);
  }

  private static void registerHints(RuntimeHints hints, Class<?> beanClass) {
    for (Method method : MethodUtils.getMethodsListWithAnnotation(beanClass, JsonRpcMethod.class)) {
      hints.reflection().registerMethod(method, ExecutableMode.INVOKE);
      for (Parameter parameter : method.getParameters()) {
        if (parameter.isAnnotationPresent(JsonRpcParams.class)) {
          BINDING.registerReflectionHints(hints.reflection(), parameter.getParameterizedType());
        }
      }
      if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
        BINDING.registerReflectionHints(hints.reflection(), method.getGenericReturnType());
      }
    }
  }
}
