package com.ifride.core.shared.services.impl;

import com.ifride.core.shared.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Log4j2
@RequiredArgsConstructor
public class AwsEmailService implements EmailService {

    private final SesClient emailClient;

    @Value("${aws.ses.email}")
    private String from;

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
