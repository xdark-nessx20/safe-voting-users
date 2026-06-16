package com.safevoting.users.domain.exception.usuario;

public class UsuarioNoHabilitadoException extends RuntimeException {

    private static final String ERROR_CODE = "USUARIO_NO_HABILITADO";

    public UsuarioNoHabilitadoException() {
        super("El usuario no está habilitado para autenticarse");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
