package es.sindicato.notificaciones;

import es.sindicato.events.AfiliadoCreadoEvent;
import es.sindicato.events.BajaSolicitadaEvent;
import es.sindicato.events.EstadoAfiliadoEvent;
import es.sindicato.events.NoticiaPublicadaEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificacionListener {
    private final JavaMailSender mailSender;
    private final AfiliadoEmailRepository afiliados;
    @Value("${app.mail.from}") private String remitente;

    public NotificacionListener(JavaMailSender mailSender, AfiliadoEmailRepository afiliados) {
        this.mailSender = mailSender;
        this.afiliados = afiliados;
    }

    @KafkaListener(topics = "afiliados.creados", groupId = "notificaciones-service")
    public void afiliadoCreado(AfiliadoCreadoEvent event) {
        enviar(event.email(), "Confirma tu afiliación",
                "Hola " + event.nombre() + ", confirma tu correo y tu alta en el sindicato: " + event.urlConfirmacion());
    }

    @KafkaListener(topics = "afiliados.bajas-solicitadas", groupId = "notificaciones-service")
    public void bajaSolicitada(BajaSolicitadaEvent event) {
        enviar(event.email(), "Confirma tu baja",
                "Hola " + event.nombre() + ", confirma tu solicitud de baja en el sindicato: " + event.urlConfirmacion());
    }

    @KafkaListener(topics = "afiliados.estados", groupId = "notificaciones-service")
    @Transactional
    public void estadoAfiliado(EstadoAfiliadoEvent event) {
        AfiliadoEmail destinatario = afiliados.findByAfiliadoId(event.afiliadoId()).orElseGet(AfiliadoEmail::new);
        destinatario.setAfiliadoId(event.afiliadoId());
        destinatario.setEmail(event.email());
        destinatario.setActivo("ALTA".equals(event.estado()));
        afiliados.save(destinatario);
    }

    @KafkaListener(topics = "noticias.publicadas", groupId = "notificaciones-service")
    public void noticiaPublicada(NoticiaPublicadaEvent event) {
        String cuerpo = event.titulo() + "\n\n" + event.contenido();
        for (AfiliadoEmail afiliado : afiliados.findByActivoTrue()) {
            enviar(afiliado.getEmail(), "Nueva noticia del sindicato: " + event.titulo(), cuerpo);
        }
    }

    private void enviar(String destino, String asunto, String cuerpo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(cuerpo, false);
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            throw new IllegalStateException("No se puede conectar con el servidor SMTP", ex);
        }
    }
}
