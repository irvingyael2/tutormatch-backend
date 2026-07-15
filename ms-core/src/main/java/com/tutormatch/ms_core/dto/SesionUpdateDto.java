package com.tutormatch.ms_core.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de ENTRADA para EDITAR una sesión existente (HU-11).
 * Se usa en el endpoint PUT /api/core/sesiones-tutorias/{id}.
 *
 * Nota de negocio: si la sesión ya tiene alumnos inscritos (inscritos > 0),
 * el backend ignorará cualquier cambio en fechaHora — ese bloqueo se aplica en el Service.
 */
@Getter
@Setter
@NoArgsConstructor
public class SesionUpdateDto {

    private String titulo;

    private String descripcion;

    private String lugar;

    /**
     * Solo se actualizará si la sesión NO tiene inscritos.
     * Si tiene inscritos, el service lanzará excepción si se intenta cambiar.
     */
    private LocalDateTime fechaHora;

    private Integer cupoMaximo;
}
