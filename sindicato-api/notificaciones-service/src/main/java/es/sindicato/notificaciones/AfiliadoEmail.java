package es.sindicato.notificaciones;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "destinatarios", uniqueConstraints = @UniqueConstraint(columnNames = "afiliado_id"))
@Getter @Setter @NoArgsConstructor
public class AfiliadoEmail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "afiliado_id", nullable = false) private Long afiliadoId;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private boolean activo;
}
