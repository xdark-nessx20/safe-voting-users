package com.safevoting.users.domain.model.exception.geografia;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class MunicipioNoEncontradoException extends DomainException {

    private static final String ERROR_CODE = "MUNICIPIO_NO_ENCONTRADO";

    public MunicipioNoEncontradoException(String id) {
        super("El municipio con id '" + id + "' no existe", ERROR_CODE);
    }
}
