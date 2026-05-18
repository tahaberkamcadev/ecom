package com.tahaberkamcadev.e_com.user_service.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email address already in use: " + email);
    }
}
