package com.tutormatch.ms_core.controller;

import com.tutormatch.ms_core.dto.SesionRequestDto;
import com.tutormatch.ms_core.dto.SesionResponseDto;
import com.tutormatch.ms_core.dto.SesionUpdateDto;
import com.tutormatch.ms_core.service.SesionService;
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
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    // -----------------------------------------------------------------------
    // HU-09: POST — Publicar nueva sesión de tutoría
    // -----------------------------------------------------------------------

    /**
     * POST /api/core/sesiones-tutorias
     * Solo accesible por ROLE_TUTOR.
     * El tutorId se extrae del JWT, no del body (evita suplantación).
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<SesionResponseDto> publicarSesion(
            @RequestBody SesionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = UUID.fromString(jwt.getSubject());
        SesionResponseDto sesionCreada = sesionService.publicarSesion(dto, tutorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(sesionCreada);
    }

    // -----------------------------------------------------------------------
    // HU-10: GET — Obtener "Mi Agenda" (sesiones futuras del tutor ordenadas)
    // -----------------------------------------------------------------------

    /**
     * GET /api/core/sesiones-tutorias/mi-agenda
     * Retorna las sesiones ACTIVAS futuras del tutor autenticado,
     * ordenadas cronológicamente (la más próxima primero).
     */
    @GetMapping("/mi-agenda")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<List<SesionResponseDto>> obtenerMiAgenda(
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = UUID.fromString(jwt.getSubject());
        List<SesionResponseDto> agenda = sesionService.obtenerAgendaTutor(tutorId);
        return ResponseEntity.ok(agenda);
    }

    // -----------------------------------------------------------------------
    // HU-11: PUT — Editar una sesión existente
    // -----------------------------------------------------------------------

    /**
     * PUT /api/core/sesiones-tutorias/{id}
     * Actualiza los datos de una sesión del tutor autenticado.
     * Si la sesión tiene alumnos inscritos, el campo fechaHora estará bloqueado.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<SesionResponseDto> actualizarSesion(
            @PathVariable UUID id,
            @RequestBody SesionUpdateDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = UUID.fromString(jwt.getSubject());
        SesionResponseDto sesionActualizada = sesionService.actualizarSesion(id, dto, tutorId);
        return ResponseEntity.ok(sesionActualizada);
    }

    // -----------------------------------------------------------------------
    // HU-12: DELETE — Cancelar sesión (borrado lógico)
    // -----------------------------------------------------------------------

    /**
     * DELETE /api/core/sesiones-tutorias/{id}
     * Cambia el estado de la sesión a CANCELADA (no elimina la fila).
     * La sesión desaparece de la agenda y del catálogo general.
     * Deja un log preparado para la notificación a inscritos (EP-06).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<Void> cancelarSesion(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = UUID.fromString(jwt.getSubject());
        sesionService.cancelarSesion(id, tutorId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // -----------------------------------------------------------------------
    // Manejo de errores de negocio → 400 Bad Request
    // -----------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityError(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
