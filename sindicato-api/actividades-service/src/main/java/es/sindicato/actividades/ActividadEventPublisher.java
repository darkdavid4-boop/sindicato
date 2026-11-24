package es.sindicato.actividades;

import es.sindicato.common.ApiException;
import es.sindicato.events.ActividadCreadaEvent;
import es.sindicato.events.ParticipacionEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ActividadEventPublisher {
    private final KafkaTemplate<String, Object> kafka;

    public ActividadEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @CircuitBreaker(name = "actividadesKafka", fallbackMethod = "falloKafka")
    public void publicarActividad(ActividadCreadaEvent event) {
        enviar("actividades.creadas", event.actividadId().toString(), event);
    }

    @CircuitBreaker(name = "actividadesKafka", fallbackMethod = "falloKafka")
    public void publicarParticipacion(ParticipacionEvent event) {
        enviar("participaciones.eventos", event.participacionId().toString(), event);
    }

    private void enviar(String topic, String key, Object event) {
        try {
            kafka.send(topic, key, event).get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo publicar el evento en Kafka", ex);
        }
    }

    private void falloKafka(ActividadCreadaEvent event, Exception ex) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se puede conectar con Kafka");
    }

    private void falloKafka(ParticipacionEvent event, Exception ex) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se puede conectar con Kafka");
    }
}
