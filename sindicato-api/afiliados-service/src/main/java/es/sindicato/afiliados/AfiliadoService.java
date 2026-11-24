package es.sindicato.afiliados;

import es.sindicato.common.ApiException;
import es.sindicato.common.ImageStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.UUID;

@Service
public class AfiliadoService {
    private final AfiliadoRepository repository;
    private final AfiliadoEventPublisher eventPublisher;
    private final ImageStorage imageStorage;
    private final PasswordEncoder passwordEncoder;

    public AfiliadoService(AfiliadoRepository repository, AfiliadoEventPublisher eventPublisher,
                           ImageStorage imageStorage, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.imageStorage = imageStorage;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Afiliado crear(AfiliadoRequest request) {
        return crear(request, null);
    }

    @Transactional
    public Afiliado crear(AfiliadoRequest request, MultipartFile imagen) {
        Afiliado afiliado = fromRequest(request);
        if (imagen != null) afiliado.setImagenUrl(imageStorage.store(imagen, "afiliados"));
        if (request.password() != null) afiliado.setPasswordHash(passwordEncoder.encode(request.password()));
        afiliado.setEstado(EstadoAfiliado.PENDIENTE);
        afiliado.setAccionPendiente("ALTA");
        afiliado.setTokenConfirmacion(UUID.randomUUID().toString());
        afiliado = repository.save(afiliado);
        eventPublisher.publicarCreacion(afiliado);
        return afiliado;
    }

    @Transactional(readOnly = true)
    public Afiliado obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Afiliado no encontrado"));
    }

    @Transactional(readOnly = true)
    public Page<Afiliado> listar(int pagina) {
        if (repository.count() == 0) throw new ApiException(HttpStatus.NOT_FOUND, "No hay afiliados registrados");
        return repository.findAll(PageRequest.of(Math.max(0, pagina), 10));
    }

    @Transactional
    public Afiliado actualizar(Long id, AfiliadoRequest request) {
        return actualizar(id, request, null);
    }

    @Transactional
    public Afiliado actualizar(Long id, AfiliadoRequest request, MultipartFile imagen) {
        Afiliado afiliado = obtener(id);
        afiliado.setNombre(request.nombre());
        afiliado.setApellidos(request.apellidos());
        afiliado.setDni(request.dni());
        afiliado.setEmail(request.email());
        afiliado.setTelefono(request.telefono());
        if (request.password() != null) afiliado.setPasswordHash(passwordEncoder.encode(request.password()));
        if (imagen != null && !imagen.isEmpty()) afiliado.setImagenUrl(imageStorage.store(imagen, "afiliados"));
        afiliado = repository.save(afiliado);
        if (afiliado.getEstado() == EstadoAfiliado.ALTA) eventPublisher.publicarEstado(afiliado);
        return afiliado;
    }

    @Transactional
    public void eliminar(Long id) {
        Afiliado afiliado = obtener(id);
        afiliado.setEstado(EstadoAfiliado.BAJA);
        eventPublisher.publicarEstado(afiliado);
        repository.delete(afiliado);
    }

    @Transactional
    public void solicitarBaja(Long id) {
        Afiliado afiliado = obtener(id);
        afiliado.setAccionPendiente("BAJA");
        afiliado.setTokenConfirmacion(UUID.randomUUID().toString());
        repository.save(afiliado);
        eventPublisher.publicarBaja(afiliado);
    }

    @Transactional
    public Afiliado confirmar(String token) {
        Afiliado afiliado = repository.findByTokenConfirmacion(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El enlace de confirmación no es válido"));
        afiliado.setEstado("BAJA".equals(afiliado.getAccionPendiente()) ? EstadoAfiliado.BAJA : EstadoAfiliado.ALTA);
        afiliado.setAccionPendiente(null);
        afiliado.setTokenConfirmacion(null);
        afiliado = repository.save(afiliado);
        eventPublisher.publicarEstado(afiliado);
        return afiliado;
    }

    @Transactional(readOnly = true)
    public Afiliado autenticar(String email, String password) {
        Afiliado afiliado = repository.findByEmailIgnoreCase(email)
                .filter(candidate -> candidate.getEstado() == EstadoAfiliado.ALTA)
                .filter(candidate -> candidate.getPasswordHash() != null
                        && passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        return afiliado;
    }

    private Afiliado fromRequest(AfiliadoRequest request) {
        Afiliado afiliado = new Afiliado();
        afiliado.setNombre(request.nombre());
        afiliado.setApellidos(request.apellidos());
        afiliado.setDni(request.dni().toUpperCase(Locale.ROOT));
        afiliado.setEmail(request.email());
        afiliado.setTelefono(request.telefono());
        return afiliado;
    }
}
