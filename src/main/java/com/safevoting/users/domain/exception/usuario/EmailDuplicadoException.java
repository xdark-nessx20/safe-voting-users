package com.safevoting.users.domain.exception.usuario;

public class EmailDuplicadoException extends RuntimeException {

    private static final String ERROR_CODE = "EMAIL_DUPLICADO";

    public EmailDuplicadoException(String email) {
        super("El email '" + email + "' ya está registrado");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
