package com.energy_tracker.alert_service.service;

import com.energy_tracker.alert_service.entity.Alert;
import com.energy_tracker.alert_service.repository.AlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EmailService {

    private JavaMailSender mailSender;
    private AlertRepository alertRepository;

    public EmailService(JavaMailSender javaMailSender, AlertRepository alertRepository) {
        this.mailSender = javaMailSender;
        this.alertRepository = alertRepository;
    }

    public void sendEmail(String to, String subject, String body, Long userId) {
        // Implement email sending logic here using javaMailSender
        log.info("Sending email to: {}, subject: {}, body: {}, userId: {}", to, subject, body, userId);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("noreplt@hetpatel.com");
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);

            final Alert alertSent = Alert.builder()
                    .sent(true)
                    .createdAt(LocalDateTime.now())
                    .userId(userId)
                    .build();
            alertRepository.saveAndFlush(alertSent);
        }catch (Exception e) {
            log.error("Error sending email to: {}, error: {}", to,  e.getMessage());
            final Alert alertNotSent = Alert.builder()
                    .sent(false)
                    .createdAt(LocalDateTime.now())
                    .userId(userId)
                    .build();
            alertRepository.saveAndFlush(alertNotSent);
            return;
        }

        log.info("Email sent successfully to: {}, subject: {}, body: {}, userId: {}", to, subject, body, userId);
    }
}
