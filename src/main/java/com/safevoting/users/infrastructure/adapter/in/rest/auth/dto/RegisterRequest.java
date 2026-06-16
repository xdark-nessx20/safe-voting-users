package com.safevoting.users.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email no es válido")
        String email,

        String telefono,

        @NotBlank(message = "El documento es obligatorio")
        String documento,

        @NotNull(message = "El municipio es obligatorio")
        UUID municipioId
) {}
