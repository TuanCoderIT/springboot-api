package com.example.springboot_api.exceptions;

public class ExportGenerationException extends RuntimeException {
    public ExportGenerationException(String message) {
        super(message);
    }
    
    public ExportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}