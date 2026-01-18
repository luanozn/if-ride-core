package com.ifride.core.shared.config.openapi;

import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class SchemaConfiguration {

    public static ObjectSchema createApiExceptionSchema() {
        var schema = new ObjectSchema();

        schema.name("ApiException");
        schema.description("Estrutura padrão de resposta para erros da API");
        schema.addProperty("status", new IntegerSchema());
        schema.addProperty("message", new StringSchema());
        schema.addProperty("details", new StringSchema());

        return schema;
    }

    public static Object getExampleValueForStatus(HttpStatus status) {
        return Map.of(
                "status", status.value(),
                "message", "Mensagem de erro contendo detalhes do problema",
                "details", "Detalhes do problema (Se existirem)."
        );
    }
}
