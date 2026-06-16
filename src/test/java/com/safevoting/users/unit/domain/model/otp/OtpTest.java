package com.safevoting.users.unit.domain.model.otp;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.exception.otp.TransicionEstadoOtpInvalidaException;
import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.shared.Email;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OtpTest {

    private final Email email = Email.builder().valor("juan@example.com").build();

    @Test
    void deberiaConstruirOtpValidoConBuilderYValidacionExitosa() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo("ABC123")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertDoesNotThrow(otp::validateInfo);
        assertEquals("ABC123", otp.getCodigo());
        assertEquals(EstadoOtp.ACTIVO, otp.getEstado());
    }

    @Test
    void deberiaConstruirOtpValidoConCodigoNumerico() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertDoesNotThrow(otp::validateInfo);
        assertTrue(otp.esActivo());
        assertFalse(otp.esUsado());
        assertFalse(otp.esInvalidado());
    }

    @Test
    void deberiaLanzarExcepcionCuandoCodigoNoTieneSeisCaracteres() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("12345")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertThrows(DatosInvalidosException.class, otp::validateInfo);
    }

    @Test
    void deberiaLanzarExcepcionCuandoCodigoTieneMinusculas() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("abc123")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertThrows(DatosInvalidosException.class, otp::validateInfo);
    }

    @Test
    void deberiaInvalidarOtpDespuesDeTresIncrementosDeIntento() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        otp.incrementarIntento();
        assertEquals(1, otp.getIntentos());
        assertTrue(otp.esActivo());

        otp.incrementarIntento();
        assertEquals(2, otp.getIntentos());

        otp.incrementarIntento();
        assertEquals(3, otp.getIntentos());
        assertTrue(otp.esInvalidado());
    }

    @Test
    void deberiaRetornarFalseCuandoEsValidoConFechaExpirada() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().minus(1, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertFalse(otp.esValido());
    }

    @Test
    void deberiaRetornarTrueCuandoEsValidoConFechaFutura() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertTrue(otp.esValido());
    }

    @Test
    void deberiaMarcarOtpComoUsadoCuandoEstadoEsActivo() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        otp.marcarUsado();
        assertTrue(otp.esUsado());
    }

    @Test
    void deberiaLanzarExcepcionCuandoMarcarUsadoEnEstadoNoActivo() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.USADO)
                .build();

        assertThrows(TransicionEstadoOtpInvalidaException.class, otp::marcarUsado);
    }

    @Test
    void deberiaInvalidarOtpCuandoEstadoEsActivo() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        otp.invalidar();
        assertTrue(otp.esInvalidado());
    }

    @Test
    void deberiaLanzarExcepcionCuandoInvalidarEnEstadoNoActivo() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.INVALIDADO)
                .build();

        assertThrows(TransicionEstadoOtpInvalidaException.class, otp::invalidar);
    }

    @Test
    void deberiaRetornarFalseCuandoOtpNoEstaActivoEnEsValido() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.USADO)
                .build();

        assertFalse(otp.esValido());
    }
}
