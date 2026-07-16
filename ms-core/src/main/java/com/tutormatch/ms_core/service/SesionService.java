package com.tutormatch.ms_core.service;

import com.tutormatch.ms_core.dto.SesionRequestDto;
import com.tutormatch.ms_core.dto.SesionResponseDto;
import com.tutormatch.ms_core.entity.Sesion;
import com.tutormatch.ms_core.repository.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SesionService {

    private final SesionRepository sesionRepository;

    public SesionService(SesionRepository sesionRepository) {
        this.sesionRepository = sesionRepository;
    }

    /**
     * HU-09: Publica una nueva sesión de tutoría.
     *
     * Validaciones de negocio antes de guardar:
     * 1. La fecha de la sesión debe ser en el FUTURO (mínimo 1 hora desde ahora).
     * 2. El cupo máximo debe ser al menos 1.
     * 3. El tutor NO puede tener otra sesión en las 2 horas antes o después de la nueva fecha (cruce de horarios).
     *
     * @param dto      Datos enviados por el tutor desde el frontend
     * @param tutorId  UUID extraído del token JWT por el controlador
     * @return         DTO con los datos de la sesión recién creada (id, fechaHora, cupos, etc.)
     */
    @Transactional
    public SesionResponseDto publicarSesion(SesionRequestDto dto, UUID tutorId) {

        // --- VALIDACIÓN 1: La fecha debe ser al menos 1 hora en el futuro ---
        LocalDateTime limiteMinimo = LocalDateTime.now().plusHours(1);
        if (dto.getFechaHora() == null || dto.getFechaHora().isBefore(limiteMinimo)) {
            throw new IllegalArgumentException(
                "La fecha de la sesión debe ser al menos 1 hora en el futuro."
            );
        }

        // --- VALIDACIÓN 2: El cupo máximo debe ser positivo ---
        if (dto.getCupoMaximo() == null || dto.getCupoMaximo() < 1) {
            throw new IllegalArgumentException(
                "El cupo máximo debe ser al menos 1 alumno."
            );
        }

        // --- VALIDACIÓN 3: Cruce de horarios (ventana de ±2 horas) ---
        // Buscamos todas las sesiones futuras del tutor y verificamos solapamiento
        List<Sesion> sesionesFuturas = sesionRepository.findByTutorIdAndFechaHoraAfter(
            tutorId, LocalDateTime.now()
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

        // --- GUARDAR EN BASE DE DATOS ---
        Sesion nuevaSesion = new Sesion();
        nuevaSesion.setTutorId(tutorId);
        nuevaSesion.setTitulo(dto.getTitulo());
        nuevaSesion.setDescripcion(dto.getDescripcion());
        nuevaSesion.setFechaHora(dto.getFechaHora());
        nuevaSesion.setCupoMaximo(dto.getCupoMaximo());
        nuevaSesion.setCupoDisponible(dto.getCupoMaximo()); // Al crear, disponible = máximo

        Sesion sesionGuardada = sesionRepository.save(nuevaSesion);

        // --- TRANSFORMAR Entity → DTO de Respuesta ---
        return mapToResponseDto(sesionGuardada);
    }

    public SesionResponseDto obtenerSesion(UUID id) {
        Sesion sesion = sesionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        return mapToResponseDto(sesion);
    }

    /**
     * Convierte una Entity Sesion a su DTO de respuesta.
     * Método reutilizable para otros endpoints futuros (HU-10, HU-11).
     */
    public SesionResponseDto mapToResponseDto(Sesion sesion) {
        return new SesionResponseDto(
            sesion.getId(),
            sesion.getTutorId(),
            sesion.getTitulo(),
            sesion.getDescripcion(),
            sesion.getFechaHora(),
            sesion.getCupoMaximo(),
            sesion.getCupoDisponible(),
            sesion.getCreadoEn()
        );
    }
}
