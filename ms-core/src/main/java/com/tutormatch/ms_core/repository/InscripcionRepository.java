package com.tutormatch.ms_core.repository;

import com.tutormatch.ms_core.entity.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {

    /**
     * Cuenta cuántos alumnos tienen inscripción CONFIRMADA en una sesión.
     * Se usa en HU-10 (mostrar contador) y HU-11 (bloquear fecha si inscritos > 0).
     *
     * @param sesionId ID de la sesión
     * @param estado   Estado de la inscripción (pasar "CONFIRMADA")
     * @return número de inscripciones activas
     */
    long countBySesionIdAndEstado(UUID sesionId, String estado);
}
