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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.callibrity.ripcurl.core.annotation.JsonRpcMethod;
import com.callibrity.ripcurl.core.annotation.JsonRpcParams;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

class JsonRpcMethodAotProcessorTest {

  public record GreetingParams(String name, int times) {}

  public record GreetingResult(String message) {}

  public record UnboundParam(String value) {}

  public static class AnnotatedService {

    @JsonRpcMethod("greet")
    public GreetingResult greet(@JsonRpcParams GreetingParams params) {
      return new GreetingResult("hi " + params.name());
    }

    @JsonRpcMethod("ping")
    public void ping() {
      // fire-and-forget
    }

    @JsonRpcMethod("echo")
    public GreetingResult echo(UnboundParam unbound) {
      return new GreetingResult(unbound.value());
    }

    @JsonRpcMethod("noop")
    public Void noop() {
      return null;
    }

    // Not annotated — must be ignored.
    public String unrelated() {
      return "";
    }
  }

  public static class UnannotatedBean {
    public String something() {
      return "";
    }
  }

  private final JsonRpcMethodAotProcessor processor = new JsonRpcMethodAotProcessor();

  @Test
  void returnsNullContributionForBeansWithoutJsonRpcMethods() {
    var bean = registeredBean("unannotated", UnannotatedBean.class);

    assertThat(processor.processAheadOfTime(bean)).isNull();
  }

  @Test
  void returnsContributionForBeansWithJsonRpcMethods() {
    var bean = registeredBean("annotated", AnnotatedService.class);

    assertThat(processor.processAheadOfTime(bean)).isNotNull();
  }

  @Test
  void registersInvokeHintForEveryJsonRpcMethod() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .anyMatch(
            th ->
                th.getType().equals(TypeReference.of(AnnotatedService.class))
                    && th.methods()
                        .anyMatch(
                            m ->
                                m.getName().equals("greet") && m.getMode() == ExecutableMode.INVOKE)
                    && th.methods()
                        .anyMatch(
                            m ->
                                m.getName().equals("ping")
                                    && m.getMode() == ExecutableMode.INVOKE));
  }

  @Test
  void doesNotRegisterInvokeHintForUnannotatedMethods() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .filteredOn(th -> th.getType().equals(TypeReference.of(AnnotatedService.class)))
        .singleElement()
        .satisfies(th -> assertThat(th.methods()).noneMatch(m -> m.getName().equals("unrelated")));
  }

  @Test
  void registersBindingHintsForJsonRpcParamsType() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .anyMatch(th -> th.getType().equals(TypeReference.of(GreetingParams.class)));
  }

  @Test
  void registersBindingHintsForReturnType() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .anyMatch(th -> th.getType().equals(TypeReference.of(GreetingResult.class)));
  }

  @Test
  void skipsBindingHintsForVoidReturn() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .noneMatch(th -> th.getType().equals(TypeReference.of(void.class)));
  }

  @Test
  void skipsBindingHintsForBoxedVoidReturn() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .noneMatch(th -> th.getType().equals(TypeReference.of(Void.class)));
  }

  @Test
  void doesNotRegisterBindingHintsForParametersWithoutJsonRpcParams() {
    var hints = applyContribution(AnnotatedService.class);

    assertThat(hints.reflection().typeHints())
        .noneMatch(th -> th.getType().equals(TypeReference.of(UnboundParam.class)));
  }

  private RuntimeHints applyContribution(Class<?> beanClass) {
    var bean = registeredBean("bean", beanClass);
    BeanRegistrationAotContribution contribution = processor.processAheadOfTime(bean);
    assertThat(contribution).isNotNull();

    var hints = new RuntimeHints();
    var genContext = mock(GenerationContext.class);
    when(genContext.getRuntimeHints()).thenReturn(hints);
    contribution.applyTo(genContext, null);
    return hints;
  }

  private static RegisteredBean registeredBean(String name, Class<?> type) {
    var factory = new DefaultListableBeanFactory();
    factory.registerBeanDefinition(name, new RootBeanDefinition(type));
    return RegisteredBean.of(factory, name);
  }
}
