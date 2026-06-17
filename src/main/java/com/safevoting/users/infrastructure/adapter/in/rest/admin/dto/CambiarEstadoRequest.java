package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {}
