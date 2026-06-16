package com.safevoting.users.domain.model.exception.comun;

public class DatosInvalidosException extends DomainException {

    private static final String ERROR_CODE = "DATOS_INVALIDOS";

    public DatosInvalidosException(String mensaje) {
        super(mensaje, ERROR_CODE);
    }
}
