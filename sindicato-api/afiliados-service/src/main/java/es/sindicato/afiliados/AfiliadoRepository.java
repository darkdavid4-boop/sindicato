package es.sindicato.afiliados;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AfiliadoRepository extends JpaRepository<Afiliado, Long> {
    Optional<Afiliado> findByTokenConfirmacion(String tokenConfirmacion);
    Optional<Afiliado> findByEmailIgnoreCase(String email);
}
