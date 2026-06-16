package com.safevoting.users.domain.model.geografia;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
public class Departamento {

    private UUID id;
    private String nombre;

    public Departamento(UUID id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public static class DepartamentoBuilder {
        public Departamento build() {
            Departamento d = new Departamento(id, nombre);
            d.validateInfo();
            return d;
        }
    }

    public void validateInfo() {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del departamento no puede estar vacío");
        }
    }
}
