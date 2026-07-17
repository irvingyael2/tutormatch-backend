package com.tutormatch.ms_usuarios.controller;

import com.tutormatch.ms_usuarios.dto.RegistroDto;
import com.tutormatch.ms_usuarios.dto.SolicitudPendienteDto;
import com.tutormatch.ms_usuarios.dto.SolicitudTutorRequestDto;
import com.tutormatch.ms_usuarios.dto.UsuarioPerfilDto;
import com.tutormatch.ms_usuarios.entity.Usuario;
import com.tutormatch.ms_usuarios.service.UsuarioService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    // Endpoint para registrar un nuevo usuario
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody RegistroDto dto) {
        try {
            service.registrar(dto);
            return ResponseEntity.ok("Usuario registrado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // HU-05: el Alumno autenticado envía su justificación para solicitar ser Tutor
    @PatchMapping("/solicitud-tutor")
    public ResponseEntity<?> solicitarTutor(
            @RequestBody SolicitudTutorRequestDto dto,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            UUID usuarioId = UUID.fromString(jwt.getClaimAsString("usuario_id"));
            service.solicitarTutor(usuarioId, dto.getJustificacion());
            return ResponseEntity.ok("Solicitud enviada correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // HU-05/HU-08: perfil resumido del usuario autenticado (rol y estado_solicitud actuales)
    @GetMapping("/me")
    public ResponseEntity<UsuarioPerfilDto> obtenerPerfil(@AuthenticationPrincipal Jwt jwt) {
        UUID usuarioId = UUID.fromString(jwt.getClaimAsString("usuario_id"));
        return ResponseEntity.ok(service.obtenerPerfil(usuarioId));
    }

    // HU-06: el Administrador consulta todas las solicitudes de tutor pendientes de revisión
    @GetMapping("/solicitudes-pendientes")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<SolicitudPendienteDto>> obtenerSolicitudesPendientes() {
        return ResponseEntity.ok(service.obtenerSolicitudesPendientes());
    }

    // HU-07: el Administrador aprueba una solicitud -> el usuario pasa a ser TUTOR
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable UUID id) {
        try {
            service.aprobarSolicitud(id);
            return ResponseEntity.ok("Solicitud aprobada correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // HU-07: el Administrador rechaza una solicitud -> el usuario se mantiene como ALUMNO
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable UUID id) {
        try {
            service.rechazarSolicitud(id);
            return ResponseEntity.ok("Solicitud rechazada correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> obtenerUsuario(@PathVariable UUID id) {
        Usuario usuario = service.obtenerPorId(id);

        Map<String, String> response = new HashMap<>();

        response.put("email", usuario.getEmail());
        response.put("nombre", usuario.getNombre());

        return ResponseEntity.ok(response);
    }

}