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
     * Busca todas las sesiones futuras de un tutor específico.
     * Útil para la validación de cruce de horarios y para HU-10 (Mi Agenda).
     *
     * @param tutorId ID del tutor extraído del JWT
     * @param fecha   Momento de referencia (normalmente LocalDateTime.now())
     * @return Lista de sesiones del tutor con fechaHora posterior a 'fecha'
     */
    List<Sesion> findByTutorIdAndFechaHoraAfter(UUID tutorId, LocalDateTime fecha);
}
