package com.safevoting.users.domain.exception.inscripcion;

public class MotivoRequeridoException extends RuntimeException {

    private static final String ERROR_CODE = "MOTIVO_REQUERIDO";

    public MotivoRequeridoException() {
        super("El motivo de rechazo es obligatorio");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
