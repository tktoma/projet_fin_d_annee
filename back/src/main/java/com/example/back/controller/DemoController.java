package com.example.back.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DemoController {
    
    @GetMapping("/demo")
    public String demo() {
        return "demo";
    }
    
    @GetMapping("/")
    public String home() {
        return "demo";
    }
}
