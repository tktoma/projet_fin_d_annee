package com.example.back.controller;

import com.example.back.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
