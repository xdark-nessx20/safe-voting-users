package com.safevoting.users.domain.shared;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class Email {

    private String valor;

    public Email(String valor) {
        this.valor = valor;
        validateInfo();
    }

    public static class EmailBuilder {
        public Email build() {
            Email email = new Email(valor);
            email.validateInfo();
            return email;
        }
    }

    public void validateInfo() {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El email no puede estar vacío");
        }
        if (!valor.contains("@")) {
            throw new DatosInvalidosException("El email debe contener '@'");
        }
        int atIndex = valor.indexOf('@');
        if (atIndex == 0 || atIndex == valor.length() - 1) {
            throw new DatosInvalidosException("El email tiene un formato inválido");
        }
        String dominio = valor.substring(atIndex + 1);
        if (!dominio.contains(".")) {
            throw new DatosInvalidosException("El email tiene un formato inválido");
        }
    }
}
