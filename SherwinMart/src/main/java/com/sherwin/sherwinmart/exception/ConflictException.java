package com.sherwin.sherwinmart.exception;

/** Checked exception for state conflicts, e.g. duplicate email or insufficient stock (HTTP 409). */
public class ConflictException extends Exception {

    public ConflictException(String message) {
        super(message);
    }
}
