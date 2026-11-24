package es.sindicato.afiliados;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/afiliados")
@Tag(name = "Afiliados", description = "Registro, consulta y gestión de afiliados")
public class AfiliadoController {
    private final AfiliadoService service;
    private final AuthenticationService authentication;

    public AfiliadoController(AfiliadoService service, AuthenticationService authentication) {
        this.service = service;
        this.authentication = authentication;
    }

    @PostMapping("/sesion")
    public SessionResponse iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return authentication.iniciarSesion(request);
    }

    @Operation(summary = "Crear afiliado", description = "Registra un afiliado. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Afiliado crear(@Valid @RequestBody AfiliadoRequest request) { return service.crear(request); }

    @Operation(summary = "Crear afiliado con imagen", description = "Registra un afiliado junto con su imagen de perfil. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Afiliado crearConImagen(@Valid @RequestPart("datos") AfiliadoRequest request,
                                   @RequestPart("imagen") MultipartFile imagen) {
        return service.crear(request, imagen);
    }

    @Operation(summary = "Consultar afiliado", description = "Consulta un afiliado por identificador. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @GetMapping("/{id}") public Afiliado obtener(@PathVariable Long id) { return service.obtener(id); }

    @Operation(summary = "Listar afiliados", description = "Devuelve una página de afiliados. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @GetMapping public Page<Afiliado> listar(@RequestParam(defaultValue = "0") int pagina) { return service.listar(pagina); }

    @Operation(summary = "Actualizar afiliado", description = "Actualiza los datos del afiliado. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Afiliado actualizar(@PathVariable Long id, @Valid @RequestBody AfiliadoRequest request) { return service.actualizar(id, request); }

    @Operation(summary = "Actualizar afiliado con imagen", description = "Actualiza los datos y opcionalmente la imagen de perfil. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Afiliado actualizarConImagen(@PathVariable Long id, @Valid @RequestPart("datos") AfiliadoRequest request,
                                        @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return service.actualizar(id, request, imagen);
    }

    @Operation(summary = "Eliminar afiliado", description = "Elimina el afiliado. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @DeleteMapping("/{id}") public void eliminar(@PathVariable Long id) { service.eliminar(id); }

    @Operation(summary = "Solicitar baja", description = "Envía el correo para confirmar la baja. El Bearer es opcional en Swagger y no se valida actualmente en este servicio.")
    @PostMapping("/{id}/baja") public Map<String, String> solicitarBaja(@PathVariable Long id) {
        service.solicitarBaja(id);
        return Map.of("mensaje", "Se ha enviado un enlace para confirmar la baja");
    }
    @GetMapping("/confirmar") public Afiliado confirmar(@RequestParam String token) { return service.confirmar(token); }
}
