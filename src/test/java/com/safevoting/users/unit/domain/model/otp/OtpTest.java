package com.safevoting.users.unit.domain.model.otp;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
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
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        assertDoesNotThrow(otp::validateInfo);
        assertEquals("123456", otp.getCodigo());
        assertEquals(EstadoOtp.ACTIVO, otp.getEstado());
    }

    @Test
    void deberiaLanzarExcepcionCuandoCodigoNoTieneSeisDigitos() {
        assertThrows(DatosInvalidosException.class, () ->
                Otp.builder()
                        .email(email)
                        .codigo("12345")
                        .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                        .intentos(0)
                        .estado(EstadoOtp.ACTIVO)
                        .build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoCodigoNoEsNumerico() {
        assertThrows(DatosInvalidosException.class, () ->
                Otp.builder()
                        .email(email)
                        .codigo("abc123")
                        .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                        .intentos(0)
                        .estado(EstadoOtp.ACTIVO)
                        .build());
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
        assertEquals(EstadoOtp.ACTIVO, otp.getEstado());

        otp.incrementarIntento();
        assertEquals(2, otp.getIntentos());

        otp.incrementarIntento();
        assertEquals(3, otp.getIntentos());
        assertEquals(EstadoOtp.INVALIDADO, otp.getEstado());
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
    void deberiaMarcarOtpComoUsado() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        otp.marcarUsado();
        assertEquals(EstadoOtp.USADO, otp.getEstado());
    }

    @Test
    void deberiaInvalidarOtpExplicitamente() {
        Otp otp = Otp.builder()
                .email(email)
                .codigo("123456")
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        otp.invalidar();
        assertEquals(EstadoOtp.INVALIDADO, otp.getEstado());
    }

    @Test
    void deberiaRetornarFalseCuandoOtpNoEstaActivo() {
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
