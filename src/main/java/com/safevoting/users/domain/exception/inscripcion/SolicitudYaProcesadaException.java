package com.safevoting.users.domain.exception.inscripcion;

import java.util.UUID;

public class SolicitudYaProcesadaException extends RuntimeException {

    private static final String ERROR_CODE = "SOLICITUD_YA_PROCESADA";

    public SolicitudYaProcesadaException(UUID solicitudId) {
        super("La solicitud " + solicitudId + " ya fue procesada y no puede modificarse");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
