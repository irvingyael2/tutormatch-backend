package com.tutormatch.ms_core.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class NotificacionRequestDto {
    private UUID usuarioId;
    private String correoDestino;
    private String titulo;
    private String mensaje;
}