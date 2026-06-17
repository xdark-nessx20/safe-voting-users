package com.safevoting.users.domain.model.geografia;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Municipio {

    private UUID id;
    private String nombre;
    private Departamento departamento;

    public void validateInfo() {
        if (nombreInvalido()) {
            throw new DatosInvalidosException("El nombre del municipio no puede estar vacío");
        }
        if (departamento == null) {
            throw new DatosInvalidosException("El departamento no puede ser nulo");
        }
    }

    public boolean equals(Municipio otroMunicipio){
        return this.id.equals(otroMunicipio.id);
    }

    private boolean nombreInvalido(){
        return nombre == null || nombre.isBlank();
    }
}
