package es.sindicato.actividades;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipacionRepository extends JpaRepository<Participacion, Long> {
    List<Participacion> findByActividadId(Long actividadId);
    List<Participacion> findByActividadIdAndEstado(Long actividadId, EstadoParticipacion estado);
    long countByActividadIdAndEstado(Long actividadId, EstadoParticipacion estado);
}
