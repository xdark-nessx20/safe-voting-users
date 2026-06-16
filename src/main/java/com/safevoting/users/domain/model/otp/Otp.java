package com.safevoting.users.domain.model.otp;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.exception.otp.TransicionEstadoOtpInvalidaException;
import com.safevoting.users.domain.shared.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Otp {

    public static final int CODIGO_LONGITUD = 6;
    public static final int MAXIMO_INTENTOS = 3;
    public static final int TIEMPO_EXPIRACION_MINUTOS = 5;
    private static final String PATRON_CODIGO = "[A-Z0-9]{" + CODIGO_LONGITUD + "}";

    private UUID id;
    private Email email;
    private String codigo;

    @Builder.Default
    private Instant expiracion = Instant.now()
            .plusSeconds(TIEMPO_EXPIRACION_MINUTOS * 60);

    @Builder.Default
    private int intentos = 0;

    @Builder.Default
    private EstadoOtp estado = EstadoOtp.ACTIVO;

    public void validateInfo() {
        email.validateInfo();
        if (codigo == null || !codigo.matches(PATRON_CODIGO)) {
            throw new DatosInvalidosException("El código OTP debe tener " + CODIGO_LONGITUD
                    + " caracteres alfanuméricos en mayúsculas");
        }
        if (expiracion == null) {
            throw new DatosInvalidosException("La expiración del OTP no puede ser nula");
        }
    }

    public boolean esActivo() {
        return this.estado == EstadoOtp.ACTIVO;
    }

    public boolean esUsado() {
        return this.estado == EstadoOtp.USADO;
    }

    public boolean esInvalidado() {
        return this.estado == EstadoOtp.INVALIDADO;
    }

    public void incrementarIntento() {
        this.intentos++;
        if (this.intentos >= MAXIMO_INTENTOS) {
            invalidar();
        }
    }

    public void marcarUsado() {
        if (!esValido()) {
            throw new TransicionEstadoOtpInvalidaException(
                    "No se puede marcar como usado un OTP en estado " + this.estado);
        }
        this.estado = EstadoOtp.USADO;
    }

    public void invalidar() {
        if (!esActivo()) {
            throw new TransicionEstadoOtpInvalidaException(
                    "No se puede invalidar un OTP en estado " + this.estado);
        }
        this.estado = EstadoOtp.INVALIDADO;
    }

    public boolean esValido() {
        return esActivo() && Instant.now().isBefore(this.expiracion);
    }
}
