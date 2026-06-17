package com.safevoting.users.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(
        @NotBlank(message = "El documento es obligatorio")
        String documento
) {}
