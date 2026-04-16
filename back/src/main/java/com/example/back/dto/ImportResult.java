package com.example.back.dto;

import lombok.Data;

@Data
public class ImportResult {
    private int importes;
    private int total;
    
    public ImportResult(int importes, int total) {
        this.importes = importes;
        this.total = total;
    }
}
