package com.safevoting.users.domain.shared;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class Email {

    private static final String PATTERN = "^[^@]+@[^@]+\\.[^@]+$";

    private String valor;

    @Builder
    public Email(String valor) {
        this.valor = valor;
        validateInfo();
    }

    public void validateInfo() {
        if (valor == null || !valor.matches(PATTERN)) {
            throw new DatosInvalidosException("El formato del email no es válido");
        }
    }
}
