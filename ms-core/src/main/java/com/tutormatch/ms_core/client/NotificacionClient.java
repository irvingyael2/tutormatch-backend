package com.tutormatch.ms_core.client;

import com.tutormatch.ms_core.dto.NotificacionRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-notificaciones", url = "http://localhost:8080/api/notificaciones")
public interface NotificacionClient {

    @PostMapping("/enviar")
    void enviarNotificacion(@RequestBody NotificacionRequestDto request);
}