package com.safevoting.users.domain.exception.inscripcion;

import java.util.UUID;

public class SolicitudNotFoundException extends RuntimeException {
    public SolicitudNotFoundException(UUID id) {
        super("SolicitudCambioInscripcion con id %s not found.".formatted(id));
    }
}
