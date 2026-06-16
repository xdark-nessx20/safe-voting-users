package com.safevoting.users.unit.domain.shared;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoIdentidadTest {

    @Test
    void deberiaCrearDocumentoIdentidadCuandoValorEsValido() {
        DocumentoIdentidad doc = DocumentoIdentidad.builder().valor("123456789").build();
        assertEquals("123456789", doc.getValor());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void deberiaLanzarExcepcionCuandoValorEsNuloOVacio(String valor) {
        assertThrows(DatosInvalidosException.class, () ->
                DocumentoIdentidad.builder().valor(valor).build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoContieneCaracteresNoNumericos() {
        assertThrows(DatosInvalidosException.class, () ->
                DocumentoIdentidad.builder().valor("abc123").build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoLongitudEsMenorACinco() {
        assertThrows(DatosInvalidosException.class, () ->
                DocumentoIdentidad.builder().valor("1234").build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoLongitudEsMayorAQuince() {
        assertThrows(DatosInvalidosException.class, () ->
                DocumentoIdentidad.builder().valor("1234567890123456").build());
    }
}
