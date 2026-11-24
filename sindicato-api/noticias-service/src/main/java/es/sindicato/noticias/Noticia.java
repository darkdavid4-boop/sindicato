package es.sindicato.noticias;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "noticias")
@Getter @Setter @NoArgsConstructor
public class Noticia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String titulo;
    @Column(nullable = false, columnDefinition = "TEXT") private String contenido;
    @Column(nullable = false) private LocalDateTime fechaPublicacion;
    private Long actividadId;
    @Column(length = 512) private String imagenUrl;
}
