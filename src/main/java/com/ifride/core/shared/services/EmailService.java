package com.ifride.core.shared.services;

public interface EmailService {
    void sendHtmlEmail(String to, String subject, String htmlContent);
}