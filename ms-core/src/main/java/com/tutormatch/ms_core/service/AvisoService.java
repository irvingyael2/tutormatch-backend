package com.tutormatch.ms_core.service;

import com.tutormatch.ms_core.dto.AvisoRequestDto;
import com.tutormatch.ms_core.dto.AvisoResponseDto;
import com.tutormatch.ms_core.entity.Aviso;
import com.tutormatch.ms_core.repository.AvisoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AvisoService {

    private final AvisoRepository avisoRepository;

    public AvisoService(AvisoRepository avisoRepository) {
        this.avisoRepository = avisoRepository;
    }

    @Transactional
    public AvisoResponseDto crearAviso(AvisoRequestDto dto) {
        if (dto.getTitulo() == null || dto.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título del aviso no puede estar vacío.");
        }
        if (dto.getMensaje() == null || dto.getMensaje().isBlank()) {
            throw new IllegalArgumentException("El mensaje del aviso no puede estar vacío.");
        }

        Aviso aviso = new Aviso();
        aviso.setTitulo(dto.getTitulo().trim());
        aviso.setMensaje(dto.getMensaje().trim());

        Aviso guardado = avisoRepository.save(aviso);
        return mapToDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<AvisoResponseDto> listarAvisos() {
        return avisoRepository.findAllByOrderByCreadoEnDesc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AvisoResponseDto actualizarAviso(UUID id, AvisoRequestDto dto) {
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado."));

        if (dto.getTitulo() == null || dto.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título del aviso no puede estar vacío.");
        }
        if (dto.getMensaje() == null || dto.getMensaje().isBlank()) {
            throw new IllegalArgumentException("El mensaje del aviso no puede estar vacío.");
        }

        aviso.setTitulo(dto.getTitulo().trim());
        aviso.setMensaje(dto.getMensaje().trim());

        return mapToDto(avisoRepository.save(aviso));
    }

    @Transactional
    public void eliminarAviso(UUID id) {
        Aviso aviso = avisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aviso no encontrado."));
        avisoRepository.delete(aviso);
    }

    private AvisoResponseDto mapToDto(Aviso aviso) {
        return new AvisoResponseDto(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getMensaje(),
                aviso.getCreadoEn()
        );
    }
}
