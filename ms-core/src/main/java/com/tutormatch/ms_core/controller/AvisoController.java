package com.tutormatch.ms_core.controller;

import com.tutormatch.ms_core.dto.AvisoRequestDto;
import com.tutormatch.ms_core.dto.AvisoResponseDto;
import com.tutormatch.ms_core.service.AvisoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/core/avisos")
public class AvisoController {

    private final AvisoService avisoService;

    public AvisoController(AvisoService avisoService) {
        this.avisoService = avisoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AvisoResponseDto> crearAviso(@RequestBody AvisoRequestDto dto) {
        AvisoResponseDto avisoCreado = avisoService.crearAviso(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(avisoCreado);
    }

    @GetMapping
    public ResponseEntity<List<AvisoResponseDto>> listarAvisos() {
        return ResponseEntity.ok(avisoService.listarAvisos());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AvisoResponseDto> actualizarAviso(
            @PathVariable UUID id,
            @RequestBody AvisoRequestDto dto) {
        return ResponseEntity.ok(avisoService.actualizarAviso(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> eliminarAviso(@PathVariable UUID id) {
        avisoService.eliminarAviso(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
