package com.safevoting.users.domain.exception.inscripcion;

import java.util.UUID;

public class SolicitudNotFoundException extends RuntimeException {

    private static final String ERROR_CODE = "SOLICITUD_NO_ENCONTRADA";

    public SolicitudNotFoundException(UUID id) {
        super("No se encontró la solicitud con id '%s'".formatted(id));
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
