package es.sindicato.noticias;

import es.sindicato.common.ApiException;
import es.sindicato.common.ImageStorage;
import es.sindicato.events.ActividadCreadaEvent;
import es.sindicato.events.NoticiaPublicadaEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class NoticiaService {
    private final NoticiaRepository repository;
    private final KafkaTemplate<String, Object> kafka;
    private final ImageStorage imageStorage;

    public NoticiaService(NoticiaRepository repository, KafkaTemplate<String, Object> kafka, ImageStorage imageStorage) {
        this.repository = repository;
        this.kafka = kafka;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public Noticia crear(NoticiaRequest request) {
        return crear(request, null);
    }

    @Transactional
    public Noticia crear(NoticiaRequest request, MultipartFile imagen) {
        Noticia noticia = new Noticia();
        noticia.setTitulo(request.titulo());
        noticia.setContenido(request.contenido());
        noticia.setFechaPublicacion(request.fechaPublicacion());
        noticia.setActividadId(request.actividadId());
        if (imagen != null) noticia.setImagenUrl(imageStorage.store(imagen, "noticias"));
        return guardarYNotificar(noticia);
    }

    @Transactional
    public Noticia crearDesdeActividad(ActividadCreadaEvent event) {
        Noticia noticia = new Noticia();
        noticia.setTitulo(event.titulo());
        noticia.setContenido(event.contenido());
        noticia.setFechaPublicacion(event.fechaPublicacion());
        noticia.setActividadId(event.actividadId());
        noticia.setImagenUrl(event.imagenUrl());
        return guardarYNotificar(noticia);
    }

    private Noticia guardarYNotificar(Noticia noticia) {
        noticia = repository.save(noticia);
        try {
            kafka.send("noticias.publicadas", noticia.getId().toString(),
                    new NoticiaPublicadaEvent(noticia.getId(), noticia.getTitulo(), noticia.getContenido(), noticia.getFechaPublicacion()))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se puede conectar con Kafka");
        }
        return noticia;
    }

    @Transactional(readOnly = true)
    public Noticia obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Noticia no encontrada"));
    }

    @Transactional(readOnly = true)
    public Page<Noticia> listar(int pagina) {
        if (repository.count() == 0) throw new ApiException(HttpStatus.NOT_FOUND, "No hay noticias publicadas");
        return repository.findAll(PageRequest.of(Math.max(0, pagina), 10));
    }

    @Transactional(readOnly = true)
    public List<Noticia> listarDeActividades() {
        List<Noticia> noticias = repository.findByActividadIdIsNotNull();
        if (noticias.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "No hay noticias asociadas a actividades");
        return noticias;
    }

    @Transactional
    public Noticia actualizar(Long id, NoticiaRequest request) {
        return actualizar(id, request, null);
    }

    @Transactional
    public Noticia actualizar(Long id, NoticiaRequest request, MultipartFile imagen) {
        Noticia noticia = obtener(id);
        noticia.setTitulo(request.titulo());
        noticia.setContenido(request.contenido());
        noticia.setFechaPublicacion(request.fechaPublicacion());
        noticia.setActividadId(request.actividadId());
        if (imagen != null && !imagen.isEmpty()) noticia.setImagenUrl(imageStorage.store(imagen, "noticias"));
        return repository.save(noticia);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtener(id));
    }
}
