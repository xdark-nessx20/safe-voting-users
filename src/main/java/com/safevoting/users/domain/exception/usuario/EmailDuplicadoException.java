package com.safevoting.users.domain.model.exception.usuario;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class EmailDuplicadoException extends DomainException {

    private static final String ERROR_CODE = "EMAIL_DUPLICADO";

    public EmailDuplicadoException(String email) {
        super("El email '" + email + "' ya está registrado", ERROR_CODE);
    }
}
