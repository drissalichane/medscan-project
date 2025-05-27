package com.example.medscan.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Medication not found")
public class ResourceNotFoundException extends RuntimeException {

    // Constructor to pass the error message to the exception
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
