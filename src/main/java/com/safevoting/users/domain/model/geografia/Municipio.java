package com.safevoting.users.domain.model.geografia;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
public class Municipio {

    private UUID id;
    private String nombre;
    private Departamento departamento;

    public Municipio(UUID id, String nombre, Departamento departamento) {
        this.id = id;
        this.nombre = nombre;
        this.departamento = departamento;
    }

    public static class MunicipioBuilder {
        public Municipio build() {
            Municipio m = new Municipio(id, nombre, departamento);
            m.validateInfo();
            return m;
        }
    }

    public void validateInfo() {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException("El nombre del municipio no puede estar vacío");
        }
        if (departamento == null) {
            throw new DatosInvalidosException("El departamento no puede ser nulo");
        }
    }
}
