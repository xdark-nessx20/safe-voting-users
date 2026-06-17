package com.safevoting.users.infrastructure.adapter.in.rest.admin.dto;

import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AsignarGestorRequest(
        @NotBlank(message = "El documento es obligatorio")
        String documento,

        @NotNull(message = "El alcance de operación es obligatorio")
        AlcanceOperacion alcanceOperacion
) {}
