package com.callibrity.ripcurl.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RipCurlController.class)
@ContextConfiguration(classes = {RipCurlAutoConfiguration.class, JacksonAutoConfiguration.class, TestRpcConfiguration.class})
@AutoConfigureMockMvc
class RipCurlControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    void shouldCallHelloSuccessfully() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "hello",
                                    "params": {"name": "John"},
                                    "id": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result").value("Hello, John!"));
    }

    @Test
    void exceptionDuringRpcShouldError() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "evil",
                                    "params": {"input": "bad"},
                                    "id": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32603)) // Internal error code
                .andExpect(jsonPath("$.error.message").value("Internal error"));
    }

    @Test
    void wrongJsonRpcVersionShouldError() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.01",
                                    "method": "hello",
                                    "params": {"name": "John"},
                                    "id": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Invalid Request"));
    }

    @Test
    void idOfWrongTypeShouldError() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "hello",
                                    "params": {"name": "John"},
                                    "id": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Invalid Request"));
    }

    @Test
    void methodNotFoundShouldError() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "nonExistentMethod",
                                    "params": {"name": "John"},
                                    "id": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andExpect(jsonPath("$.error.message").value("Method not found"));
    }

    @Test
    void invalidJsonShouldError() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "hello",
                                    "params": {"name": "John"},
                                    "id": 1
                                """)) // Missing closing brace
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32700))
                .andExpect(jsonPath("$.error.message").value("Parse error"));
    }

    @Test
    void invalidParamsShouldError() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "hello",
                                    "params": 123,
                                    "id": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32602))
                .andExpect(jsonPath("$.error.message").value("Invalid params"));
    }

    @Test
    void notificationShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "notify",
                                    "params": {"message": "This is a notification"}
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void internalErrorDuringNotificationShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "evil",
                                    "params": {"input": "bad"}
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void notificationsCannotSendExplicitNullForId() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "evil",
                                    "params": {"input": "bad"},
                                    "id": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error.code").value(-32600))
                .andExpect(jsonPath("$.error.message").value("Invalid Request"));
    }
}