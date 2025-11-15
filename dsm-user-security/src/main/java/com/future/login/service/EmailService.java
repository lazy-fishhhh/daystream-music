package com.future.login.service;

import entity.pojo.util.Result;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface EmailService {
    Result sendCaptcha(String toEmail) throws MessagingException, IOException;
}
