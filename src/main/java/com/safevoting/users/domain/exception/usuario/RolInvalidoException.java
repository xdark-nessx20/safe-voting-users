package com.safevoting.users.domain.exception.usuario;

public class RolInvalidoException extends RuntimeException {

    private static final String ERROR_CODE = "ROL_INVALIDO";

    public RolInvalidoException(String mensaje) {
        super(mensaje);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
