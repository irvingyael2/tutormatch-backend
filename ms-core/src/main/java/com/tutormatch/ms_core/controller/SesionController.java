package com.tutormatch.ms_core.controller;

import com.tutormatch.ms_core.dto.CatalogoSesionDto;
import com.tutormatch.ms_core.dto.SesionRequestDto;
import com.tutormatch.ms_core.dto.SesionResponseDto;
import com.tutormatch.ms_core.dto.SesionUpdateDto;
import com.tutormatch.ms_core.service.SesionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/core/sesiones-tutorias")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    private UUID extraerUsuarioId(Jwt jwt) {
        try {
            String userIdStr = jwt.getClaimAsString("usuario_id");
            if (userIdStr == null) userIdStr = jwt.getSubject();
            return UUID.fromString(userIdStr);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Token inválido: No se puede extraer el ID del usuario.");
        }
    }

    // -----------------------------------------------------------------------
    // HU-13: GET — Catálogo público (sin autenticación requerida)
    // -----------------------------------------------------------------------

    /**
     * GET /api/core/sesiones-tutorias/catalogo
     * Endpoint público. Retorna sesiones ACTIVAS, futuras y con cupo > 0.
     * Acepta filtros opcionales por query param. El campo "lugar" NO se incluye.
     *
     * @param materia Texto para buscar en el título de la sesión
     * @param tutor   Texto para buscar en el nombre del tutor
     * @param fecha   Fecha exacta (formato yyyy-MM-dd)
     */
    @GetMapping("/catalogo")
    public ResponseEntity<List<CatalogoSesionDto>> getCatalogo(
            @RequestParam(required = false) String materia,
            @RequestParam(required = false) String tutor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        return ResponseEntity.ok(sesionService.getCatalogo(materia, tutor, fecha));
    }

    // -----------------------------------------------------------------------
    // HU-09: POST — Publicar nueva sesión de tutoría
    // -----------------------------------------------------------------------

    /**
     * POST /api/core/sesiones-tutorias
     * Solo accesible por ROLE_TUTOR.
     * tutorId y tutorNombre se extraen del JWT (evita suplantación).
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<SesionResponseDto> publicarSesion(
            @RequestBody SesionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = extraerUsuarioId(jwt);
        String tutorNombre = jwt.getClaimAsString("nombre");

        SesionResponseDto sesionCreada = sesionService.publicarSesion(dto, tutorId, tutorNombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(sesionCreada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SesionResponseDto> obtenerSesion(@PathVariable UUID id) {
        SesionResponseDto sesion = sesionService.obtenerSesion(id);
        return ResponseEntity.ok(sesion);
    }

    // -----------------------------------------------------------------------
    // HU-10: GET — Agenda del Tutor
    // -----------------------------------------------------------------------

    @GetMapping("/mi-agenda")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<List<SesionResponseDto>> obtenerMiAgenda(
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = extraerUsuarioId(jwt);
        return ResponseEntity.ok(sesionService.obtenerAgendaTutor(tutorId));
    }

    // -----------------------------------------------------------------------
    // HU-11: PUT — Editar sesión
    // -----------------------------------------------------------------------

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<SesionResponseDto> actualizarSesion(
            @PathVariable UUID id,
            @RequestBody SesionUpdateDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = extraerUsuarioId(jwt);
        return ResponseEntity.ok(sesionService.actualizarSesion(id, dto, tutorId));
    }

    // -----------------------------------------------------------------------
    // HU-12: DELETE — Cancelar sesión (borrado lógico)
    // -----------------------------------------------------------------------

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<Void> cancelarSesion(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID tutorId = extraerUsuarioId(jwt);
        sesionService.cancelarSesion(id, tutorId);
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------------------------------
    // HU-Historial (Epica 5): GET — Historial de sesiones pasadas del usuario
    // -----------------------------------------------------------------------

    /**
     * GET /api/core/sesiones-tutorias/historial
     *
     * Devuelve el historial de sesiones pasadas del usuario logueado,
     * ordenadas de más reciente a más antigua.
     *
     * Seguridad: Usuarios con rol ROLE_TUTOR o ROLE_ALUMNO pueden llamar este endpoint.
     * El usuarioId se extrae automáticamente del token JWT (claim "usuario_id").
     *
     * @param jwt Token JWT inyectado automáticamente por Spring Security
     * @return    200 OK con la lista de sesiones pasadas del usuario
     */
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('ROLE_TUTOR', 'ROLE_ALUMNO')")
    public ResponseEntity<List<SesionResponseDto>> obtenerHistorial(
            @AuthenticationPrincipal Jwt jwt) {

        UUID usuarioId = UUID.fromString(jwt.getClaimAsString("usuario_id"));
        return ResponseEntity.ok(sesionService.obtenerHistorial(usuarioId));
    }

    // -----------------------------------------------------------------------
    // Manejo de errores
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
