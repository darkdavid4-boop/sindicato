package es.sindicato.noticias;

import es.sindicato.events.ActividadCreadaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ActividadCreadaListener {
    private final NoticiaService service;
    public ActividadCreadaListener(NoticiaService service) { this.service = service; }
    @KafkaListener(topics = "actividades.creadas", groupId = "noticias-service")
    public void onActividadCreada(ActividadCreadaEvent event) { service.crearDesdeActividad(event); }
}
