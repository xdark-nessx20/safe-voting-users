package com.safevoting.users.domain.exception.usuario;

public class GestorNoModificableException extends RuntimeException {

    private static final String ERROR_CODE = "GESTOR_NO_MODIFICABLE";

    public GestorNoModificableException() {
        super("No se permite modificar a otro gestor electoral");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
