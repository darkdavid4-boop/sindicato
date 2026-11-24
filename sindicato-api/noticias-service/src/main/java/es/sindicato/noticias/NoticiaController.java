package es.sindicato.noticias;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/noticias")
public class NoticiaController {
    private final NoticiaService service;
    public NoticiaController(NoticiaService service) { this.service = service; }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Noticia crear(@Valid @RequestBody NoticiaRequest request) { return service.crear(request); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Noticia crearConImagen(@Valid @RequestPart("datos") NoticiaRequest request,
                                  @RequestPart("imagen") MultipartFile imagen) {
        return service.crear(request, imagen);
    }
    @GetMapping("/{id}") public Noticia obtener(@PathVariable Long id) { return service.obtener(id); }
    @GetMapping public Page<Noticia> listar(@RequestParam(defaultValue = "0") int pagina) { return service.listar(pagina); }
    @GetMapping("/actividades") public List<Noticia> listarDeActividades() { return service.listarDeActividades(); }
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Noticia actualizar(@PathVariable Long id, @Valid @RequestBody NoticiaRequest request) { return service.actualizar(id, request); }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Noticia actualizarConImagen(@PathVariable Long id, @Valid @RequestPart("datos") NoticiaRequest request,
                                       @RequestPart(value = "imagen", required = false) MultipartFile imagen) {
        return service.actualizar(id, request, imagen);
    }
    @DeleteMapping("/{id}") public void eliminar(@PathVariable Long id) { service.eliminar(id); }
}
