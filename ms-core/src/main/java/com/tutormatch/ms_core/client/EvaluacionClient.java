package com.tutormatch.ms_core.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "ms-evaluaciones", url = "http://localhost:8080")
public interface EvaluacionClient {

    @PostMapping("/api/evaluaciones/promedios-lote")
    Map<UUID, Double> obtenerPromediosLote(@RequestBody List<UUID> tutorIds);

    @GetMapping("/api/evaluaciones/alumno/{alumnoId}/evaluadas")
    List<UUID> obtenerSesionesEvaluadas(@PathVariable("alumnoId") UUID alumnoId);
}
