package com.tutormatch.ms_core.client;

import com.tutormatch.ms_core.dto.UsuarioResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.UUID;

@FeignClient(name = "ms-usuarios", url = "http://localhost:8080/api/usuarios")
public interface UsuarioClient {

    @GetMapping("/{id}")
    UsuarioResponseDto obtenerUsuarioPorId(@PathVariable("id") UUID id);
}