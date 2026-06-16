package com.safevoting.users.domain.model.exception.usuario;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class EmailNoRegistradoException extends DomainException {

    private static final String ERROR_CODE = "EMAIL_NO_REGISTRADO";

    public EmailNoRegistradoException(String email) {
        super("El email '" + email + "' no está registrado", ERROR_CODE);
    }
}
