package com.safevoting.users.domain.shared;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class DocumentoIdentidad {

    private static final String PATTERN = "\\d{5,15}";

    private String valor;

    @Builder
    public DocumentoIdentidad(String valor) {
        this.valor = valor;
        validateInfo();
    }

    public void validateInfo() {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El documento de identidad no puede estar vacío");
        }
        if (!valor.matches(PATTERN)) {
            throw new DatosInvalidosException("El documento de identidad debe contener solo dígitos y tener entre 5 y 15 caracteres");
        }
    }
}
