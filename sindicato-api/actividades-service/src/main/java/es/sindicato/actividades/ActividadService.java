package es.sindicato.actividades;

import es.sindicato.common.ApiException;
import es.sindicato.common.ImageStorage;
import es.sindicato.events.ActividadCreadaEvent;
import es.sindicato.events.ParticipacionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ActividadService {
    private final ActividadRepository actividades;
    private final ParticipacionRepository participaciones;
    private final ActividadEventPublisher eventPublisher;
    private final ImageStorage imageStorage;

    public ActividadService(ActividadRepository actividades, ParticipacionRepository participaciones,
                            ActividadEventPublisher eventPublisher, ImageStorage imageStorage) {
        this.actividades = actividades;
        this.participaciones = participaciones;
        this.eventPublisher = eventPublisher;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public Actividad crear(ActividadRequest request) {
        return crear(request, null);
    }

    @Transactional
    public Actividad crear(ActividadRequest request, MultipartFile imagen) {
        Actividad actividad = new Actividad();
        actualizarCampos(actividad, request);
        if (imagen != null) actividad.setImagenUrl(imageStorage.store(imagen, "actividades"));
        actividad = actividades.save(actividad);
        eventPublisher.publicarActividad(new ActividadCreadaEvent(
                actividad.getId(), actividad.getTitulo(), actividad.getContenido(), actividad.getFechaPublicacion(),
                actividad.getImagenUrl()));
        return actividad;
    }

    @Transactional(readOnly = true)
    public Actividad obtener(Long id) {
        return actividades.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Actividad no encontrada"));
    }

    @Transactional(readOnly = true)
    public Page<Actividad> listar(int pagina) {
        if (actividades.count() == 0) throw new ApiException(HttpStatus.NOT_FOUND, "No hay actividades registradas");
        return actividades.findAll(PageRequest.of(Math.max(0, pagina), 10));
    }

    @Transactional
    public Actividad actualizar(Long id, ActividadRequest request) {
        return actualizar(id, request, null);
    }

    @Transactional
    public Actividad actualizar(Long id, ActividadRequest request, MultipartFile imagen) {
        Actividad actividad = actividades.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Actividad no encontrada"));
        actualizarCampos(actividad, request);
        if (imagen != null && !imagen.isEmpty()) actividad.setImagenUrl(imageStorage.store(imagen, "actividades"));
        long aceptados = participaciones.countByActividadIdAndEstado(id, EstadoParticipacion.ACEPTADO);
        if (request.capacidad() < aceptados) throw new ApiException(HttpStatus.CONFLICT, "La capacidad no puede ser menor a las participaciones aceptadas");
        actividad.setEstado(aceptados >= actividad.getCapacidad() ? EstadoActividad.LLENO : EstadoActividad.DISPONIBLE);
        return actividades.save(actividad);
    }

    @Transactional
    public void eliminar(Long id) {
        Actividad actividad = actividades.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Actividad no encontrada"));
        participaciones.deleteAll(participaciones.findByActividadId(id));
        actividades.delete(actividad);
    }

    @Transactional
    public Participacion inscribir(Long actividadId, Long afiliadoId) {
        Actividad actividad = actividades.findByIdForUpdate(actividadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Actividad no encontrada"));
        if (actividad.getEstado() == EstadoActividad.LLENO) {
            throw new ApiException(HttpStatus.CONFLICT, "La actividad está llena");
        }
        Participacion participacion = new Participacion();
        participacion.setActividadId(actividadId);
        participacion.setAfiliadoId(afiliadoId);
        participacion.setFechaInscripcion(java.time.LocalDateTime.now());
        participaciones.save(participacion);
        participacion.setEstado(EstadoParticipacion.ACEPTADO);
        participacion = participaciones.save(participacion);
        long aceptados = participaciones.countByActividadIdAndEstado(actividadId, EstadoParticipacion.ACEPTADO);
        if (aceptados >= actividad.getCapacidad()) {
            actividad.setEstado(EstadoActividad.LLENO);
            actividades.save(actividad);
            List<Participacion> procesando = participaciones.findByActividadIdAndEstado(actividadId, EstadoParticipacion.PROCESANDO);
            procesando.forEach(p -> p.setEstado(EstadoParticipacion.CANCELADO));
            participaciones.saveAll(procesando);
        }
        eventPublisher.publicarParticipacion(toEvent(participacion));
        return participacion;
    }

    @Transactional(readOnly = true)
    public List<Participacion> listarParticipaciones(Long actividadId) {
        obtener(actividadId);
        return participaciones.findByActividadId(actividadId);
    }

    @Transactional
    public Participacion cancelar(Long actividadId, Long participacionId) {
        Participacion participacion = participaciones.findById(participacionId)
                .filter(p -> p.getActividadId().equals(actividadId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Participación no encontrada"));
        participacion.setEstado(EstadoParticipacion.CANCELADO);
        Actividad actividad = actividades.findByIdForUpdate(actividadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Actividad no encontrada"));
        if (participaciones.countByActividadIdAndEstado(actividadId, EstadoParticipacion.ACEPTADO) < actividad.getCapacidad()) {
            actividad.setEstado(EstadoActividad.DISPONIBLE);
            actividades.save(actividad);
        }
        participacion = participaciones.save(participacion);
        eventPublisher.publicarParticipacion(toEvent(participacion));
        return participacion;
    }

    private ParticipacionEvent toEvent(Participacion p) {
        return new ParticipacionEvent(p.getId(), p.getActividadId(), p.getAfiliadoId(),
                p.getFechaInscripcion(), p.getEstado().name());
    }

    private void actualizarCampos(Actividad actividad, ActividadRequest request) {
        actividad.setTitulo(request.titulo());
        actividad.setContenido(request.contenido());
        actividad.setFechaPublicacion(request.fechaPublicacion());
        actividad.setCapacidad(request.capacidad());
    }
}
