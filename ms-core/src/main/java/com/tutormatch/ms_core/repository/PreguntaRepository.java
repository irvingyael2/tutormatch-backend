package com.tutormatch.ms_core.repository;

import com.tutormatch.ms_core.entity.SesionPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PreguntaRepository extends JpaRepository<SesionPregunta, UUID> {

    List<SesionPregunta> findBySesionIdOrderByCreadoEnAsc(UUID sesionId);

    Optional<SesionPregunta> findByIdAndSesionId(UUID id, UUID sesionId);
}
