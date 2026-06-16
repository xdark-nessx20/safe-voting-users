package com.safevoting.users.domain.exception.otp;

public class OtpExpiradoException extends RuntimeException {

    private static final String ERROR_CODE = "OTP_EXPIRADO";

    public OtpExpiradoException() {
        super("El código OTP ha expirado");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
