package com.tutormatch.ms_core.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de ENTRADA para crear una nueva sesión de tutoría.
 * El tutor_id NO viene aquí — se extrae del JWT en el servicio.
 */
@Getter
@Setter
@NoArgsConstructor
public class SesionRequestDto {

    private String titulo;

    private String descripcion;

    // Formato esperado: "2025-08-15T10:30:00"
    private LocalDateTime fechaHora;

    private Integer cupoMaximo;
}
