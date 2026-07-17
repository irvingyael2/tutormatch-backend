package com.tutormatch.ms_evaluaciones.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class EvaluacionRequestDto {
    private UUID sesionId;
    private UUID tutorId;
    private Integer calificacion;
    private String comentario;
}
