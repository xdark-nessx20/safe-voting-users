package com.safevoting.users.domain.exception.common;

public class DatosInvalidosException extends RuntimeException {

    private static final String ERROR_CODE = "DATOS_INVALIDOS";

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
