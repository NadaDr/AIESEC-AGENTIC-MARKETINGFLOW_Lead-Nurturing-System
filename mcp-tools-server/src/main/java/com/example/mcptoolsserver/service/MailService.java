
package com.example.mcptoolsserver.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.SimpleMailMessage;

import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,

            @Value("${app.mail.from}") String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public Map<String, Object> sendEmail(String to, String subject, String body) {

        Map<String, Object> result = new LinkedHashMap<>();

        if (to == null || to.isBlank() || !to.contains("@")) {
            result.put("sent", false);
            result.put("error", "Recipient email address is missing or invalid.");

            return result;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);

        message.setSubject(subject == null || subject.isBlank() ? "(no subject)" : subject);

        message.setText(body == null ? "" : body);

        try {

            mailSender.send(message);
            result.put("sent", true);
            result.put("to", to);
            result.put("subject", subject);
        } catch (Exception exception) {

            result.put("sent", false);
            result.put("error", "Email could not be sent: " + exception.getMessage());
        }

        return result;
    }
}
