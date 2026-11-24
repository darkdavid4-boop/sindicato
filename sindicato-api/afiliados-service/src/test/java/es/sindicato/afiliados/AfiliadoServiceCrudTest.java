package es.sindicato.afiliados;

import es.sindicato.common.ApiException;
import es.sindicato.common.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfiliadoServiceCrudTest {
    private AfiliadoRepository repository;
    private AfiliadoEventPublisher eventPublisher;
    private PasswordEncoder passwordEncoder;
    private AfiliadoService service;

    @BeforeEach
    void setUp() {
        repository = mock(AfiliadoRepository.class);
        eventPublisher = mock(AfiliadoEventPublisher.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AfiliadoService(repository, eventPublisher, mock(ImageStorage.class), passwordEncoder);
    }

    @Test
    void createsPendingAffiliateAndPublishesCreation() {
        when(repository.save(any(Afiliado.class))).thenAnswer(invocation -> {
            Afiliado afiliado = invocation.getArgument(0);
            afiliado.setId(7L);
            return afiliado;
        });
        when(passwordEncoder.encode("una-clave-segura")).thenReturn("hash-seguro");

        Afiliado created = service.crear(request("una-clave-segura"));

        assertEquals(7L, created.getId());
        assertEquals(EstadoAfiliado.PENDIENTE, created.getEstado());
        assertEquals("hash-seguro", created.getPasswordHash());
        verify(passwordEncoder).encode("una-clave-segura");
        verify(eventPublisher).publicarCreacion(created);
    }

    @Test
    void getsAffiliateAndReportsMissingIds() {
        Afiliado afiliado = afiliado(4L, EstadoAfiliado.ALTA);
        when(repository.findById(4L)).thenReturn(Optional.of(afiliado));

        assertEquals(afiliado, service.obtener(4L));
        ApiException error = assertThrows(ApiException.class, () -> service.obtener(5L));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void listsAffiliatesByTenAndRejectsAnEmptyRegistry() {
        Afiliado afiliado = afiliado(2L, EstadoAfiliado.ALTA);
        Page<Afiliado> page = new PageImpl<>(List.of(afiliado));
        when(repository.count()).thenReturn(1L);
        when(repository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        assertEquals(page, service.listar(-1));
        verify(repository).findAll(PageRequest.of(0, 10));

        when(repository.count()).thenReturn(0L);
        ApiException error = assertThrows(ApiException.class, () -> service.listar(0));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void updatesAffiliateAndPublishesActiveStatus() {
        Afiliado afiliado = afiliado(3L, EstadoAfiliado.ALTA);
        when(repository.findById(3L)).thenReturn(Optional.of(afiliado));
        when(repository.save(afiliado)).thenReturn(afiliado);
        when(passwordEncoder.encode("nueva-clave-segura")).thenReturn("nuevo-hash");

        Afiliado updated = service.actualizar(3L, request("nueva-clave-segura"));

        assertEquals("Ana", updated.getNombre());
        assertEquals("nuevo-hash", updated.getPasswordHash());
        verify(eventPublisher).publicarEstado(afiliado);
    }

    @Test
    void deletesAffiliateAndPublishesItsInactiveStatus() {
        Afiliado afiliado = afiliado(9L, EstadoAfiliado.ALTA);
        when(repository.findById(9L)).thenReturn(Optional.of(afiliado));

        service.eliminar(9L);

        assertEquals(EstadoAfiliado.BAJA, afiliado.getEstado());
        verify(eventPublisher).publicarEstado(afiliado);
        verify(repository).delete(afiliado);
    }

    private AfiliadoRequest request(String password) {
        return new AfiliadoRequest("Ana", "Pérez", "12345678Z", "ana@example.com", null, password);
    }

    private Afiliado afiliado(Long id, EstadoAfiliado estado) {
        Afiliado afiliado = new Afiliado();
        afiliado.setId(id);
        afiliado.setNombre("Ana");
        afiliado.setApellidos("Pérez");
        afiliado.setDni("12345678Z");
        afiliado.setEmail("ana@example.com");
        afiliado.setEstado(estado);
        return afiliado;
    }
}
