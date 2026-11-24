package es.sindicato.afiliados;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "afiliados")
@Getter
@Setter
@NoArgsConstructor
public class Afiliado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private String apellidos;
    @Column(nullable = false, unique = true) private String dni;
    @Column(nullable = false, unique = true) private String email;
    private String telefono;
    @Column(length = 512) private String imagenUrl;
    @JsonIgnore private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EstadoAfiliado estado = EstadoAfiliado.PENDIENTE;
    @JsonIgnore private String tokenConfirmacion;
    @JsonIgnore private String accionPendiente;
}
