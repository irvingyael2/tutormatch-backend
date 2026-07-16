package com.tutormatch.ms_evaluaciones.repository;

import com.tutormatch.ms_evaluaciones.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

    @Query("SELECT AVG(e.calificacion) FROM Evaluacion e WHERE e.tutorId = :tutorId")
    Double obtenerPromedioPorTutorId(@Param("tutorId") UUID tutorId);
}
