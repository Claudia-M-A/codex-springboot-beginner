package com.example.codexspringbootbeginner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/hello")
    public HelloResponse hello() {
        return new HelloResponse("Hello, Codex!");
    }

    public record HelloResponse(String message) {}
}
