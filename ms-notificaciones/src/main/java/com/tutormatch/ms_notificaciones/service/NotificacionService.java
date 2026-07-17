package com.tutormatch.ms_notificaciones.service;

import com.tutormatch.ms_notificaciones.dto.NotificacionRequest;
import com.tutormatch.ms_notificaciones.entity.Notificacion;
import com.tutormatch.ms_notificaciones.repository.NotificacionRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j // Anotación de Lombok para poder usar logs en la consola
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final JavaMailSender mailSender;

    // Se inyecta el mismo correo configurado en spring.mail.username,
    // así evitamos hardcodearlo dos veces.
    @Value("${spring.mail.username}")
    private String remitente;

    // Método para obtener el historial
    public java.util.List<Notificacion> obtenerNotificacionesNoLeidas(java.util.UUID usuarioId) {
        return notificacionRepository.findByUsuarioIdAndLeidaFalseOrderByCreadoEnDesc(usuarioId);
    }

    // Método para marcar como leída
    public void marcarComoLeida(java.util.UUID id) {
        notificacionRepository.findById(id).ifPresent(notificacion -> {
            notificacion.setLeida(true);
            notificacionRepository.save(notificacion);
            log.info("Notificación {} marcada como leída.", id);
        });
    }

    // Método que almacena y envía notificaciones
    public void procesarNotificacion(NotificacionRequest request) {

        // 1. Guardar en Base de Datos
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(request.getUsuarioId());
        notificacion.setTitulo(request.getTitulo());
        notificacion.setMensaje(request.getMensaje());

        notificacionRepository.save(notificacion);
        log.info("Notificación guardada en BD para el usuario: {}", request.getUsuarioId());

        // 2. Enviar Correo
        enviarCorreoHtml(request.getCorreoDestino(), request.getTitulo(), request.getMensaje());
    }

    // Método que envía el correo HTML
    private void enviarCorreoHtml(String destinatario, String asunto, String contenido) {
        try {
            // MimeMessage para soportar diseño HTML en lugar de texto plano
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(remitente); // <-- FIX: sin esto, Gmail rechaza el envío
            helper.setTo(destinatario);
            helper.setSubject(asunto);

            String htmlTemplate = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;">
                        <div style="background-color: #2563eb; color: white; padding: 20px; text-align: center;">
                            <h2 style="margin: 0;">TutorMatch</h2>
                        </div>
                        <div style="padding: 30px; color: #1e293b; line-height: 1.6;">
                            <h3 style="color: #2563eb; margin-top: 0;">%s</h3>
                            <p>%s</p>
                        </div>
                        <div style="background-color: #f8fafc; padding: 15px; text-align: center; color: #64748b; font-size: 0.85rem;">
                            Este es un mensaje automático de la red de tutorías universitarias TutorMatch.
                        </div>
                    </div>
                    """
                    .formatted(asunto, contenido);

            helper.setText(htmlTemplate, true);

            // Acción que se conecta con el SMTP de Gmail
            mailSender.send(mensaje);
            log.info("Correo enviado con éxito a: {}", destinatario);

        } catch (MessagingException e) {
            // Fallos al CONSTRUIR el mensaje (adjuntos, encoding, etc.)
            log.error("Fallo al construir el correo para {}. Error: {}", destinatario, e.getMessage());
        } catch (MailException e) {
            // FIX: Fallos al ENVIAR (SMTP caído, credenciales, remitente inválido, etc.)
            // MailSendException es una RuntimeException y hereda de MailException,
            // por eso antes se escapaba sin capturar.
            log.error("Fallo al enviar correo (SMTP) a {}. Error: {}", destinatario, e.getMessage());
        }
    }
}