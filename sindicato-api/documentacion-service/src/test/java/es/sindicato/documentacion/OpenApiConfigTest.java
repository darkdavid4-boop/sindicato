package es.sindicato.documentacion;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {
    @Test
    void declaresMetadataAsJsonPartInMultipartUpload() {
        RequestBody requestBody = new RequestBody().content(new Content()
                .addMediaType("multipart/form-data", new MediaType()));
        OpenAPI openApi = new OpenAPI().paths(new Paths().addPathItem(
                "/api/documentos",
                new PathItem().post(new Operation().requestBody(requestBody))));
        GlobalOpenApiCustomizer customizer = new OpenApiConfig().documentoMultipartEncoding();

        customizer.customise(openApi);

        assertEquals("application/json", requestBody.getContent().get("multipart/form-data")
                .getEncoding().get("datos").getContentType());
    }
}
