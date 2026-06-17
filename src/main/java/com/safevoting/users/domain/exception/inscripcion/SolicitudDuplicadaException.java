package com.safevoting.users.domain.exception.inscripcion;

public class SolicitudDuplicadaException extends RuntimeException {

    private static final String ERROR_CODE = "SOLICITUD_DUPLICADA";

    public SolicitudDuplicadaException(String mensaje) {
        super(mensaje);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
