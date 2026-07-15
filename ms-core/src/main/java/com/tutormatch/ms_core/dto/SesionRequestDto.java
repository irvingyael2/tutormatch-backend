package com.tutormatch.ms_core.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de ENTRADA para crear una nueva sesión de tutoría (HU-09).
 * El tutor_id NO viene aquí — se extrae del JWT en el servicio.
 */
@Getter
@Setter
@NoArgsConstructor
public class SesionRequestDto {

    private String titulo;

    private String descripcion;

    /** Lugar físico o virtual donde se impartirá (ej. "Sala 3B", "En línea - Meet") */
    private String lugar;

    // Formato esperado ISO-8601: "2026-08-15T10:30:00"
    private LocalDateTime fechaHora;

    private Integer cupoMaximo;
}
