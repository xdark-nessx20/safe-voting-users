package com.safevoting.users.domain.shared;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class Phone {

    private static final String PATTERN = "\\d{7,15}";

    private String valor;

    @Builder
    public Phone(String valor) {
        this.valor = valor;
        validateInfo();
    }

    public void validateInfo() {
        if (valor == null || valor.isBlank()) {
            return;
        }
        if (!valor.matches(PATTERN)) {
            throw new DatosInvalidosException("El teléfono debe contener solo dígitos y tener entre 7 y 15 caracteres");
        }
    }
}
