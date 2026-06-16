package com.safevoting.users.domain.exception.otp;

public class TransicionEstadoOtpInvalidaException extends RuntimeException {

    private static final String ERROR_CODE = "TRANSICION_ESTADO_OTP_INVALIDA";

    public TransicionEstadoOtpInvalidaException(String mensaje) {
        super(mensaje);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
