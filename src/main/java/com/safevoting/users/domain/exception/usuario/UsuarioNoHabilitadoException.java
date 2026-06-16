package com.safevoting.users.domain.model.exception.usuario;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class UsuarioNoHabilitadoException extends DomainException {

    private static final String ERROR_CODE = "USUARIO_NO_HABILITADO";

    public UsuarioNoHabilitadoException() {
        super("El usuario no está habilitado para autenticarse", ERROR_CODE);
    }
}
