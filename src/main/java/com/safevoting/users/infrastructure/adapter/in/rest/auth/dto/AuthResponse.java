package com.safevoting.users.infrastructure.adapter.in.rest.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String rol
) {}
