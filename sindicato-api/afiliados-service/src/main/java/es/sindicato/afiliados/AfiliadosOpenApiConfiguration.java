package es.sindicato.afiliados;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AfiliadosOpenApiConfiguration {
    @Bean
    OpenAPI afiliadosOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de afiliados")
                        .description("Registro y gestión de afiliados del sindicato.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT de sesión emitido por POST /api/afiliados/sesion.")));
    }

    @Bean
    GlobalOpenApiCustomizer afiliadosBearerOpcional() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            agregarBearerOpcional(openApi.getPaths().get("/api/afiliados"), PathItem.HttpMethod.POST);
            agregarBearerOpcional(openApi.getPaths().get("/api/afiliados"), PathItem.HttpMethod.GET);
            agregarBearerOpcional(openApi.getPaths().get("/api/afiliados/{id}"), PathItem.HttpMethod.GET);
            agregarBearerOpcional(openApi.getPaths().get("/api/afiliados/{id}"), PathItem.HttpMethod.PUT);
            agregarBearerOpcional(openApi.getPaths().get("/api/afiliados/{id}"), PathItem.HttpMethod.DELETE);
            agregarBearerOpcional(openApi.getPaths().get("/api/afiliados/{id}/baja"), PathItem.HttpMethod.POST);
        };
    }

    private void agregarBearerOpcional(PathItem path, PathItem.HttpMethod method) {
        if (path == null) {
            return;
        }
        Operation operation = path.readOperationsMap().get(method);
        if (operation != null) {
            operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
            operation.addSecurityItem(new SecurityRequirement());
        }
    }
}
