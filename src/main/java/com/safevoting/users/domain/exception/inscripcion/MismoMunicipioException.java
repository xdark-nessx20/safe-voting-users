package com.safevoting.users.domain.exception.inscripcion;

public class MismoMunicipioException extends RuntimeException {

    private static final String ERROR_CODE = "MISMO_MUNICIPIO";

    public MismoMunicipioException() {
        super("El municipio destino no puede ser igual al municipio de inscripción actual");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
