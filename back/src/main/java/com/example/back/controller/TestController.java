package com.example.back.controller;

import com.example.back.dto.ErrorResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("dev")
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/runtime-exception")
    public ResponseEntity<ErrorResponse> testRuntimeException() {
        throw new RuntimeException("Ceci est un test de RuntimeException");
    }

    @GetMapping("/illegal-argument")
    public ResponseEntity<ErrorResponse> testIllegalArgumentException() {
        throw new IllegalArgumentException("Ceci est un test de IllegalArgumentException");
    }

    @GetMapping("/generic-exception")
    public ResponseEntity<ErrorResponse> testGenericException() throws Exception {
        throw new Exception("Ceci est un test de Exception générique");
    }
}
