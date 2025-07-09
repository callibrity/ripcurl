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
package com.callibrity.ripcurl.core.def;

import com.callibrity.ripcurl.core.JsonRpcRequest;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodHandlerProviderFactory;
import com.callibrity.ripcurl.core.annotation.DefaultAnnotationJsonRpcMethodHandlerProviderFactory;
import com.callibrity.ripcurl.core.annotation.JsonRpc;
import com.callibrity.ripcurl.core.exception.JsonRpcInvalidRequestException;
import com.callibrity.ripcurl.core.exception.JsonRpcMethodNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultJsonRpcServiceTest {

    private final AnnotationJsonRpcMethodHandlerProviderFactory factory = new DefaultAnnotationJsonRpcMethodHandlerProviderFactory(new ObjectMapper());

    public static class HelloService {
        @JsonRpc
        public String sayHello(String name) {
            return String.format("Hello, %s!", name);
        }
    }

    @Test
    void shouldReturnResponseForValidRequest() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of(factory.create(new HelloService())));

        var id = TextNode.valueOf("123");
        var response = service.execute(new JsonRpcRequest("2.0", "HelloService.sayHello", mapper.createObjectNode().put("name", "World"), id));
        assertThat(response).isNotNull();
        assertThat(response.id()).isSameAs(id);
        assertThat(response.result().textValue()).isEqualTo("Hello, World!");
    }

    @Test
    void missingMethodNameShouldThrowException() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of(factory.create(new HelloService())));

        var id = TextNode.valueOf("123");
        var request = new JsonRpcRequest("2.0", "bogus", mapper.createObjectNode().put("name", "World"), id);
        assertThatThrownBy(() -> service.execute(request))
                .isExactlyInstanceOf(JsonRpcMethodNotFoundException.class);
    }

    @Test
    void shouldAllowNumericIds() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of(factory.create(new HelloService())));

        var id = IntNode.valueOf(123);
        var response = service.execute(new JsonRpcRequest("2.0", "HelloService.sayHello", mapper.createObjectNode().put("name", "World"), id));
        assertThat(response).isNotNull();
        assertThat(response.id()).isSameAs(id);
        assertThat(response.result().textValue()).isEqualTo("Hello, World!");
    }

    @Test
    void shouldReturnNullForNotificationRequest() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of(factory.create(new HelloService())));

        var response = service.execute(new JsonRpcRequest("2.0", "HelloService.sayHello", mapper.createObjectNode().put("name", "World"), null));
        assertThat(response).isNull();
    }

    @Test
    void invalidJsonRpcValueShouldThrowException() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of());
        var request = new JsonRpcRequest("2.01", "HelloService.sayHello", mapper.createObjectNode().put("name", "World"), TextNode.valueOf("123"));
        assertThatThrownBy(() -> service.execute(request))
                .isExactlyInstanceOf(JsonRpcInvalidRequestException.class);
    }

    @Test
    void missingMethodShouldThrowException() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of());
        var request = new JsonRpcRequest("2.0", null, mapper.createObjectNode().put("name", "World"), TextNode.valueOf("123"));
        assertThatThrownBy(() -> service.execute(request))
                .isExactlyInstanceOf(JsonRpcInvalidRequestException.class);
    }

    @Test
    void wrongIdTypeShouldThrowException() {
        ObjectMapper mapper = new ObjectMapper();
        var service = new DefaultJsonRpcService(List.of(factory.create(new HelloService())));
        var request = new JsonRpcRequest("2.0", "HelloService.sayHello", mapper.createObjectNode().put("name", "World"), mapper.createObjectNode());
        assertThatThrownBy(() -> service.execute(request))
                .isExactlyInstanceOf(JsonRpcInvalidRequestException.class);
    }
}