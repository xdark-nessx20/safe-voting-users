package com.safevoting.users.domain.exception.usuario;

public class AlcanceInsuficienteException extends RuntimeException {

    private static final String ERROR_CODE = "ALCANCE_INSUFICIENTE";

    public AlcanceInsuficienteException(String mensaje) {
        super(mensaje);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
