package com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SolicitarCambioRequest(
        @NotNull(message = "El municipio destino es obligatorio")
        UUID municipioDestinoId,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
        String motivo
) {}
