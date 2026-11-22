package com.ifride.core.shared.services.impl;

import com.ifride.core.shared.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Log4j2
@RequiredArgsConstructor
@Profile("!it")
public class AwsEmailService implements EmailService {

    private final SesClient emailClient;

    @Value("${aws.ses.email}")
    private String from;
    @Value("${frontend.url}")
    private String cloudfrontUrl;

    @Override
    public void sendEmailVerificationEmail(String to, String token) {
        String verificationUrl = cloudfrontUrl + "/v1/auth/verify-email?token=" + token;
        String htmlBody = "<h1>Verifique seu e-mail</h1><p>Clique <a href=\"" + verificationUrl + "\">aqui</a> para validar sua conta no IF Ride.</p>";

        sendHtmlEmail(to, "Verificação de Conta - IF Ride", htmlBody);
    }

    @Override
    public void sendPasswordChangeEmail(String to, String token) {
        String resetUrl = cloudfrontUrl + "/v1/auth/reset-password?token=" + token;

        String htmlBody = """
                <div style="font-family: sans-serif; text-align: center; padding: 20px;">
                    <h1 style="color: #333;">Redefinição de Senha</h1>
                    <p style="color: #555; font-size: 16px;">Recebemos uma solicitação para redefinir a senha da sua conta no IF Ride.</p>
                    <p style="margin: 30px 0;">
                        <a href="%s" style="background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;">Criar Nova Senha</a>
                    </p>
                    <p style="color: #888; font-size: 12px;">Este link expira em 1 hora. Se você não solicitou a alteração, ignore este e-mail.</p>
                </div>
                """.formatted(resetUrl);

        sendHtmlEmail(to, "Redefinição de Senha - IF Ride", htmlBody);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder().html(Content.builder().data(htmlContent).build()).build())
                            .build())
                    .source(from)
                    .build();

            emailClient.sendEmail(request);
            log.info("E-mail de verificação enviado com sucesso para: {}", to);
        } catch (SesException e) {
            log.error("Falha ao enviar e-mail via SES: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Erro ao processar envio de e-mail acadêmico.");
        }
    }
}
