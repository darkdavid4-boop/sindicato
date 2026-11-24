package es.sindicato.actividades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "participaciones", uniqueConstraints = @UniqueConstraint(columnNames = {"actividad_id", "afiliado_id"}))
@Getter @Setter @NoArgsConstructor
public class Participacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "actividad_id", nullable = false) private Long actividadId;
    @Column(name = "afiliado_id", nullable = false) private Long afiliadoId;
    @Column(nullable = false) private LocalDateTime fechaInscripcion;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoParticipacion estado = EstadoParticipacion.PROCESANDO;
}
