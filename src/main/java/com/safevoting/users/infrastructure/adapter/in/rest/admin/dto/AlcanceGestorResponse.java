package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import java.util.UUID;

public record AlcanceGestorResponse(
        String alcanceOperacion,
        UUID municipioId,
        String municipioNombre,
        UUID departamentoId,
        String departamentoNombre
) {}
