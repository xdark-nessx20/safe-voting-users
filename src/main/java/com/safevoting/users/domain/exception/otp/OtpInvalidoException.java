package com.safevoting.users.domain.model.exception.otp;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class OtpInvalidoException extends DomainException {

    private static final String ERROR_CODE = "OTP_INVALIDO";

    public OtpInvalidoException() {
        super("El código OTP es inválido", ERROR_CODE);
    }
}
