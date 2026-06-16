package com.safevoting.users.domain.model.exception.comun;

public class RolInvalidoException extends DomainException {

    private static final String ERROR_CODE = "ROL_INVALIDO";

    public RolInvalidoException(String mensaje) {
        super(mensaje, ERROR_CODE);
    }
}
