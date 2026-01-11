package com.busconnect.userservice.exception;

/**
 * Exception thrown when a user is already active in the system.
 */
public class UserAlreadyActiveException extends RuntimeException {
    public UserAlreadyActiveException(String message) {
        super(message);
    }

}
