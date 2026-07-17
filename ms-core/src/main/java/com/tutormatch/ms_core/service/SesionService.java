package com.tutormatch.ms_core.service;

import com.tutormatch.ms_core.client.EvaluacionClient;
import com.tutormatch.ms_core.client.NotificacionClient;
import com.tutormatch.ms_core.client.UsuarioClient;
import com.tutormatch.ms_core.dto.*;
import com.tutormatch.ms_core.entity.Inscripcion;
import com.tutormatch.ms_core.entity.Sesion;
import com.tutormatch.ms_core.repository.InscripcionRepository;
import com.tutormatch.ms_core.repository.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SesionService {

    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final String INSCRIPCION_CONFIRMADA = "CONFIRMADA";

    private final SesionRepository sesionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final NotificacionClient notificacionClient;
    private final UsuarioClient usuarioClient;
    private final EvaluacionClient evaluacionClient;

    public SesionService(SesionRepository sesionRepository,
            InscripcionRepository inscripcionRepository, NotificacionClient notificacionClient,
            UsuarioClient usuarioClient, EvaluacionClient evaluacionClient) {
        this.sesionRepository = sesionRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.notificacionClient = notificacionClient;
        this.usuarioClient = usuarioClient;
        this.evaluacionClient = evaluacionClient;
    }

    // =========================================================================
    // HU-09: Publicar nueva sesión
    // =========================================================================

    @Transactional
    public SesionResponseDto publicarSesion(SesionRequestDto dto, UUID tutorId, String tutorNombre) {

        // --- VALIDACIÓN 1: Fecha futura (mínimo 1 hora) ---
        LocalDateTime limiteMinimo = LocalDateTime.now().plusHours(1);
        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(limiteMinimo)) {
            throw new IllegalArgumentException(
                    "La fecha de la sesión debe ser al menos 1 hora en el futuro.");
        }

        // --- VALIDACIÓN 2: Cupo positivo ---
        if (dto.getCupoMaximo() == null || dto.getCupoMaximo() < 1) {
            throw new IllegalArgumentException("El cupo máximo debe ser al menos 1 alumno.");
        }

        // --- VALIDACIÓN 3: Cruce de horarios con sesiones ACTIVAS (ventana ±2h) ---
        List<Sesion> sesionesFuturas = sesionRepository.findByTutorIdAndEstadoAndFechaHoraAfter(
                tutorId, ESTADO_ACTIVA, LocalDateTime.now());

        LocalDateTime nuevaFecha = dto.getFechaHora();
        for (Sesion existente : sesionesFuturas) {
            LocalDateTime inicio = existente.getFechaHora().minusHours(2);
            LocalDateTime fin = existente.getFechaHora().plusHours(2);
            if (nuevaFecha.isAfter(inicio) && nuevaFecha.isBefore(fin)) {
                throw new IllegalArgumentException(
                        "Ya tienes una sesión programada cerca de ese horario (" +
                                existente.getFechaHora() + "). Debe haber al menos 2 horas de diferencia.");
            }
        }

        // --- GUARDAR ---
        Sesion nueva = new Sesion();
        nueva.setTutorId(tutorId);
        nueva.setTutorNombre(tutorNombre != null ? tutorNombre : "Tutor");
        nueva.setTitulo(dto.getTitulo());
        nueva.setDescripcion(dto.getDescripcion());
        nueva.setLugar(dto.getLugar());
        nueva.setFechaHora(dto.getFechaHora());
        nueva.setCupoMaximo(dto.getCupoMaximo());
        nueva.setCupoDisponible(dto.getCupoMaximo());
        nueva.setEstado(ESTADO_ACTIVA);

        return mapToResponseDto(sesionRepository.save(nueva));
    }

    public SesionResponseDto obtenerSesion(UUID id) {
        Sesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        return mapToResponseDto(sesion);
    }

    // =========================================================================
    // HU-10: Agenda del Tutor — sesiones futuras ordenadas
    // =========================================================================

    public List<SesionResponseDto> obtenerAgendaTutor(UUID tutorId) {
        return sesionRepository
                .findByTutorIdAndEstadoAndFechaHoraAfterOrderByFechaHoraAsc(
                        tutorId, ESTADO_ACTIVA, LocalDateTime.now())
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // HU-11: Editar sesión
    // =========================================================================

    @Transactional
    public SesionResponseDto actualizarSesion(UUID sesionId, SesionUpdateDto dto, UUID tutorId) {

        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        if (!sesion.getTutorId().equals(tutorId)) {
            throw new SecurityException("No tienes permiso para editar esta sesión.");
        }
        if (ESTADO_CANCELADA.equals(sesion.getEstado())) {
            throw new IllegalArgumentException("No se puede editar una sesión cancelada.");
        }

        long inscritos = inscripcionRepository.countBySesionIdAndEstado(sesionId, INSCRIPCION_CONFIRMADA);

        if (dto.getFechaHora() != null && !dto.getFechaHora().equals(sesion.getFechaHora())) {
            if (inscritos > 0) {
                throw new IllegalArgumentException(
                        "No puedes cambiar la fecha/hora porque ya hay " + inscritos + " alumno(s) inscrito(s).");
            }
            if (dto.getFechaHora().isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException("La nueva fecha debe ser al menos 1 hora en el futuro.");
            }
            sesion.setFechaHora(dto.getFechaHora());
        }

        if (dto.getCupoMaximo() != null) {
            if (dto.getCupoMaximo() < 1) {
                throw new IllegalArgumentException("El cupo máximo debe ser al menos 1.");
            }
            if (dto.getCupoMaximo() < inscritos) {
                throw new IllegalArgumentException(
                        "El nuevo cupo (" + dto.getCupoMaximo() + ") no puede ser menor " +
                                "al número de alumnos ya inscritos (" + inscritos + ").");
            }
            int diferencia = dto.getCupoMaximo() - sesion.getCupoMaximo();
            sesion.setCupoMaximo(dto.getCupoMaximo());
            sesion.setCupoDisponible(Math.max(0, sesion.getCupoDisponible() + diferencia));
        }

        if (dto.getTitulo() != null)
            sesion.setTitulo(dto.getTitulo());
        if (dto.getDescripcion() != null)
            sesion.setDescripcion(dto.getDescripcion());
        if (dto.getLugar() != null)
            sesion.setLugar(dto.getLugar());

        return mapToResponseDto(sesionRepository.save(sesion));
    }

    // =========================================================================
    // HU-12: Cancelar sesión (borrado lógico)
    // =========================================================================

    @Transactional
    public void cancelarSesion(UUID sesionId, UUID tutorId) {
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada."));

        if (!sesion.getTutorId().equals(tutorId)) {
            throw new SecurityException("No tienes permiso para cancelar esta sesión.");
        }
        if (ESTADO_CANCELADA.equals(sesion.getEstado())) {
            throw new IllegalArgumentException("La sesión ya está cancelada.");
        }

        // 1. Buscamos todas las inscripciones a esta clase
        List<Inscripcion> inscripciones = inscripcionRepository.findBySesionId(sesionId);

        String fechaFormateada = sesion.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // 2. Avisamos a cada alumno
        for (Inscripcion inscripcion : inscripciones) {

            // ¡MAGIA!: Le preguntamos a ms-usuarios el correo de este UUID
            UsuarioResponseDto alumno = usuarioClient.obtenerUsuarioPorId(inscripcion.getAlumnoId());

            String mensajeHtml = String.format(
                    "Lamentamos informarte que la sesión de <strong style='color: #ef4444;'>%s</strong> ha sido <strong>CANCELADA</strong> por el tutor.<br><br>"
                            +
                            "<strong>Detalles de la sesión cancelada:</strong><br>" +
                            "<ul>" +
                            "<li><strong>Fecha original:</strong> %s</li>" +
                            "</ul>" +
                            "Entendemos que esto altera tus planes. Te invitamos a revisar el catálogo para encontrar otro horario.",
                    sesion.getTitulo(),
                    fechaFormateada);

            NotificacionRequestDto notificacion = new NotificacionRequestDto();
            notificacion.setUsuarioId(inscripcion.getAlumnoId());
            notificacion.setCorreoDestino(alumno.getEmail());
            notificacion.setTitulo("Aviso: Tutoría Cancelada (" + sesion.getTitulo() + ")");
            notificacion.setMensaje(mensajeHtml);

            notificacionClient.enviarNotificacion(notificacion);
        }

        sesion.setEstado(ESTADO_CANCELADA);
        sesionRepository.save(sesion);

        long inscritos = inscripcionRepository.countBySesionIdAndEstado(sesionId, INSCRIPCION_CONFIRMADA);
        if (inscritos > 0) {
            System.out.println("[EP-06 PENDIENTE] Sesión " + sesionId + " cancelada con " +
                    inscritos + " alumno(s) inscritos. Se debe notificar por correo.");
        }
    }

    // =========================================================================
    // HU-13: Catálogo público con filtros opcionales
    // =========================================================================
    /**
     * Retorna el catálogo de sesiones disponibles para el público.
     * Los parámetros son opcionales (null = sin filtro).
     * IMPORTANTE: El DTO resultante (CatalogoSesionDto) NO incluye el campo
     * "lugar".
     *
     * @param materia Texto parcial para buscar en el título de la sesión
     * @param tutor   Texto parcial para buscar en el nombre del tutor
     * @param fecha   Fecha exacta (yyyy-MM-dd) para filtrar
     */
    public List<CatalogoSesionDto> getCatalogo(String materia, String tutor, LocalDate fecha) {
        String fechaStr = (fecha != null) ? fecha.toString() : null;
        List<Sesion> sesiones = sesionRepository.findCatalogo(LocalDateTime.now(), materia, tutor, fechaStr);
        
        List<UUID> tutorIds = sesiones.stream().map(Sesion::getTutorId).distinct().collect(Collectors.toList());
        Map<UUID, Double> promedios = evaluacionClient.obtenerPromediosLote(tutorIds);
        
        return sesiones.stream()
                .map(s -> mapToCatalogoDto(s, promedios.get(s.getTutorId())))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // HU-Historial (Epica 5): Sesiones pasadas del usuario
    // =========================================================================

    /**
     * Devuelve el historial de sesiones pasadas de un usuario (tutor o alumno).
     *
     * @param usuarioId UUID del usuario extraído del JWT
     * @return          Lista de DTOs de sesiones pasadas del usuario
     */
    @Transactional(readOnly = true)
    public List<SesionResponseDto> obtenerHistorial(UUID usuarioId) {
        List<Sesion> sesiones = sesionRepository.findHistorialByUsuarioId(usuarioId, LocalDateTime.now());
        
        List<UUID> tutorIds = sesiones.stream().map(Sesion::getTutorId).distinct().collect(Collectors.toList());
        Map<UUID, Double> promedios = evaluacionClient.obtenerPromediosLote(tutorIds);
        List<UUID> evaluadas = evaluacionClient.obtenerSesionesEvaluadas(usuarioId);
        
        return sesiones.stream()
            .map(s -> mapToResponseDto(s, promedios.get(s.getTutorId()), evaluadas.contains(s.getId())))
            .collect(Collectors.toList());
    }

    // =========================================================================
    // Métodos auxiliares: Entity -> DTO
    // =========================================================================

    public SesionResponseDto mapToResponseDto(Sesion sesion) {
        return mapToResponseDto(sesion, null, null);
    }
    
    public SesionResponseDto mapToResponseDto(Sesion sesion, Double promedioTutor, Boolean fueEvaluada) {
        int inscritos = (int) inscripcionRepository
                .countBySesionIdAndEstado(sesion.getId(), INSCRIPCION_CONFIRMADA);

        return new SesionResponseDto(
                sesion.getId(),
                sesion.getTutorId(),
                sesion.getTutorNombre(),
                sesion.getTitulo(),
                sesion.getDescripcion(),
                sesion.getLugar(),
                sesion.getFechaHora(),
                sesion.getCupoMaximo(),
                sesion.getCupoDisponible(),
                inscritos,
                sesion.getEstado(),
                sesion.getCreadoEn(),
                fueEvaluada,
                promedioTutor);
    }

   private CatalogoSesionDto mapToCatalogoDto(Sesion sesion, Double calificacion) {
        int inscritos = (int) inscripcionRepository
                .countBySesionIdAndEstado(sesion.getId(), INSCRIPCION_CONFIRMADA);

        return new CatalogoSesionDto(
                sesion.getId(),
                sesion.getTutorNombre(),
                sesion.getTitulo(),
                sesion.getDescripcion(),
                // lugar NO incluido en catálogo (seguridad HU-13)
                sesion.getFechaHora(),
                sesion.getCupoMaximo(),
                sesion.getCupoDisponible(),
                inscritos,
                calificacion
        );
    }

    /**
     * Búsqueda de sesión por ID con acceso público (para InscripcionService).
     */
    public Sesion findById(UUID id) {
        return sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada: " + id));
    }
}
