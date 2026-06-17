package com.safevoting.users.domain.model.usuario;

import com.safevoting.users.domain.model.geografia.Municipio;

import java.util.UUID;

public enum AlcanceOperacion {
    NACIONAL,
    DEPARTAMENTAL,
    MUNICIPAL;

    public boolean cubre(Municipio municipioGestor, Municipio municipioObjetivo) {
        if (this == NACIONAL) {
            return true;
        }
        if (this == DEPARTAMENTAL) {
            UUID deptoGestor = municipioGestor.getDepartamento().getId();
            UUID deptoObjetivo = municipioObjetivo.getDepartamento().getId();
            return deptoGestor.equals(deptoObjetivo);
        }
        return municipioGestor.getId().equals(municipioObjetivo.getId());
    }
}
