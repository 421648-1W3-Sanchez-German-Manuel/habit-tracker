package com.tp1.habittracker.exception;

public class UpstreamBadResponseException extends RuntimeException {

    public UpstreamBadResponseException(String message) {
        super(message);
    }

    public UpstreamBadResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
