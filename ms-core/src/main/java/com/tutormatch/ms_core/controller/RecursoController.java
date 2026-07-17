package com.tutormatch.ms_core.controller;

import com.tutormatch.ms_core.dto.RecursoRequestDto;
import com.tutormatch.ms_core.dto.RecursoResponseDto;
import com.tutormatch.ms_core.service.RecursoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/core/recursos")
public class RecursoController {

    private final RecursoService recursoService;

    public RecursoController(RecursoService recursoService) {
        this.recursoService = recursoService;
    }

    /**
     * POST /api/core/recursos/sesion/{sesionId}
     *
     * Épica 7: Permite que un Tutor agregue un recurso (material) a una sesión.
     *
     * Seguridad: Solo usuarios con rol TUTOR pueden llamar este endpoint.
     *
     * @param sesionId ID de la sesión (path variable)
     * @param dto      Body JSON con titulo y url
     * @return         201 Created con los datos del recurso creado
     */
    @PostMapping("/sesion/{sesionId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<RecursoResponseDto> agregarRecurso(
            @PathVariable UUID sesionId,
            @RequestBody RecursoRequestDto dto) {

        RecursoResponseDto recursoCreado = recursoService.agregarRecurso(sesionId, dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(recursoCreado);
    }

    /**
     * GET /api/core/recursos/sesion/{sesionId}
     *
     * Épica 7: Obtiene todos los recursos de una sesión.
     * Público para cualquier usuario autenticado.
     *
     * @param sesionId ID de la sesión (path variable)
     * @return         200 OK con la lista de recursos
     */
    @GetMapping("/sesion/{sesionId}")
    public ResponseEntity<List<RecursoResponseDto>> obtenerRecursosPorSesion(
            @PathVariable UUID sesionId) {

        List<RecursoResponseDto> recursos = recursoService.obtenerRecursosPorSesion(sesionId);

        return ResponseEntity.ok(recursos);
    }

    /**
     * DELETE /api/core/recursos/{recursoId}
     *
     * Épica 7: Permite que un Tutor elimine un recurso.
     *
     * Seguridad: Solo usuarios con rol TUTOR pueden llamar este endpoint.
     *
     * @param recursoId ID del recurso a eliminar (path variable)
     * @return          204 No Content
     */
    @DeleteMapping("/{recursoId}")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<Void> eliminarRecurso(@PathVariable UUID recursoId) {

        recursoService.eliminarRecurso(recursoId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Manejo de errores de validación de negocio.
     * Cuando el Service lanza IllegalArgumentException, devolvemos 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
