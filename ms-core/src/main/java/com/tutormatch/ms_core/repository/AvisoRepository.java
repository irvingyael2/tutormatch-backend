package com.tutormatch.ms_core.repository;

import com.tutormatch.ms_core.entity.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvisoRepository extends JpaRepository<Aviso, UUID> {

    List<Aviso> findAllByOrderByCreadoEnDesc();
}
