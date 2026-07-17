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
public class AvisoResponseDto {

    private UUID id;
    private String titulo;
    private String mensaje;
    private LocalDateTime creadoEn;
}
