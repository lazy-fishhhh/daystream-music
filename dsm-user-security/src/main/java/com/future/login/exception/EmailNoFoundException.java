package com.future.login.exception;

import org.springframework.security.core.AuthenticationException;

public class EmailNoFoundException extends AuthenticationException {
    public EmailNoFoundException(String msg) {
        super(msg);
    }
}
