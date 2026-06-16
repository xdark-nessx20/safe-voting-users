package com.safevoting.users.domain.model.exception.comun;

public class DomainException extends RuntimeException {

    private final String errorCode;

    public DomainException(String mensaje, String errorCode) {
        super(mensaje);
        this.errorCode = errorCode;
    }

    public DomainException(String mensaje) {
        super(mensaje);
        this.errorCode = "DOMAIN_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
