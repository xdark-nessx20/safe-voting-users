package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CambioMasivoRequest(
        @NotBlank(message = "El alcance es obligatorio")
        String alcance,

        UUID departamentoId,

        UUID municipioId,

        @NotBlank(message = "El estado es obligatorio")
        String estado,

        @NotNull(message = "El municipio del gestor es obligatorio")
        UUID gestorMunicipioId
) {}
