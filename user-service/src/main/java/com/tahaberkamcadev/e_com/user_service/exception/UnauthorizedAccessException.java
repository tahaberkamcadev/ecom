package com.tahaberkamcadev.e_com.user_service.exception;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException() {
        super("You do not have permission to perform this action");
    }

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
