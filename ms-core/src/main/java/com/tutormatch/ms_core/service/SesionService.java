package com.tutormatch.ms_core.service;

import com.tutormatch.ms_core.dto.SesionRequestDto;
import com.tutormatch.ms_core.dto.SesionResponseDto;
import com.tutormatch.ms_core.dto.SesionUpdateDto;
import com.tutormatch.ms_core.entity.Sesion;
import com.tutormatch.ms_core.repository.InscripcionRepository;
import com.tutormatch.ms_core.repository.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SesionService {

    private static final String ESTADO_ACTIVA    = "ACTIVA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String INSCRIPCION_CONFIRMADA = "CONFIRMADA";

    private final SesionRepository sesionRepository;
    private final InscripcionRepository inscripcionRepository;

    public SesionService(SesionRepository sesionRepository,
                         InscripcionRepository inscripcionRepository) {
        this.sesionRepository = sesionRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    // =========================================================================
    // HU-09: Publicar nueva sesión
    // =========================================================================

    /**
     * Publica una nueva sesión de tutoría con las siguientes validaciones:
     * 1. La fecha debe ser al menos 1 hora en el futuro.
     * 2. El cupo máximo debe ser >= 1.
     * 3. No puede haber cruce de horarios con otras sesiones ACTIVAS del mismo tutor (±2h).
     */
    @Transactional
    public SesionResponseDto publicarSesion(SesionRequestDto dto, UUID tutorId) {

        // --- VALIDACIÓN 1: Fecha futura (mínimo 1 hora) ---
        LocalDateTime limiteMinimo = LocalDateTime.now().plusHours(1);
        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(limiteMinimo)) {
            throw new IllegalArgumentException(
                "La fecha de la sesión debe ser al menos 1 hora en el futuro."
            );
        }

        // --- VALIDACIÓN 2: Cupo positivo ---
        if (dto.getCupoMaximo() == null || dto.getCupoMaximo() < 1) {
            throw new IllegalArgumentException(
                "El cupo máximo debe ser al menos 1 alumno."
            );
        }

        // --- VALIDACIÓN 3: Cruce de horarios con sesiones ACTIVAS (ventana ±2h) ---
        List<Sesion> sesionesFuturas = sesionRepository.findByTutorIdAndEstadoAndFechaHoraAfter(
            tutorId, ESTADO_ACTIVA, LocalDateTime.now()
        );

        LocalDateTime nuevaFecha = dto.getFechaHora();
        for (Sesion existente : sesionesFuturas) {
            LocalDateTime inicio = existente.getFechaHora().minusHours(2);
            LocalDateTime fin    = existente.getFechaHora().plusHours(2);
            if (nuevaFecha.isAfter(inicio) && nuevaFecha.isBefore(fin)) {
                throw new IllegalArgumentException(
                    "Ya tienes una sesión programada cerca de ese horario (" +
                    existente.getFechaHora() + "). Debe haber al menos 2 horas de diferencia."
                );
            }
        }

        // --- GUARDAR ---
        Sesion nueva = new Sesion();
        nueva.setTutorId(tutorId);
        nueva.setTitulo(dto.getTitulo());
        nueva.setDescripcion(dto.getDescripcion());
        nueva.setLugar(dto.getLugar());
        nueva.setFechaHora(dto.getFechaHora());
        nueva.setCupoMaximo(dto.getCupoMaximo());
        nueva.setCupoDisponible(dto.getCupoMaximo()); // Al crear, disponible = máximo
        nueva.setEstado(ESTADO_ACTIVA);

        return mapToResponseDto(sesionRepository.save(nueva));
    }

    // =========================================================================
    // HU-10: Visualizar "Mi Agenda" — sesiones futuras ordenadas cronológicamente
    // =========================================================================

    /**
     * Retorna todas las sesiones ACTIVAS y futuras del tutor ordenadas cronológicamente.
     * Cada sesión incluye el contador de inscritos (inscritos/cupoMaximo).
     *
     * @param tutorId UUID del tutor extraído del JWT
     */
    public List<SesionResponseDto> obtenerAgendaTutor(UUID tutorId) {
        List<Sesion> sesiones = sesionRepository
            .findByTutorIdAndEstadoAndFechaHoraAfterOrderByFechaHoraAsc(
                tutorId, ESTADO_ACTIVA, LocalDateTime.now()
            );

        return sesiones.stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());
    }

    // =========================================================================
    // HU-11: Editar una sesión existente
    // =========================================================================

    /**
     * Actualiza los datos de una sesión perteneciente al tutor autenticado.
     *
     * Reglas de negocio:
     * - Solo el tutor propietario puede editar su sesión.
     * - Solo se pueden editar sesiones en estado ACTIVA.
     * - Si la sesión ya tiene alumnos inscritos (CONFIRMADA), el campo fechaHora
     *   queda bloqueado: se lanza excepción si el cliente intenta cambiarlo.
     * - El cupoMaximo no puede reducirse por debajo del número actual de inscritos.
     *
     * @param sesionId ID de la sesión a editar
     * @param dto      Datos nuevos del formulario
     * @param tutorId  UUID del tutor autenticado (del JWT)
     */
    @Transactional
    public SesionResponseDto actualizarSesion(UUID sesionId, SesionUpdateDto dto, UUID tutorId) {

        Sesion sesion = sesionRepository.findById(sesionId)
            .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        // Solo el tutor dueño puede editar
        if (!sesion.getTutorId().equals(tutorId)) {
            throw new SecurityException("No tienes permiso para editar esta sesión.");
        }

        // No se puede editar una sesión cancelada
        if (ESTADO_CANCELADA.equals(sesion.getEstado())) {
            throw new IllegalArgumentException("No se puede editar una sesión cancelada.");
        }

        // Contar inscritos activos
        long inscritos = inscripcionRepository.countBySesionIdAndEstado(sesionId, INSCRIPCION_CONFIRMADA);

        // --- BLOQUEO DE FECHA si hay inscritos ---
        if (dto.getFechaHora() != null && !dto.getFechaHora().equals(sesion.getFechaHora())) {
            if (inscritos > 0) {
                throw new IllegalArgumentException(
                    "No puedes cambiar la fecha/hora porque ya hay " + inscritos +
                    " alumno(s) inscrito(s). Solo puedes modificar el título, descripción, lugar y cupo."
                );
            }
            // Validar que la nueva fecha sea futura
            if (dto.getFechaHora().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException(
                    "La nueva fecha debe ser al menos 1 hora en el futuro."
                );
            }
            sesion.setFechaHora(dto.getFechaHora());
        }

        // --- VALIDACIÓN DE CUPO ---
        if (dto.getCupoMaximo() != null) {
            if (dto.getCupoMaximo() < 1) {
                throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
            }
            if (dto.getCupoMaximo() < inscritos) {
                throw new IllegalArgumentException(
                    "El nuevo cupo (" + dto.getCupoMaximo() + ") no puede ser menor " +
                    "al número de alumnos ya inscritos (" + inscritos + ")."
                );
            }
            // Ajustar cupo disponible proporcionalmente
            int diferencia = dto.getCupoMaximo() - sesion.getCupoMaximo();
            sesion.setCupoMaximo(dto.getCupoMaximo());
            sesion.setCupoDisponible(Math.max(0, sesion.getCupoDisponible() + diferencia));
        }

        // Actualizar campos libres
        if (dto.getTitulo()      != null) sesion.setTitulo(dto.getTitulo());
        if (dto.getDescripcion() != null) sesion.setDescripcion(dto.getDescripcion());
        if (dto.getLugar()       != null) sesion.setLugar(dto.getLugar());

        return mapToResponseDto(sesionRepository.save(sesion));
    }

    // =========================================================================
    // HU-12: Cancelar sesión (borrado lógico)
    // =========================================================================

    /**
     * Cancela lógicamente una sesión del tutor autenticado.
     *
     * El estado pasa de ACTIVA → CANCELADA. La fila NO se borra de la BD.
     * Esto permite preparar la notificación a alumnos (EP-06).
     *
     * @param sesionId ID de la sesión a cancelar
     * @param tutorId  UUID del tutor autenticado (del JWT)
     */
    @Transactional
    public void cancelarSesion(UUID sesionId, UUID tutorId) {

        Sesion sesion = sesionRepository.findById(sesionId)
            .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        // Solo el dueño puede cancelar
        if (!sesion.getTutorId().equals(tutorId)) {
            throw new SecurityException("No tienes permiso para cancelar esta sesión.");
        }

        // Ya está cancelada
        if (ESTADO_CANCELADA.equals(sesion.getEstado())) {
            throw new IllegalArgumentException("La sesión ya está cancelada.");
        }

        // Borrado lógico: cambiar estado
        sesion.setEstado(ESTADO_CANCELADA);
        sesionRepository.save(sesion);

        // PREPARACIÓN EP-06: aquí se dispararía el evento de notificación.
        // Por ahora queda registrado en los logs con los datos para notificar.
        long inscritos = inscripcionRepository.countBySesionIdAndEstado(sesionId, INSCRIPCION_CONFIRMADA);
        if (inscritos > 0) {
            System.out.println("[EP-06 PENDIENTE] Sesión " + sesionId + " cancelada con " +
                inscritos + " alumno(s) inscritos. Se debe notificar por correo.");
        }
    }

    // =========================================================================
    // Método auxiliar: Entity → DTO
    // =========================================================================

    /**
     * Convierte una Entity Sesion a su DTO de respuesta.
     * Calcula el contador de inscritos en tiempo real desde la tabla inscripciones.
     */
    public SesionResponseDto mapToResponseDto(Sesion sesion) {
        int inscritos = (int) inscripcionRepository
            .countBySesionIdAndEstado(sesion.getId(), INSCRIPCION_CONFIRMADA);

        return new SesionResponseDto(
            sesion.getId(),
            sesion.getTutorId(),
            sesion.getTitulo(),
            sesion.getDescripcion(),
            sesion.getLugar(),
            sesion.getFechaHora(),
            sesion.getCupoMaximo(),
            sesion.getCupoDisponible(),
            inscritos,
            sesion.getEstado(),
            sesion.getCreadoEn()
        );
    }
}
