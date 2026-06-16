package com.safevoting.users.domain.exception.otp;

public class OtpInvalidoException extends RuntimeException {

    private static final String ERROR_CODE = "OTP_INVALIDO";

    public OtpInvalidoException() {
        super("El código OTP es inválido");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
