package com.tutormatch.ms_core.service;

import com.tutormatch.ms_core.dto.RecursoRequestDto;
import com.tutormatch.ms_core.dto.RecursoResponseDto;
import com.tutormatch.ms_core.entity.Recurso;
import com.tutormatch.ms_core.entity.Sesion;
import com.tutormatch.ms_core.repository.RecursoRepository;
import com.tutormatch.ms_core.repository.SesionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final SesionRepository sesionRepository;

    public RecursoService(RecursoRepository recursoRepository, SesionRepository sesionRepository) {
        this.recursoRepository = recursoRepository;
        this.sesionRepository = sesionRepository;
    }

    /**
     * Épica 7: Agrega un nuevo recurso (material) a una sesión de tutoría existente.
     *
     * @param sesionId ID de la sesión a la que se asocia el recurso
     * @param dto      Datos del recurso (titulo y url)
     * @return         DTO con los datos del recurso recién creado
     */
    @Transactional
    public RecursoResponseDto agregarRecurso(UUID sesionId, RecursoRequestDto dto) {

        // Buscar la sesión; si no existe lanza excepción
        Sesion sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la sesión con ID: " + sesionId));

        // Crear y asociar el recurso
        Recurso recurso = new Recurso();
        recurso.setSesion(sesion);
        recurso.setTitulo(dto.getTitulo());
        recurso.setUrl(dto.getUrl());

        Recurso recursoGuardado = recursoRepository.save(recurso);

        return mapToDto(recursoGuardado);
    }

    /**
     * Épica 7: Obtiene todos los recursos asociados a una sesión de tutoría.
     *
     * @param sesionId ID de la sesión
     * @return         Lista de DTOs de recursos
     */
    public List<RecursoResponseDto> obtenerRecursosPorSesion(UUID sesionId) {
        List<Recurso> recursos = recursoRepository.findBySesionId(sesionId);

        return recursos.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Épica 7: Elimina un recurso por su ID, validando que exista previamente.
     *
     * @param recursoId ID del recurso a eliminar
     */
    @Transactional
    public void eliminarRecurso(UUID recursoId) {
        if (!recursoRepository.existsById(recursoId)) {
            throw new IllegalArgumentException(
                    "No se encontró el recurso con ID: " + recursoId);
        }

        recursoRepository.deleteById(recursoId);
    }

    /**
     * Convierte una entidad Recurso a su DTO de respuesta.
     * Nunca se devuelven entidades puras al controlador.
     */
    private RecursoResponseDto mapToDto(Recurso recurso) {
        RecursoResponseDto dto = new RecursoResponseDto();
        dto.setId(recurso.getId());
        dto.setSesionId(recurso.getSesion().getId());
        dto.setTitulo(recurso.getTitulo());
        dto.setUrl(recurso.getUrl());
        dto.setCreadoEn(recurso.getCreadoEn());
        return dto;
    }
}
