package com.safevoting.users.domain.exception.usuario;

public class DocumentoDuplicadoException extends RuntimeException {

    private static final String ERROR_CODE = "DOCUMENTO_DUPLICADO";

    public DocumentoDuplicadoException(String documento) {
        super("El documento '" + documento + "' ya está registrado");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
