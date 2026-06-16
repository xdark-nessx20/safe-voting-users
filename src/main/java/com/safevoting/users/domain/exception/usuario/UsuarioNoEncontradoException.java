package com.safevoting.users.domain.exception.usuario;

public class UsuarioNoEncontradoException extends RuntimeException {

    private static final String ERROR_CODE = "USUARIO_NO_ENCONTRADO";

    public UsuarioNoEncontradoException(String documento) {
        super("No se encontr\u00f3 ning\u00fan usuario con el documento '" + documento + "'");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
