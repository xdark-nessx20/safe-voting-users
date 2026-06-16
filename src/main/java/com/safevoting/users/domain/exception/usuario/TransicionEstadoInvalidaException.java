package com.safevoting.users.domain.exception.usuario;

public class TransicionEstadoInvalidaException extends RuntimeException {

    private static final String ERROR_CODE = "TRANSICION_INVALIDA";

    public TransicionEstadoInvalidaException(String mensaje) {
        super(mensaje);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
