package com.callibrity.ripcurl.autoconfigure;

import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodHandler;
import com.callibrity.ripcurl.core.annotation.AnnotationJsonRpcMethodProvider;
import com.callibrity.ripcurl.core.annotation.JsonRpc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRpcConfiguration {

    public static class HelloRpc {
        @JsonRpc("hello")
        public String hello(String name) {
            return String.format("Hello, %s!", name);
        }
    }


    public static class EvilRpc {
        @JsonRpc("evil")
        public String evil(String input) {
            throw new IllegalArgumentException(String.format("Invalid input: %s", input));
        }
    }

    public static class NotificationRpc {
        @JsonRpc("notify")
        public void notify(String message) {
            // This method does not return a value, simulating a notification
        }
    }

    @Bean
    public AnnotationJsonRpcMethodProvider helloHandler(ObjectMapper objectMapper) {
        return new AnnotationJsonRpcMethodProvider(objectMapper, new HelloRpc());
    }

    @Bean
    public AnnotationJsonRpcMethodProvider evilHandler(ObjectMapper objectMapper) {
        return new AnnotationJsonRpcMethodProvider(objectMapper, new EvilRpc());
    }

    @Bean
    public AnnotationJsonRpcMethodProvider notificationHandler(ObjectMapper objectMapper) {
        return new AnnotationJsonRpcMethodProvider(objectMapper, new NotificationRpc());
    }
}
