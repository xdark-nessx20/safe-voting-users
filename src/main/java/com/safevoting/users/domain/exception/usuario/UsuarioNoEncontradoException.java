package com.safevoting.users.domain.exception.usuario;

import java.util.UUID;

public class UsuarioNoEncontradoException extends RuntimeException {

    private static final String ERROR_CODE = "USUARIO_NO_ENCONTRADO";

    public UsuarioNoEncontradoException(String documento) {
        super("No se encontró ningún usuario con el documento '" + documento + "'");
    }

    public UsuarioNoEncontradoException(UUID id){
        super("No se encontró ningún usuario con el id: %s".formatted(id));
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
