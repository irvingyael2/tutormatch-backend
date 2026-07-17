package com.tutormatch.ms_core.service;

import com.tutormatch.ms_core.client.NotificacionClient;
import com.tutormatch.ms_core.client.UsuarioClient;
import com.tutormatch.ms_core.dto.NotificacionRequestDto;
import com.tutormatch.ms_core.dto.PreguntaRequestDto;
import com.tutormatch.ms_core.dto.PreguntaResponseDto;
import com.tutormatch.ms_core.dto.RespuestaRequestDto;
import com.tutormatch.ms_core.dto.UsuarioResponseDto;
import com.tutormatch.ms_core.entity.Sesion;
import com.tutormatch.ms_core.entity.SesionPregunta;
import com.tutormatch.ms_core.repository.InscripcionRepository;
import com.tutormatch.ms_core.repository.PreguntaRepository;
import com.tutormatch.ms_core.repository.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PreguntaService {

    private static final String INSCRIPCION_CONFIRMADA = "CONFIRMADA";

    private final PreguntaRepository preguntaRepository;
    private final SesionRepository sesionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final NotificacionClient notificacionClient;
    private final UsuarioClient usuarioClient;

    public PreguntaService(PreguntaRepository preguntaRepository, SesionRepository sesionRepository,
            InscripcionRepository inscripcionRepository, NotificacionClient notificacionClient,
            UsuarioClient usuarioClient) {
        this.preguntaRepository = preguntaRepository;
        this.sesionRepository = sesionRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.notificacionClient = notificacionClient;
        this.usuarioClient = usuarioClient;
    }

    @Transactional
    public PreguntaResponseDto crearPregunta(UUID sesionId, UUID alumnoId, String alumnoNombre,
            PreguntaRequestDto dto) {
        if (dto.getPregunta() == null || dto.getPregunta().isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía.");
        }

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        boolean inscrito = inscripcionRepository
                .findBySesionIdAndAlumnoIdAndEstado(sesionId, alumnoId, INSCRIPCION_CONFIRMADA)
                .isPresent();
        if (!inscrito) {
            throw new IllegalArgumentException(
                    "Solo puedes publicar preguntas si ya estás inscrito formalmente en la sesión.");
        }

        String nombreCompleto = alumnoNombre != null && !alumnoNombre.isBlank()
                ? alumnoNombre.trim()
                : alumnoId.toString();

        SesionPregunta pregunta = new SesionPregunta();
        pregunta.setSesionId(sesion.getId());
        pregunta.setAlumnoId(alumnoId);
        pregunta.setAlumnoNombre(nombreCompleto);
        pregunta.setPregunta(dto.getPregunta().trim());

        SesionPregunta guardada = preguntaRepository.save(pregunta);
        return mapToDto(guardada);
    }

    @Transactional(readOnly = true)
    public List<PreguntaResponseDto> listarPreguntas(UUID sesionId) {
        return preguntaRepository.findBySesionIdOrderByCreadoEnAsc(sesionId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PreguntaResponseDto responderPregunta(UUID sesionId, UUID preguntaId, UUID tutorId,
            RespuestaRequestDto dto) {
        if (dto.getRespuesta() == null || dto.getRespuesta().isBlank()) {
            throw new IllegalArgumentException("La respuesta no puede estar vacía.");
        }

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        if (!sesion.getTutorId().equals(tutorId)) {
            throw new IllegalArgumentException("Solo el tutor que creó la sesión puede responder preguntas.");
        }

        SesionPregunta pregunta = preguntaRepository.findByIdAndSesionId(preguntaId, sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada para esta sesión."));

        pregunta.setRespuesta(dto.getRespuesta().trim());
        pregunta.setRespondidoEn(LocalDateTime.now());

        UsuarioResponseDto autorPregunta = usuarioClient.obtenerUsuarioPorId(pregunta.getAlumnoId());

        UsuarioResponseDto tutor = usuarioClient.obtenerUsuarioPorId(tutorId);

        String mensajeHtml = String.format(
                "¡Hola! <strong>%s</strong> ha respondido a tu duda en el foro de la tutoría.<br><br>" +
                        "<div style='border-left: 4px solid #d1d5db; padding-left: 15px; margin-bottom: 20px; color: #4b5563; font-style: italic;'>"
                        +
                        "<strong>Tu pregunta:</strong><br> \"%s\"" +
                        "</div>" +
                        "<div style='border-left: 4px solid #2563eb; padding-left: 15px; margin-bottom: 20px;'>" +
                        "<strong>Respuesta de %s:</strong><br> \"%s\"" +
                        "</div><br>" +
                        "Entra a la plataforma para continuar la conversación.",
                tutor.getNombre(),
                pregunta.getPregunta(),
                tutor.getNombre(),
                pregunta.getRespuesta());

        NotificacionRequestDto notificacion = new NotificacionRequestDto();
        notificacion.setUsuarioId(pregunta.getAlumnoId());
        notificacion.setCorreoDestino(autorPregunta.getEmail());
        notificacion.setTitulo("Nueva respuesta en el Foro");
        notificacion.setMensaje(mensajeHtml);

        notificacionClient.enviarNotificacion(notificacion);

        return mapToDto(preguntaRepository.save(pregunta));
    }

    @Transactional
    public void eliminarPregunta(UUID sesionId, UUID preguntaId, UUID alumnoId) {
        SesionPregunta pregunta = preguntaRepository.findByIdAndSesionId(preguntaId, sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Pregunta no encontrada para esta sesión."));

        if (!pregunta.getAlumnoId().equals(alumnoId)) {
            throw new IllegalArgumentException("Solo el alumno que realizó la pregunta puede eliminarla.");
        }

        preguntaRepository.delete(pregunta);
    }

    private PreguntaResponseDto mapToDto(SesionPregunta pregunta) {
        return new PreguntaResponseDto(
                pregunta.getId(),
                pregunta.getSesionId(),
                pregunta.getAlumnoId(),
                pregunta.getAlumnoNombre(),
                pregunta.getPregunta(),
                pregunta.getRespuesta(),
                pregunta.getCreadoEn(),
                pregunta.getRespondidoEn());
    }
}
