package com.ifride.core.shared.config;

import com.ifride.core.shared.services.EmailService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("it")
public class LocalEmailService implements EmailService {

    public record EmailCall(String to, String subject, String body) {}

    private final List<EmailCall> calls = new CopyOnWriteArrayList<>();

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        calls.add(new EmailCall(to, subject, htmlContent));
    }

    @Override
    public void sendEmailVerificationEmail(String to, String token) {
        calls.add(new EmailCall(to, "Verificação de Email", token));
    }

    public List<EmailCall> getCalls() {
        return List.copyOf(calls);
    }

    public void clear() {
        calls.clear();
    }

    public Optional<EmailCall> lastCallTo(String email) {
        return calls.stream()
                .filter(c -> c.to().equals(email))
                .reduce((first, second) -> second);
    }
}