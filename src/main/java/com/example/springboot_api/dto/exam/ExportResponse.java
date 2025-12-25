package com.example.springboot_api.dto.exam;

import lombok.Data;

@Data
public class ExportResponse {
    private String filename;
    private String mimeType;
    private byte[] data;
    private long size;
    
    public ExportResponse(String filename, String mimeType, byte[] data) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.data = data;
        this.size = data != null ? data.length : 0;
    }
}