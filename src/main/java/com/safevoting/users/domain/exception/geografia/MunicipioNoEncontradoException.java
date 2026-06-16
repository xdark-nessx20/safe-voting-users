package com.safevoting.users.domain.exception.geografia;

public class MunicipioNoEncontradoException extends RuntimeException {

    private static final String ERROR_CODE = "MUNICIPIO_NO_ENCONTRADO";

    public MunicipioNoEncontradoException(String id) {
        super("El municipio con id '" + id + "' no existe");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
