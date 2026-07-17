package com.tutormatch.ms_core.controller;

import com.tutormatch.ms_core.dto.PreguntaRequestDto;
import com.tutormatch.ms_core.dto.PreguntaResponseDto;
import com.tutormatch.ms_core.dto.RespuestaRequestDto;
import com.tutormatch.ms_core.service.PreguntaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/core/sesiones-tutorias")
public class PreguntaController {

    private final PreguntaService preguntaService;

    public PreguntaController(PreguntaService preguntaService) {
        this.preguntaService = preguntaService;
    }

    @PostMapping("/{sesionId}/preguntas")
    @PreAuthorize("hasRole('ROLE_ALUMNO')")
    public ResponseEntity<PreguntaResponseDto> crearPregunta(
            @PathVariable UUID sesionId,
            @RequestBody PreguntaRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        UUID alumnoId = UUID.fromString(jwt.getClaimAsString("usuario_id"));
        String nombre = jwt.getClaimAsString("nombre");

        PreguntaResponseDto creada = preguntaService.crearPregunta(sesionId, alumnoId, nombre, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PostMapping("/{sesionId}/preguntas/{preguntaId}/respuesta")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<PreguntaResponseDto> responderPregunta(
            @PathVariable UUID sesionId,
            @PathVariable UUID preguntaId,
            @RequestBody RespuestaRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = UUID.fromString(jwt.getClaimAsString("usuario_id"));
        PreguntaResponseDto actualizada = preguntaService.responderPregunta(sesionId, preguntaId, tutorId, dto);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{sesionId}/preguntas/{preguntaId}")
    @PreAuthorize("hasRole('ROLE_ALUMNO')")
    public ResponseEntity<Void> eliminarPregunta(
            @PathVariable UUID sesionId,
            @PathVariable UUID preguntaId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID alumnoId = UUID.fromString(jwt.getClaimAsString("usuario_id"));
        preguntaService.eliminarPregunta(sesionId, preguntaId, alumnoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sesionId}/preguntas")
    public ResponseEntity<List<PreguntaResponseDto>> listarPreguntas(@PathVariable UUID sesionId) {
        return ResponseEntity.ok(preguntaService.listarPreguntas(sesionId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}