package com.safevoting.users.domain.model.otp;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import com.safevoting.users.domain.shared.Email;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
public class Otp {

    public static final int CODIGO_LONGITUD = 6;
    public static final int MAXIMO_INTENTOS = 3;
    public static final int TIEMPO_EXPIRACION_MINUTOS = 5;

    private UUID id;
    private Email email;
    private String codigo;
    private Instant expiracion;
    private int intentos;
    private EstadoOtp estado;

    public Otp(UUID id, Email email, String codigo, Instant expiracion, int intentos, EstadoOtp estado) {
        this.id = id;
        this.email = email;
        this.codigo = codigo;
        this.expiracion = expiracion;
        this.intentos = intentos;
        this.estado = estado;
    }

    public static class OtpBuilder {
        public Otp build() {
            Otp o = new Otp(id, email, codigo, expiracion, intentos, estado);
            o.validateInfo();
            return o;
        }
    }

    public void validateInfo() {
        if (email == null) {
            throw new DatosInvalidosException("El email del OTP no puede ser nulo");
        }
        email.validateInfo();
        if (codigo == null || codigo.length() != CODIGO_LONGITUD) {
            throw new DatosInvalidosException("El código OTP debe tener " + CODIGO_LONGITUD + " caracteres");
        }
        if (!codigo.matches("\\d+")) {
            throw new DatosInvalidosException("El código OTP debe ser numérico");
        }
        if (expiracion == null) {
            throw new DatosInvalidosException("La expiración del OTP no puede ser nula");
        }
        if (estado == null) {
            throw new DatosInvalidosException("El estado del OTP no puede ser nulo");
        }
    }

    public void incrementarIntento() {
        this.intentos++;
        if (this.intentos >= MAXIMO_INTENTOS) {
            invalidar();
        }
    }

    public void marcarUsado() {
        this.estado = EstadoOtp.USADO;
    }

    public void invalidar() {
        this.estado = EstadoOtp.INVALIDADO;
    }

    public boolean esValido() {
        return this.estado == EstadoOtp.ACTIVO && Instant.now().isBefore(this.expiracion);
    }
}
