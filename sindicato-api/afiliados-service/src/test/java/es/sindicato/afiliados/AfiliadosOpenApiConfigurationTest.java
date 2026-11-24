package es.sindicato.afiliados;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfiliadosOpenApiConfigurationTest {
    @Test
    void declaresOptionalBearerOnCrudOperationsOnly() {
        Operation getAll = new Operation();
        Operation login = new Operation();
        Paths paths = new Paths()
                .addPathItem("/api/afiliados", new PathItem().get(getAll))
                .addPathItem("/api/afiliados/sesion", new PathItem().post(login));
        OpenAPI openApi = new OpenAPI().paths(paths);
        GlobalOpenApiCustomizer customizer = new AfiliadosOpenApiConfiguration().afiliadosBearerOpcional();

        customizer.customise(openApi);

        assertEquals(2, getAll.getSecurity().size());
        assertTrue(getAll.getSecurity().get(0).containsKey("bearerAuth"));
        assertTrue(getAll.getSecurity().get(1).isEmpty());
        assertTrue(login.getSecurity() == null);
    }
}
