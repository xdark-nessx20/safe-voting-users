package com.safevoting.users.domain.shared;

import com.safevoting.users.domain.model.exception.comun.DatosInvalidosException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
public class DocumentoIdentidad {

    private String valor;

    public DocumentoIdentidad(String valor) {
        this.valor = valor;
        validateInfo();
    }

    public static class DocumentoIdentidadBuilder {
        public DocumentoIdentidad build() {
            DocumentoIdentidad doc = new DocumentoIdentidad(valor);
            doc.validateInfo();
            return doc;
        }
    }

    public void validateInfo() {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El documento de identidad no puede estar vacío");
        }
        if (!valor.matches("\\d+")) {
            throw new DatosInvalidosException("El documento de identidad debe contener solo dígitos");
        }
        if (valor.length() < 5 || valor.length() > 15) {
            throw new DatosInvalidosException("El documento de identidad debe tener entre 5 y 15 dígitos");
        }
    }
}
