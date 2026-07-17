package com.tutormatch.ms_usuarios.service;

import com.tutormatch.ms_usuarios.dto.RegistroDto;
import com.tutormatch.ms_usuarios.dto.SolicitudPendienteDto;
import com.tutormatch.ms_usuarios.dto.UsuarioPerfilDto;
import com.tutormatch.ms_usuarios.entity.Rol;
import com.tutormatch.ms_usuarios.entity.Usuario;
import com.tutormatch.ms_usuarios.repository.RolRepository;
import com.tutormatch.ms_usuarios.repository.UsuarioRepository;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final int JUSTIFICACION_MIN_LENGTH = 20;
    private static final String ROL_ALUMNO = "ROLE_ALUMNO";
    private static final String ROL_TUTOR = "ROLE_TUTOR";
    private static final String ESTADO_PENDIENTE = "pendiente";
    private static final String ESTADO_ACEPTADO = "aceptado";
    private static final String ESTADO_RECHAZADO = "rechazado";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Registrar: se encarga de registrar un nuevo usuario en la base de datos.
    @Transactional
    public Usuario registrar(RegistroDto dto) {
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(dto.getNombre());
        nuevoUsuario.setEmail(dto.getEmail());
        nuevoUsuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        nuevoUsuario.setRoles(new java.util.HashSet<>());

        Rol rolAlumno = rolRepository.findByNombre(ROL_ALUMNO)
                .orElseThrow(() -> new RuntimeException("Error crítico: El rol ALUMNO no existe en la base de datos"));

        nuevoUsuario.getRoles().add(rolAlumno);

        return usuarioRepository.save(nuevoUsuario);
    }

    // HU-05: un Alumno envía su justificación para solicitar el rol de Tutor.
    @Transactional
    public void solicitarTutor(UUID usuarioId, String justificacion) {
        if (justificacion == null || justificacion.trim().length() < JUSTIFICACION_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "La justificación debe tener al menos " + JUSTIFICACION_MIN_LENGTH + " caracteres");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        boolean esAlumno = usuario.getRoles().stream()
                .anyMatch(rol -> ROL_ALUMNO.equals(rol.getNombre()));
        if (!esAlumno) {
            throw new RuntimeException("Solo los usuarios con rol Alumno pueden solicitar ser Tutor");
        }

        if (ESTADO_PENDIENTE.equals(usuario.getEstadoSolicitud())) {
            throw new RuntimeException("Ya existe una solicitud pendiente de revisión");
        }

        usuario.setJustificacion(justificacion.trim());
        usuario.setEstadoSolicitud(ESTADO_PENDIENTE);
        usuarioRepository.save(usuario);
    }

    // HU-05 / HU-08: perfil resumido del usuario autenticado, usado por el
    // frontend para saber su estado_solicitud actual y sus roles vigentes.
    public UsuarioPerfilDto obtenerPerfil(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UsuarioPerfilDto dto = new UsuarioPerfilDto();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setEstadoSolicitud(usuario.getEstadoSolicitud());
        dto.setRoles(usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toList()));

        return dto;
    }

    // HU-06: lista de solicitudes pendientes que ve el Administrador en su panel.
    public List<SolicitudPendienteDto> obtenerSolicitudesPendientes() {
        return usuarioRepository.findByEstadoSolicitud(ESTADO_PENDIENTE).stream()
                .map(usuario -> {
                    SolicitudPendienteDto dto = new SolicitudPendienteDto();
                    dto.setId(usuario.getId());
                    dto.setNombre(usuario.getNombre());
                    dto.setEmail(usuario.getEmail());
                    dto.setJustificacion(usuario.getJustificacion());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU-07: el Administrador aprueba la solicitud -> el usuario gana el rol TUTOR
    // (conserva ALUMNO también, ya que "roles" es un set y puede seguir inscribiéndose
    // a tutorías de otros como alumno).
    @Transactional
    public void aprobarSolicitud(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!ESTADO_PENDIENTE.equals(usuario.getEstadoSolicitud())) {
            throw new RuntimeException("Esta solicitud ya fue revisada anteriormente");
        }

        Rol rolTutor = rolRepository.findByNombre(ROL_TUTOR)
                .orElseThrow(() -> new RuntimeException("Error crítico: El rol TUTOR no existe en la base de datos"));

        usuario.getRoles().add(rolTutor);
        usuario.setEstadoSolicitud(ESTADO_ACEPTADO);
        usuarioRepository.save(usuario);
    }

    // HU-07: el Administrador rechaza la solicitud -> el rol se mantiene como ALUMNO
    @Transactional
    public void rechazarSolicitud(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!ESTADO_PENDIENTE.equals(usuario.getEstadoSolicitud())) {
            throw new RuntimeException("Esta solicitud ya fue revisada anteriormente");
        }

        usuario.setEstadoSolicitud(ESTADO_RECHAZADO);
        usuarioRepository.save(usuario);
    // Obtener usuarios por ID
    public Usuario obtenerPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }
}