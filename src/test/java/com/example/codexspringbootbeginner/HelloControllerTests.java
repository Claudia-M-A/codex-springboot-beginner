package com.example.codexspringbootbeginner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelloControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getHelloReturnsOk() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk());
    }

    @Test
    void getHelloReturnsJsonContentType() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getHelloReturnsExpectedMessageAsJson() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(jsonPath("$.message").value("Hello, Codex!"))
                .andExpect(content().json("{\"message\":\"Hello, Codex!\"}"));
    }

    @Test
    void unknownPathReturnsNotFound() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postToHelloReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/hello"))
                .andExpect(status().isMethodNotAllowed());
    }
}
