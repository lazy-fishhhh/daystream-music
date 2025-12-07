package com.future.login.exception;


import org.springframework.security.core.AuthenticationException;

public class DeactivatedUserException extends AuthenticationException {
    public DeactivatedUserException(String msg) {
        super(msg);
    }
}
