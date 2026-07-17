package com.tutormatch.ms_core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreguntaResponseDto {

    private UUID id;
    private UUID sesionId;
    private UUID alumnoId;
    private String alumnoNombre;
    private String pregunta;
    private String respuesta;
    private LocalDateTime creadoEn;
    private LocalDateTime respondidoEn;
}
