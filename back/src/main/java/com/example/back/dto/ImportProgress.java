package com.example.back.dto;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class ImportProgress {
    private volatile boolean running = false;
    private volatile int imported = 0;
    private volatile int skipped = 0;
    private volatile int total = 0;
    private volatile int currentPage = 0;
    private volatile boolean done = false;
    private volatile String error = null;
}