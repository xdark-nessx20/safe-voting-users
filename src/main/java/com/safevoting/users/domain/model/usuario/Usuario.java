package com.safevoting.users.domain.model.usuario;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
public class Usuario {

    private UUID id;
    private String nombre;
    private Email email;
    private String telefono;
    private DocumentoIdentidad documento;
    private Municipio municipio;
    private Rol rol;
    private EstadoUsuario estado;
    private Instant createdAt;

    public Usuario(UUID id, String nombre, Email email, String telefono, DocumentoIdentidad documento,
                   Municipio municipio, Rol rol, EstadoUsuario estado, Instant createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.documento = documento;
        this.municipio = municipio;
        this.rol = rol;
        this.estado = estado;
        this.createdAt = createdAt;
    }

    public static class UsuarioBuilder {
        public Usuario build() {
            Usuario u = new Usuario(id, nombre, email, telefono, documento, municipio, rol, estado, createdAt);
            u.validateInfo();
            return u;
        }
    }

    public void validateInfo() {
        validateNombre();
        validateEmail();
        validateDocumento();
        validateMunicipio();
        validateRol();
        validateEstado();
    }

    private void validateNombre() {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre no puede estar vacío");
        }
    }

    private void validateEmail() {
        if (email == null) {
            throw new DatosInvalidosException("El email no puede ser nulo");
        }
        email.validateInfo();
    }

    private void validateDocumento() {
        if (documento == null) {
            throw new DatosInvalidosException("El documento no puede ser nulo");
        }
        documento.validateInfo();
    }

    private void validateMunicipio() {
        if (municipio == null) {
            throw new DatosInvalidosException("El municipio no puede ser nulo");
        }
    }

    private void validateRol() {
        if (rol == null) {
            throw new DatosInvalidosException("El rol no puede ser nulo");
        }
    }

    private void validateEstado() {
        if (estado == null) {
            throw new DatosInvalidosException("El estado no puede ser nulo");
        }
    }

    public void habilitar() {
        this.estado = EstadoUsuario.HABILITADO;
        validateInfo();
    }

    public void suspender() {
        this.estado = EstadoUsuario.INACTIVO;
        validateInfo();
    }
}
