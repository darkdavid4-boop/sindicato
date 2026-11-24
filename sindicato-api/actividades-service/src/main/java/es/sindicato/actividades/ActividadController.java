package es.sindicato.actividades;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/actividades")
public class ActividadController {
    private final ActividadService service;
    public ActividadController(ActividadService service) { this.service = service; }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Actividad crear(@Valid @RequestBody ActividadRequest request) { return service.crear(request); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Actividad crearConImagen(@Valid @RequestPart("datos") ActividadRequest request,
                                    @RequestPart("imagen") MultipartFile imagen) {
        return service.crear(request, imagen);
    }
    @GetMapping("/{id}") public Actividad obtener(@PathVariable Long id) { return service.obtener(id); }
    @GetMapping public Page<Actividad> listar(@RequestParam(defaultValue = "0") int pagina) { return service.listar(pagina); }
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Actividad actualizar(@PathVariable Long id, @Valid @RequestBody ActividadRequest request) { return service.actualizar(id, request); }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Actividad actualizarConImagen(@PathVariable Long id, @Valid @RequestPart("datos") ActividadRequest request,
                                         @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return service.actualizar(id, request, imagen);
    }
    @DeleteMapping("/{id}") public void eliminar(@PathVariable Long id) { service.eliminar(id); }
    @PostMapping("/{id}/participaciones")
    public Participacion inscribir(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.inscribir(id, Long.valueOf(jwt.getSubject()));
    }
    @GetMapping("/{id}/participaciones") public List<Participacion> participaciones(@PathVariable Long id) { return service.listarParticipaciones(id); }
    @DeleteMapping("/{id}/participaciones/{participacionId}") public Participacion cancelar(@PathVariable Long id, @PathVariable Long participacionId) {
        return service.cancelar(id, participacionId);
    }
}
