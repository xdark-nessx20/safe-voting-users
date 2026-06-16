package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import java.util.UUID;

public record UsuarioResponse(
        UUID id,
        String nombre,
        String email,
        String documento,
        String municipioNombre,
        String departamentoNombre,
        String estado,
        String rol,
        String telefono
) {}
