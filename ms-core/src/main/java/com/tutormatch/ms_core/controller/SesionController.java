package com.tutormatch.ms_core.controller;

import com.tutormatch.ms_core.dto.SesionRequestDto;
import com.tutormatch.ms_core.dto.SesionResponseDto;
import com.tutormatch.ms_core.service.SesionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/core/sesiones-tutorias")
public class SesionController {

    private final SesionService sesionService;

    public SesionController(SesionService sesionService) {
        this.sesionService = sesionService;
    }

    /**
     * POST /api/core/sesiones-tutorias
     *
     * HU-09: Permite que un Tutor publique una nueva sesión de tutoría.
     *
     * Seguridad: Solo usuarios con rol ROLE_TUTOR pueden llamar este endpoint.
     * El tutorId se extrae automáticamente del token JWT (claim "sub"),
     * NO se recibe desde el body para evitar suplantación.
     *
     * @param dto  Body JSON con titulo, descripcion, fechaHora, cupoMaximo
     * @param jwt  Token JWT inyectado automáticamente por Spring Security
     * @return     201 Created con los datos de la sesión publicada
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_TUTOR')")
    public ResponseEntity<SesionResponseDto> publicarSesion(
            @RequestBody SesionRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {

        // Extraemos el ID del tutor del JWT (claim "sub" contiene el UUID del usuario)
        UUID tutorId = UUID.fromString(jwt.getSubject());

        SesionResponseDto sesionCreada = sesionService.publicarSesion(dto, tutorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(sesionCreada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SesionResponseDto> obtenerSesion(@PathVariable UUID id) {
        SesionResponseDto sesion = sesionService.obtenerSesion(id);
        return ResponseEntity.ok(sesion);
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
