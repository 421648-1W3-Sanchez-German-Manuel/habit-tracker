package com.tp1.habittracker.exception;

import java.util.Map;

public class DuplicateResourceException extends RuntimeException {

    private final Map<String, Object> details;

    public DuplicateResourceException(String message) {
        this(message, null);
    }

    public DuplicateResourceException(String message, Map<String, Object> details) {
        super(message);
        this.details = details;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
