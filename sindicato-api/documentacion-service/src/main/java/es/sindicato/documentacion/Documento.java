package es.sindicato.documentacion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "documentos", indexes = @Index(name = "idx_documentos_afiliado", columnList = "afiliado_id"))
@Getter
@Setter
@NoArgsConstructor
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "afiliado_id", nullable = false)
    private Long afiliadoId;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean publicado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumento tipo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_contenido", nullable = false, length = 100)
    private String tipoContenido;

    @Column(nullable = false)
    private long tamano;

    @JsonIgnore
    @Column(name = "clave_almacenamiento", nullable = false, unique = true, length = 50)
    private String claveAlmacenamiento;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = Instant.now();
    }
}
