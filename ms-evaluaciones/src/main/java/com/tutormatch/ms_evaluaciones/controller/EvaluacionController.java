package com.tutormatch.ms_evaluaciones.controller;

import com.tutormatch.ms_evaluaciones.dto.EvaluacionRequestDto;
import com.tutormatch.ms_evaluaciones.dto.EvaluacionResponseDto;
import com.tutormatch.ms_evaluaciones.service.EvaluacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    public EvaluacionController(EvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ALUMNO')")
    public ResponseEntity<EvaluacionResponseDto> crearEvaluacion(
            @RequestBody EvaluacionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        UUID alumnoId;
        try {
            String userIdStr = jwt.getClaimAsString("usuario_id");
            if (userIdStr == null) userIdStr = jwt.getSubject();
            alumnoId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Token inválido: No se puede extraer el ID del usuario.");
        }
        
        EvaluacionResponseDto response = evaluacionService.crearEvaluacion(dto, alumnoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/promedio/{tutorId}")
    public ResponseEntity<Double> obtenerPromedioTutor(@PathVariable UUID tutorId) {
        Double promedio = evaluacionService.obtenerPromedioTutor(tutorId);
        return ResponseEntity.ok(promedio);
    }

    @PostMapping("/promedios-lote")
    public ResponseEntity<java.util.Map<UUID, Double>> obtenerPromediosLote(@RequestBody java.util.List<UUID> tutorIds) {
        return ResponseEntity.ok(evaluacionService.obtenerPromediosPorTutorIds(tutorIds));
    }

    @GetMapping("/alumno/{alumnoId}/evaluadas")
    public ResponseEntity<java.util.List<UUID>> obtenerSesionesEvaluadas(@PathVariable UUID alumnoId) {
        return ResponseEntity.ok(evaluacionService.obtenerSesionesEvaluadasPorAlumno(alumnoId));
    }

             @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralError(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error procesando la solicitud: " + ex.getMessage());
    }
}
