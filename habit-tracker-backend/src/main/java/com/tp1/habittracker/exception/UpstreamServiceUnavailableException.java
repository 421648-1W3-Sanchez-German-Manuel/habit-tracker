package com.tp1.habittracker.exception;

public class UpstreamServiceUnavailableException extends RuntimeException {

    public UpstreamServiceUnavailableException(String message) {
        super(message);
    }

    public UpstreamServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
