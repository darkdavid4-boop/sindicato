package es.sindicato.actividades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "actividades")
@Getter @Setter @NoArgsConstructor
public class Actividad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT") private String contenido;
    @Column(nullable = false) private LocalDateTime fechaPublicacion;
    @Column(nullable = false) private int capacidad;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoActividad estado = EstadoActividad.DISPONIBLE;
    @Column(length = 512) private String imagenUrl;
}
