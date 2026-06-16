package com.safevoting.users.domain.model.exception.otp;

import com.safevoting.users.domain.model.exception.comun.DomainException;

public class ReintentosExcedidosException extends DomainException {

    private static final String ERROR_CODE = "REINTENTOS_EXCEDIDOS";

    public ReintentosExcedidosException() {
        super("Número máximo de intentos excedido. El OTP ha sido invalidado", ERROR_CODE);
    }
}
