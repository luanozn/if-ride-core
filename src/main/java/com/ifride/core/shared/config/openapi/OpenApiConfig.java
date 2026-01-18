package com.ifride.core.shared.config.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.springframework.http.HttpStatus.*;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "IF Ride API",
                version = "v1",
                description = "Documentação da API do sistema de caronas acadêmicas",
                contact = @Contact(name = "Luan Ribeiro", email = "luan.ribeiro@estudante.ifgoiano.edu.br")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

        private final List<HttpStatus> errorStatuses = List.of(BAD_REQUEST, NOT_FOUND, UNAUTHORIZED, FORBIDDEN, CONFLICT, PRECONDITION_FAILED, INTERNAL_SERVER_ERROR);

        @Bean
        public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer() {
                return openApi -> {
                        if (openApi.getComponents() == null) {
                                openApi.setComponents(new Components());
                        }

                        var schema = SchemaConfiguration.createApiExceptionSchema();
                        openApi.getComponents().addSchemas("ApiException", schema);

                        openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                                if (operation.getParameters() != null) {
                                        operation.getParameters().forEach(parameter -> {
                                                switch (parameter.getName()) {
                                                        case "page" -> parameter.description("Índice da página (inicia em 0)");
                                                        case "size" -> parameter.description("Quantidade de itens por página");
                                                        case "sort" -> parameter.description("Critério de ordenação no formato: propriedade,(asc|desc)");
                                                }
                                        });
                                }

                                var responses = operation.getResponses();

                                for (HttpStatus httpStatus : errorStatuses) {
                                        String code = String.valueOf(httpStatus.value());

                                        if (responses.containsKey(code)) {
                                                var content = new Content().addMediaType("application/json",
                                                        new MediaType()
                                                                .schema(new Schema<>().$ref("ApiException"))
                                                                .example(SchemaConfiguration.getExampleValueForStatus(httpStatus)));
                                                responses.get(code).setContent(content);
                                        }
                                }
                        }));
                };
        }
}