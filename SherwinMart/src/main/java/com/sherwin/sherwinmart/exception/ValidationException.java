package com.sherwin.sherwinmart.exception;

/** Checked exception thrown when service-layer input validation fails (maps to HTTP 400). */
public class ValidationException extends Exception {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
