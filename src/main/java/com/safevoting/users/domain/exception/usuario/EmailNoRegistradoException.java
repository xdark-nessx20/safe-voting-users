package com.safevoting.users.domain.exception.usuario;

public class EmailNoRegistradoException extends RuntimeException {

    private static final String ERROR_CODE = "EMAIL_NO_REGISTRADO";

    public EmailNoRegistradoException(String email) {
        super("El email '" + email + "' no está registrado");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
