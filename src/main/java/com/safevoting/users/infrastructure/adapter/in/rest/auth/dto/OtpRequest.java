package com.safevoting.users.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OtpRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email
) {}
