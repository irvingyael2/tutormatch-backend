package com.tutormatch.ms_core.repository;

import com.tutormatch.ms_core.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, UUID> {

    /**
     * HU-09/Validación: Busca sesiones ACTIVAS futuras de un tutor para detectar cruce de horarios.
     *
     * @param tutorId ID del tutor del JWT
     * @param estado  Estado de la sesión (pasar "ACTIVA")
     * @param fecha   Momento de referencia (LocalDateTime.now())
     * @return Lista de sesiones activas futuras del tutor
     */
    List<Sesion> findByTutorIdAndEstadoAndFechaHoraAfter(UUID tutorId, String estado, LocalDateTime fecha);

    /**
     * HU-10: Retorna las sesiones ACTIVAS futuras del tutor ordenadas cronológicamente
     * (la más próxima primero), para mostrar en "Mi Agenda".
     *
     * @param tutorId ID del tutor del JWT
     * @param estado  Estado (pasar "ACTIVA")
     * @param fecha   Fecha de corte (LocalDateTime.now())
     * @return Lista ordenada por fechaHora ascendente
     */
    List<Sesion> findByTutorIdAndEstadoAndFechaHoraAfterOrderByFechaHoraAsc(
            UUID tutorId, String estado, LocalDateTime fecha);
}
