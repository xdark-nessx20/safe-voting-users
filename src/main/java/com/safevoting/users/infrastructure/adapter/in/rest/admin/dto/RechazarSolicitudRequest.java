package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarSolicitudRequest(
        @NotBlank(message = "El motivo de rechazo es obligatorio")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
        String motivoRechazo
) {}
