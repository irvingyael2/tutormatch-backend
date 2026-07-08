package com.tutormatch.ms_core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de SALIDA con los datos de una sesión publicada.
 * Nunca se devuelve la Entity directamente en el controlador.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SesionResponseDto {

    private UUID id;
    private UUID tutorId;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaHora;
    private Integer cupoMaximo;
    private Integer cupoDisponible;
    private LocalDateTime creadoEn;
}
