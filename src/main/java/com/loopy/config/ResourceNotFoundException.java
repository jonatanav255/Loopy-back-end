package com.loopy.config;

/**
 * Thrown when a requested resource is not found or not owned by the current user.
 * Returns a generic "not found" message to avoid info leakage.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
