package com.tutormatch.ms_core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tutormatch.ms_core.client.NotificacionClient;
import com.tutormatch.ms_core.dto.AgendaAlumnoDto;
import com.tutormatch.ms_core.dto.InscripcionRequestDto;
import com.tutormatch.ms_core.dto.NotificacionRequestDto;
import com.tutormatch.ms_core.entity.Inscripcion;
import com.tutormatch.ms_core.entity.Sesion;
import com.tutormatch.ms_core.repository.InscripcionRepository;
import com.tutormatch.ms_core.repository.SesionRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class InscripcionService {

    private static final String INSCRIPCION_CONFIRMADA = "CONFIRMADA";
    private static final String INSCRIPCION_CANCELADA = "CANCELADA";
    private static final String SESION_ACTIVA = "ACTIVA";

    private final InscripcionRepository inscripcionRepository;
    private final SesionRepository sesionRepository;
    private final NotificacionClient notificacionClient;
    private final SesionService sesionService;

    public InscripcionService(InscripcionRepository inscripcionRepository,
            SesionRepository sesionRepository,
            SesionService sesionService, NotificacionClient notificacionClient) {
        this.inscripcionRepository = inscripcionRepository;
        this.sesionRepository = sesionRepository;
        this.sesionService = sesionService;
        this.notificacionClient = notificacionClient;
    }

    // =========================================================================
    // HU-14: Inscribirse a una sesión
    // =========================================================================

    @Transactional
    public Inscripcion inscribirse(String correoAlumno, InscripcionRequestDto dto, UUID alumnoId) {

        // 1. Verificar que la sesión existe y está activa
        Sesion sesion = sesionRepository.findById(dto.getSesionId())
                .orElseThrow(() -> new IllegalArgumentException("La sesión no existe."));

        if (!SESION_ACTIVA.equals(sesion.getEstado())) {
            throw new IllegalArgumentException("Esta sesión ya no está disponible.");
        }

        if (sesion.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("No puedes inscribirte a una sesión que ya pasó.");
        }

        // 2. Buscar si ya existe un registro de inscripción (confirmada o cancelada)
        Inscripcion inscripcion = inscripcionRepository
                .findBySesionIdAndAlumnoId(dto.getSesionId(), alumnoId)
                .orElse(new Inscripcion());

        if (INSCRIPCION_CONFIRMADA.equals(inscripcion.getEstado())) {
            throw new IllegalArgumentException("Ya estás inscrito en esta sesión.");
        }

        // 3. Verificar cupo
        if (sesion.getCupoDisponible() <= 0) {
            throw new IllegalArgumentException("Esta sesión ya no tiene cupo disponible.");
        }

        // 4. Crear o actualizar inscripción
        inscripcion.setSesionId(dto.getSesionId());
        inscripcion.setAlumnoId(alumnoId);
        inscripcion.setEstado(INSCRIPCION_CONFIRMADA);
        inscripcion.setFechaInscripcion(LocalDateTime.now());

        // 5. Decrementar cupo
        sesion.setCupoDisponible(sesion.getCupoDisponible() - 1);
        sesionRepository.save(sesion);

        // FIX: guardamos la inscripción ANTES de intentar notificar,
        // así el registro queda persistido pase lo que pase con el correo.
        Inscripcion guardada = inscripcionRepository.save(inscripcion);

        // =========================================================
        // DISPARAR NOTIFICACIÓN (best-effort: si falla, no debe tumbar la inscripción)
        // =========================================================
        try {
            String fechaFormateada = sesion.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            String mensajeHtml = String.format(
                    "Te has inscrito exitosamente a la tutoría de <strong style='color: #2563eb;'>%s</strong>.<br><br>"
                            +
                            "<strong>Detalles de la sesión:</strong><br>" +
                            "<ul>" +
                            "<li><strong>Tutor:</strong> %s</li>" +
                            "<li><strong>Fecha y Hora:</strong> %s</li>" +
                            "<li><strong>Lugar/Enlace:</strong> %s</li>" +
                            "</ul><br>" +
                            "Te sugerimos estar listo 5 minutos antes. ¡Mucho éxito!",
                    sesion.getTitulo(),
                    sesion.getTutorNombre(),
                    fechaFormateada,
                    sesion.getLugar());

            NotificacionRequestDto notificacion = new NotificacionRequestDto();
            notificacion.setUsuarioId(alumnoId);
            notificacion.setCorreoDestino(correoAlumno);
            notificacion.setTitulo("Inscripción Confirmada: " + sesion.getTitulo());
            notificacion.setMensaje(mensajeHtml);

            notificacionClient.enviarNotificacion(notificacion);
        } catch (Exception ex) {
            log.warn("No se pudo enviar la notificación de inscripción para alumno {} en sesión {}: {}",
                    alumnoId, dto.getSesionId(), ex.getMessage());
        }

        return guardada;
    }

    // =========================================================================
    // HU-15: Agenda del Alumno — sesiones futuras inscritas (con lugar revelado)
    // =========================================================================

    public List<AgendaAlumnoDto> getAgendaAlumno(UUID alumnoId) {
        List<Inscripcion> inscripciones = inscripcionRepository
                .findByAlumnoIdAndEstado(alumnoId, INSCRIPCION_CONFIRMADA);

        LocalDateTime ahora = LocalDateTime.now();

        return inscripciones.stream()
                // Solo sesiones futuras
                .filter(insc -> {
                    Sesion s = sesionRepository.findById(insc.getSesionId()).orElse(null);
                    return s != null && SESION_ACTIVA.equals(s.getEstado()) && s.getFechaHora().isAfter(ahora);
                })
                // Ordenar cronológicamente
                .sorted((a, b) -> {
                    Sesion sA = sesionRepository.findById(a.getSesionId()).orElseThrow();
                    Sesion sB = sesionRepository.findById(b.getSesionId()).orElseThrow();
                    return sA.getFechaHora().compareTo(sB.getFechaHora());
                })
                .map(insc -> {
                    Sesion sesion = sesionRepository.findById(insc.getSesionId()).orElseThrow();
                    int inscritos = (int) inscripcionRepository
                            .countBySesionIdAndEstado(sesion.getId(), INSCRIPCION_CONFIRMADA);

                    return new AgendaAlumnoDto(
                            insc.getId(),
                            sesion.getId(),
                            sesion.getTutorNombre(),
                            sesion.getTitulo(),
                            sesion.getDescripcion(),
                            sesion.getLugar(), // LUGAR REVELADO al inscrito
                            sesion.getFechaHora(),
                            sesion.getCupoMaximo(),
                            sesion.getCupoDisponible(),
                            inscritos,
                            insc.getFechaInscripcion());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean estaInscrito(UUID sesionId, UUID alumnoId) {
        return inscripcionRepository.findBySesionIdAndAlumnoIdAndEstado(sesionId, alumnoId, INSCRIPCION_CONFIRMADA)
                .isPresent();
    }

    // =========================================================================
    // HU-16: Cancelar inscripción
    // =========================================================================

    @Transactional
    public void cancelarInscripcion(String correoAlumno, UUID inscripcionId, UUID alumnoId) {

        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new IllegalArgumentException("Inscripción no encontrada."));

        Sesion sesion = sesionRepository.findById(inscripcion.getSesionId()).orElseThrow();

        // Solo el alumno propietario puede cancelar su inscripción
        if (!inscripcion.getAlumnoId().equals(alumnoId)) {
            throw new SecurityException("No tienes permiso para cancelar esta inscripción.");
        }

        // Ya está cancelada
        if (INSCRIPCION_CANCELADA.equals(inscripcion.getEstado())) {
            throw new IllegalArgumentException("Esta inscripción ya está cancelada.");
        }

        // Cambiar estado de la inscripción
        inscripcion.setEstado(INSCRIPCION_CANCELADA);
        inscripcionRepository.save(inscripcion);

        // Incrementar cupo de la sesión
        if (SESION_ACTIVA.equals(sesion.getEstado())) {
            sesion.setCupoDisponible(sesion.getCupoDisponible() + 1);
            sesionRepository.save(sesion);
        }

        // =========================================================
        // DISPARAR NOTIFICACIÓN (best-effort: si falla, no debe tumbar la cancelación)
        // =========================================================
        try {
            String fechaFormateada = sesion.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            String mensajeHtml = String.format(
                    "Te confirmamos que se ha <strong style='color: #ef4444;'>CANCELADO</strong> tu inscripción a la tutoría de <strong>%s</strong>.<br><br>"
                            +
                            "<strong>Detalles de la sesión cancelada:</strong><br>" +
                            "<ul>" +
                            "<li><strong>Tutor:</strong> %s</li>" +
                            "<li><strong>Fecha y Hora:</strong> %s</li>" +
                            "<li><strong>Lugar/Enlace:</strong> %s</li>" +
                            "</ul><br>" +
                            "Ese espacio ha sido liberado para otro compañero. ¡Puedes buscar nuevos horarios en el catálogo cuando lo desees!",
                    sesion.getTitulo(),
                    sesion.getTutorNombre(),
                    fechaFormateada,
                    sesion.getLugar());

            NotificacionRequestDto notificacion = new NotificacionRequestDto();
            notificacion.setUsuarioId(alumnoId);
            notificacion.setCorreoDestino(correoAlumno);
            notificacion.setTitulo("Inscripción Cancelada: " + sesion.getTitulo());
            notificacion.setMensaje(mensajeHtml);

            notificacionClient.enviarNotificacion(notificacion);
        } catch (Exception ex) {
            log.warn("No se pudo enviar la notificación de cancelación para alumno {} en inscripción {}: {}",
                    alumnoId, inscripcionId, ex.getMessage());
        }
    }
}