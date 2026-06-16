package com.safevoting.users.domain.model.exception.usuario;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class DocumentoDuplicadoException extends DomainException {

    private static final String ERROR_CODE = "DOCUMENTO_DUPLICADO";

    public DocumentoDuplicadoException(String documento) {
        super("El documento '" + documento + "' ya está registrado", ERROR_CODE);
    }
}
