package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AsignarGestorCandidaturasRequest(
        @NotBlank(message = "El documento es obligatorio")
        String documento
) {}
