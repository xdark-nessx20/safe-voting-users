package com.safevoting.users.infrastructure.adapter.in.rest.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "El código OTP es obligatorio")
        String codigo
) {}
