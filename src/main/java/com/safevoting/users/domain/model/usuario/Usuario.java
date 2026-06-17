package com.safevoting.users.domain.model.usuario;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.domain.shared.Phone;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Usuario {

    private UUID id;
    private String nombre;
    private Email email;
    private Phone telefono;
    private DocumentoIdentidad documento;
    private Municipio municipio;
    private Rol rol;

    @lombok.Builder.Default
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @lombok.Builder.Default
    private Instant createdAt = Instant.now();

    public void validateInfo() {
        validateNombre();
        validateEmail();
        validateTelefono();
        validateDocumento();
        validateMunicipio();
        validateRol();
        validateEstado();
    }

    public boolean esActivo() {
        return this.estado == EstadoUsuario.ACTIVO;
    }

    public boolean esHabilitado() {
        return this.estado == EstadoUsuario.HABILITADO;
    }

    public boolean esInactivo() {
        return this.estado == EstadoUsuario.INACTIVO;
    }

    public boolean esGestor() {
        return this.rol == Rol.GESTOR_ELECTORAL;
    }

    public void habilitar() {
        if (!esActivo()) {
            throw new DatosInvalidosException("Solo se puede habilitar un usuario en estado ACTIVO");
        }
        this.estado = EstadoUsuario.HABILITADO;
    }

    public void inhabilitar() {
        if (!esHabilitado()) {
            throw new DatosInvalidosException("Solo se puede inhabilitar un usuario en estado HABILITADO");
        }
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void suspender() {
        if (esInactivo()) {
            throw new DatosInvalidosException("No se puede suspender un usuario en estado INACTIVO");
        }
        this.estado = EstadoUsuario.INACTIVO;
    }

    public void reactivar() {
        if (!esInactivo()) {
            throw new DatosInvalidosException("Solo se puede reactivar un usuario en estado INACTIVO");
        }
        this.estado = EstadoUsuario.ACTIVO;
    }

    public void setMunicipio(Municipio municipio) {
        this.municipio = municipio;
        validateMunicipio();
    }

    public void setTelefono(Phone newPhone) {
        newPhone.validateInfo();
        this.telefono = newPhone;
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

    private void validateTelefono() {
        if (telefono != null) {
            telefono.validateInfo();
        }
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
}
