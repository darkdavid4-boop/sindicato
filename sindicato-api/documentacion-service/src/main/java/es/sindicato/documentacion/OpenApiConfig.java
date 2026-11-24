package es.sindicato.documentacion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI documentacionOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestor de documentación")
                        .description("API para publicar, previsualizar y descargar documentos públicos.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT emitido por POST /api/afiliados/sesion; los roles dependen de APP_ADMIN_EMAILS.")));
    }

    @Bean
    GlobalOpenApiCustomizer documentoMultipartEncoding() {
        return openApi -> {
            PathItem path = openApi.getPaths().get("/api/documentos");
            Operation operation = path == null ? null : path.getPost();
            if (operation == null || operation.getRequestBody() == null
                    || operation.getRequestBody().getContent() == null
                    || operation.getRequestBody().getContent().get("multipart/form-data") == null) {
                throw new IllegalStateException("No se encontró el contrato multipart de carga de documentos");
            }
            operation.getRequestBody().getContent().get("multipart/form-data")
                    .addEncoding("datos", new Encoding().contentType("application/json"));
        };
    }
}
