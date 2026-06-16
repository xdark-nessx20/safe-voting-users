package com.safevoting.users.domain.exception.otp;

public class ReintentosExcedidosException extends RuntimeException {

    private static final String ERROR_CODE = "REINTENTOS_EXCEDIDOS";

    public ReintentosExcedidosException() {
        super("Número máximo de intentos excedido. El OTP ha sido invalidado");
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
