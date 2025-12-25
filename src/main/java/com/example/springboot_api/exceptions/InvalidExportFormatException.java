package com.example.springboot_api.exceptions;

public class InvalidExportFormatException extends RuntimeException {
    public InvalidExportFormatException(String message) {
        super(message);
    }
}