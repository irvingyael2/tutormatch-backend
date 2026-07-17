package com.tutormatch.ms_evaluaciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluacionResponseDto {
    private UUID id;
    private UUID tutorId;
    private UUID alumnoId;
    private UUID sesionId;
    private Integer calificacion;
    private String comentario;
    private LocalDateTime creadoEn;
}
