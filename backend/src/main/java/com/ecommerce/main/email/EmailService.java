package com.ecommerce.main.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String code) {
        log.info("Sending verification email to: {}", toEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Email Verification - Ecom");
        message.setText(
            "Merhaba,\n\n" +
            "Dogrulama kodunuz: " + code + "\n\n" +
            "Bu kod 15 dakika gecerlidir.\n\n" +
            "Ecom"
        );

        try {
            mailSender.send(message);
            log.info("Successfully sent verification email to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send verification email to: {}. Error: {}", toEmail, e.getMessage());
        }
    }

    public void sendRegistrationAttemptEmail(String toEmail) {
        log.info("Sending registration attempt email to: {}", toEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Kayit Girişimi - Ecom");
        message.setText(
            "Merhaba,\n\n" +
            "Bu email adresiyle yeni bir hesap acilmaya calisildi. " +
            "Eger bu siz degilseniz, hesabiniz guvendedir ve herhangi bir islem yapmaniza gerek yoktur.\n\n" +
            "Sifrenizi degistirmek isterseniz sifre sifirlama sayfasini kullanabilirsiniz.\n\n" +
            "Ecom"
        );
        try {
            mailSender.send(message);
            log.info("Successfully sent registration attempt email to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send registration attempt email to: {}. Error: {}", toEmail, e.getMessage());
        }
    }

    public void sendPasswordResetEmail(String toEmail, String code) {
        log.info("Sending password reset email to: {}", toEmail);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset - Ecom");
        message.setText(
            "Merhaba,\n\n" +
            "Sifre sifirlama kodunuz: " + code + "\n\n" +
            "Bu kod 15 dakika gecerlidir.\n\n" +
            "Ecom Team"
        );

        try {
            mailSender.send(message);
            log.info("Successfully sent password reset email to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send password reset email to: {}. Error: {}", toEmail, e.getMessage());
        }
    }
}
