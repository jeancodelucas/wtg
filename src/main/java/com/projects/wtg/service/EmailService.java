// src/main/java/com/projects/wtg/service/EmailService.java

package com.projects.wtg.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String to, String link) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Redefinição de Senha - WTG App");
        message.setText("Olá,\n\nPara redefinir sua senha, clique no link abaixo:\n" + link);
        mailSender.send(message);
    }

    public void sendRegistrationTokenEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Seu código de verificação - WTG App");
        message.setText("Olá,\n\nSeu código de verificação é: " + token);
        mailSender.send(message);
    }
}