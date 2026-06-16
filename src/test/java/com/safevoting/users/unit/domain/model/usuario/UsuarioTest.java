package com.safevoting.users.unit.domain.model.usuario;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.domain.shared.Phone;
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

    private Usuario crearUsuarioActivo() {
        Usuario u = Usuario.builder()
                .nombre("Juan Pérez")
                .email(emailValido)
                .telefono(Phone.builder().valor("3001234567").build())
                .documento(documentoValido)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(Instant.now())
                .build();
        return u;
    }

    @Test
    void deberiaConstruirUsuarioValidoConValidacionExitosa() {
        Usuario usuario = crearUsuarioActivo();
        assertDoesNotThrow(usuario::validateInfo);
        assertEquals("Juan Pérez", usuario.getNombre());
        assertTrue(usuario.esActivo());
        assertFalse(usuario.esHabilitado());
        assertFalse(usuario.esInactivo());
    }

    @Test
    void deberiaLanzarExcepcionCuandoEmailEsNulo() {
        Usuario usuario = Usuario.builder()
                .nombre("Juan")
                .email(null)
                .documento(documentoValido)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        assertThrows(DatosInvalidosException.class, usuario::validateInfo);
    }

    @Test
    void deberiaLanzarExcepcionCuandoDocumentoEsInvalido() {
        assertThrows(DatosInvalidosException.class, () ->
                DocumentoIdentidad.builder().valor("abc").build());
    }

    @Test
    void deberiaLanzarExcepcionCuandoNombreEsVacio() {
        Usuario usuario = Usuario.builder()
                .nombre("")
                .email(emailValido)
                .documento(documentoValido)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        assertThrows(DatosInvalidosException.class, usuario::validateInfo);
    }

    @Test
    void deberiaHabilitarUsuarioActivo() {
        Usuario usuario = crearUsuarioActivo();
        usuario.habilitar();
        assertTrue(usuario.esHabilitado());
    }

    @Test
    void deberiaLanzarExcepcionCuandoHabilitarUsuarioNoActivo() {
        Usuario usuario = crearUsuarioActivo();
        usuario.habilitar();
        assertThrows(DatosInvalidosException.class, usuario::habilitar);
    }

    @Test
    void deberiaInhabilitarUsuarioHabilitado() {
        Usuario usuario = crearUsuarioActivo();
        usuario.habilitar();
        usuario.inhabilitar();
        assertTrue(usuario.esActivo());
    }

    @Test
    void deberiaLanzarExcepcionCuandoInhabilitarUsuarioNoHabilitado() {
        Usuario usuario = crearUsuarioActivo();
        assertThrows(DatosInvalidosException.class, usuario::inhabilitar);
    }

    @Test
    void deberiaSuspenderUsuarioActivo() {
        Usuario usuario = crearUsuarioActivo();
        usuario.suspender();
        assertTrue(usuario.esInactivo());
    }

    @Test
    void deberiaSuspenderUsuarioHabilitado() {
        Usuario usuario = crearUsuarioActivo();
        usuario.habilitar();
        usuario.suspender();
        assertTrue(usuario.esInactivo());
    }

    @Test
    void deberiaLanzarExcepcionCuandoSuspenderUsuarioInactivo() {
        Usuario usuario = crearUsuarioActivo();
        usuario.suspender();
        assertThrows(DatosInvalidosException.class, usuario::suspender);
    }

    @Test
    void deberiaReactivarUsuarioInactivo() {
        Usuario usuario = crearUsuarioActivo();
        usuario.suspender();
        usuario.reactivar();
        assertTrue(usuario.esActivo());
    }

    @Test
    void deberiaLanzarExcepcionCuandoReactivarUsuarioNoInactivo() {
        Usuario usuario = crearUsuarioActivo();
        assertThrows(DatosInvalidosException.class, usuario::reactivar);
    }

    @Test
    void deberiaCambiarMunicipioValido() {
        Usuario usuario = crearUsuarioActivo();
        Municipio nuevoMunicipio = Municipio.builder()
                .id(UUID.randomUUID())
                .nombre("Envigado")
                .departamento(departamento)
                .build();

        usuario.setMunicipio(nuevoMunicipio);
        assertEquals("Envigado", usuario.getMunicipio().getNombre());
    }

    @Test
    void deberiaLanzarExcepcionCuandoMunicipioEsNulo() {
        Usuario usuario = crearUsuarioActivo();
        assertThrows(DatosInvalidosException.class, () -> usuario.setMunicipio(null));
    }
}
