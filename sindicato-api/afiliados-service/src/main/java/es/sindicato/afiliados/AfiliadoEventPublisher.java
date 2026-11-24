package es.sindicato.afiliados;

import es.sindicato.common.ApiException;
import es.sindicato.events.AfiliadoCreadoEvent;
import es.sindicato.events.BajaSolicitadaEvent;
import es.sindicato.events.EstadoAfiliadoEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AfiliadoEventPublisher {
    private final KafkaTemplate<String, Object> kafka;
    @Value("${app.public-url:http://localhost:8081}")
    private String publicUrl;

    public AfiliadoEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @CircuitBreaker(name = "afiliadosKafka", fallbackMethod = "falloKafka")
    public void publicarCreacion(Afiliado afiliado) {
        enviar("afiliados.creados", afiliado.getId().toString(),
                new AfiliadoCreadoEvent(afiliado.getId(), afiliado.getNombre(), afiliado.getEmail(),
                        publicUrl + "/api/afiliados/confirmar?token=" + afiliado.getTokenConfirmacion()));
    }

    @CircuitBreaker(name = "afiliadosKafka", fallbackMethod = "falloKafka")
    public void publicarBaja(Afiliado afiliado) {
        enviar("afiliados.bajas-solicitadas", afiliado.getId().toString(),
                new BajaSolicitadaEvent(afiliado.getId(), afiliado.getNombre(), afiliado.getEmail(),
                        publicUrl + "/api/afiliados/confirmar?token=" + afiliado.getTokenConfirmacion()));
    }

    @CircuitBreaker(name = "afiliadosKafka", fallbackMethod = "falloKafka")
    public void publicarEstado(Afiliado afiliado) {
        enviar("afiliados.estados", afiliado.getId().toString(),
                new EstadoAfiliadoEvent(afiliado.getId(), afiliado.getEmail(), afiliado.getEstado().name()));
    }

    private void enviar(String topic, String key, Object event) {
        try {
            kafka.send(topic, key, event).get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo publicar el evento en Kafka", ex);
        }
    }

    private void falloKafka(Afiliado afiliado, Exception ex) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se puede conectar con Kafka");
    }
}
