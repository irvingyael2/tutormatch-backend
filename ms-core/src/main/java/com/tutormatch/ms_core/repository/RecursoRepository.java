package com.tutormatch.ms_core.repository;

import com.tutormatch.ms_core.entity.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, UUID> {
    
    // Método mágico de Spring Data para traer los recursos de una sesión específica
    List<Recurso> findBySesionId(UUID sesionId);
}