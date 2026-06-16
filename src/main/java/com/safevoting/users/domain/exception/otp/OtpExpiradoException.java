package com.safevoting.users.domain.model.exception.otp;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class OtpExpiradoException extends DomainException {

    private static final String ERROR_CODE = "OTP_EXPIRADO";

    public OtpExpiradoException() {
        super("El código OTP ha expirado", ERROR_CODE);
    }
}
