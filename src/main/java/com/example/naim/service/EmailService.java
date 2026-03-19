package com.example.naim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.from}")
    private String fromEmail;

    public void sendVerificationEmail(String to, String token) {
        String subject = "Verify your email - IntelliStudy";
        String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;
        
        log.info("Sending verification email to {}. Link: {}", to, verificationUrl);
        
        String message = "Welcome to IntelliStudy!\n\n" +
                "Please click the link below to verify your email address:\n" +
                verificationUrl + "\n\n" +
                "If you did not sign up for IntelliStudy, please ignore this email.";

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(to);
        email.setSubject(subject);
        email.setText(message);
        email.setFrom(fromEmail);

        mailSender.send(email);
    }
}
