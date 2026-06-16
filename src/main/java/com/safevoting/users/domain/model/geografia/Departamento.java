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
@EqualsAndHashCode
public class Departamento {

    private UUID id;
    private String nombre;

    public void validateInfo() {
        if (nombreInvalido()) {
            throw new DatosInvalidosException("El nombre del departamento no puede estar vacío");
        }
    }

    private boolean nombreInvalido(){
        return nombre == null || nombre.isBlank();
    }
}
