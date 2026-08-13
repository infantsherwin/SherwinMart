package com.sherwin.sherwinmart.exception;

/** Checked exception for authentication/authorization failures (maps to HTTP 401/403). */
public class AuthException extends Exception {

    private final int statusCode;

    public AuthException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
