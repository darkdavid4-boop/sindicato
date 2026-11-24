package es.sindicato.notificaciones;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AfiliadoEmailRepository extends JpaRepository<AfiliadoEmail, Long> {
    Optional<AfiliadoEmail> findByAfiliadoId(Long afiliadoId);
    List<AfiliadoEmail> findByActivoTrue();
}
