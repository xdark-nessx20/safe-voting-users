package com.safevoting.users.unit.domain.shared;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import com.safevoting.users.domain.shared.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void deberiaCrearEmailCuandoValorEsValido() {
        Email email = Email.builder().valor("juan@example.com").build();
        assertEquals("juan@example.com", email.getValor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void deberiaLanzarExcepcionCuandoValorEsNuloOVacio(String valor) {
        assertThrows(DatosInvalidosException.class, () ->
                Email.builder().valor(valor).build());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sinarroba.com", "sindominio@", "@sinlocal"})
    void deberiaLanzarExcepcionCuandoFormatoEsInvalido(String valor) {
        assertThrows(DatosInvalidosException.class, () ->
                Email.builder().valor(valor).build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoNoTieneArroba() {
        assertThrows(DatosInvalidosException.class, () ->
                Email.builder().valor("sinarroba.com").build());
    }
}
