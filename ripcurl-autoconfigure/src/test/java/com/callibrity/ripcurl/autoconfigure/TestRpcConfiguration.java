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
package com.callibrity.ripcurl.autoconfigure;

import com.callibrity.ripcurl.core.annotation.JsonRpc;
import com.callibrity.ripcurl.core.annotation.JsonRpcService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestRpcConfiguration {

    @JsonRpcService
    public static class HelloRpc {
        @JsonRpc("hello")
        public String hello(String name) {
            return String.format("Hello, %s!", name);
        }
    }

    @JsonRpcService
    public static class EvilRpc {
        @JsonRpc("evil")
        public String evil(String input) {
            throw new IllegalArgumentException(String.format("Invalid input: %s", input));
        }
    }

    @JsonRpcService
    public static class NotificationRpc {
        @JsonRpc("notify")
        public void notify(String message) {
            // This method does not return a value, simulating a notification
        }
    }

    @Bean
    public HelloRpc helloRpc() {
        return new HelloRpc();
    }

    @Bean
    public EvilRpc evilRpc() {
        return new EvilRpc();
    }

    @Bean
    public NotificationRpc notificationRpc() {
        return new NotificationRpc();
    }
}
