package com.safevoting.users.domain.exception.usuario;

public class UsuarioInactivoException extends RuntimeException {

    private static final String ERROR_CODE = "USUARIO_INACTIVO";

    public UsuarioInactivoException() {
        super("El usuario está inactivo y no puede solicitar un código OTP");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
