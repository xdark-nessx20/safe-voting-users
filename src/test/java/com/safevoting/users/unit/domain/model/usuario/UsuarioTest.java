package com.safevoting.users.unit.domain.model.usuario;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    private Departamento departamento;
    private Municipio municipio;
    private Email emailValido;
    private DocumentoIdentidad documentoValido;

    @BeforeEach
    void setUp() {
        departamento = Departamento.builder()
                .id(UUID.randomUUID())
                .nombre("Antioquia")
                .build();
        municipio = Municipio.builder()
                .id(UUID.randomUUID())
                .nombre("Medellín")
                .departamento(departamento)
                .build();
        emailValido = Email.builder().valor("juan@example.com").build();
        documentoValido = DocumentoIdentidad.builder().valor("123456789").build();
    }

    @Test
    void deberiaConstruirUsuarioValidoConBuilderYValidacionExitosa() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan Pérez")
                .email(emailValido)
                .telefono("3001234567")
                .documento(documentoValido)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(Instant.now())
                .build();

        assertDoesNotThrow(usuario::validateInfo);
        assertEquals("Juan Pérez", usuario.getNombre());
        assertEquals(EstadoUsuario.ACTIVO, usuario.getEstado());
    }

    @Test
    void deberiaLanzarExcepcionCuandoEmailEsNulo() {
        assertThrows(DatosInvalidosException.class, () ->
                Usuario.builder()
                        .nombre("Juan")
                        .email(null)
                        .documento(documentoValido)
                        .municipio(municipio)
                        .rol(Rol.VOTANTE)
                        .estado(EstadoUsuario.ACTIVO)
                        .build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoDocumentoEsInvalido() {
        assertThrows(DatosInvalidosException.class, () -> {
            DocumentoIdentidad docInvalido = DocumentoIdentidad.builder().valor("abc").build();
            Usuario.builder()
                    .nombre("Juan")
                    .email(emailValido)
                    .documento(docInvalido)
                    .municipio(municipio)
                    .rol(Rol.VOTANTE)
                    .estado(EstadoUsuario.ACTIVO)
                    .build();
        });
    }

    @Test
    void deberiaLanzarExcepcionCuandoNombreEsVacio() {
        assertThrows(DatosInvalidosException.class, () ->
                Usuario.builder()
                        .nombre("")
                        .email(emailValido)
                        .documento(documentoValido)
                        .municipio(municipio)
                        .rol(Rol.VOTANTE)
                        .estado(EstadoUsuario.ACTIVO)
                        .build());
    }

    @Test
    void deberiaHabilitarUsuarioCambiandoEstadoAHabilitado() {
        Usuario usuario = Usuario.builder()
                .nombre("Juan")
                .email(emailValido)
                .documento(documentoValido)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        usuario.habilitar();
        assertEquals(EstadoUsuario.HABILITADO, usuario.getEstado());
    }

    @Test
    void deberiaSuspenderUsuarioCambiandoEstadoAInactivo() {
        Usuario usuario = Usuario.builder()
                .nombre("Juan")
                .email(emailValido)
                .documento(documentoValido)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.HABILITADO)
                .build();

        usuario.suspender();
        assertEquals(EstadoUsuario.INACTIVO, usuario.getEstado());
    }
}
