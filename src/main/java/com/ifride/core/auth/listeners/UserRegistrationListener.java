package com.ifride.core.auth.listeners;

import com.ifride.core.auth.service.EmailVerificationTokenService;
import com.ifride.core.events.models.UserRegisteredEvent;
import com.ifride.core.shared.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final EmailVerificationTokenService tokenService;
    private final EmailService emailService;

    @Value("${api.gateway.url}")
    private String apiGatewayUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        var user = event.user();
        var actionToken = tokenService.generateToken(user);

        String verificationUrl = apiGatewayUrl + "/v1/auth/verify-email?token=" + actionToken.getToken();
        String htmlBody = "<h1>Verifique seu e-mail</h1><p>Clique <a href=\"" + verificationUrl + "\">aqui</a> para validar sua conta no IF Ride.</p>";

        emailService.sendHtmlEmail(user.getEmail(), "Verificação de Conta - IF Ride", htmlBody);
    }
}