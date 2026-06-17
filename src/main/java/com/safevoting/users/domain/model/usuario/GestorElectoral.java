package com.safevoting.users.domain.model.usuario;

import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.exception.usuario.AlcanceInsuficienteException;
import com.safevoting.users.domain.exception.usuario.GestorNoModificableException;
import com.safevoting.users.domain.model.geografia.Municipio;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GestorElectoral extends Usuario {

    private AlcanceOperacion alcance;

    public void validateInfo() {
        super.validateInfo();
        if (!esGestor()) {
            throw new DatosInvalidosException("El usuario asociado debe tener rol GESTOR_ELECTORAL");
        }
        if (alcance == null) {
            throw new DatosInvalidosException("El gestor electoral debe tener un alcance de operación asignado");
        }
    }

    public boolean cubre(Municipio municipioObjetivo) {
        return alcance.cubre(getMunicipio(), municipioObjetivo);
    }

    public void validarAlcance(Municipio municipioObjetivo) {
        if (!cubre(municipioObjetivo)) {
            throw new AlcanceInsuficienteException(
                    "El gestor con alcance " + alcance + " no tiene jurisdicción sobre el municipio objetivo");
        }
    }

    public void validarAlcanceNoSuperado(AlcanceOperacion alcanceSolicitado) {
        if (alcance == AlcanceOperacion.MUNICIPAL && alcanceSolicitado != AlcanceOperacion.MUNICIPAL) {
            throw new AlcanceInsuficienteException(
                    "Un gestor con alcance MUNICIPAL solo puede realizar operaciones con alcance MUNICIPAL");
        }
        if (alcance == AlcanceOperacion.DEPARTAMENTAL && alcanceSolicitado == AlcanceOperacion.NACIONAL) {
            throw new AlcanceInsuficienteException(
                    "Un gestor con alcance DEPARTAMENTAL no puede realizar operaciones con alcance NACIONAL");
        }
    }

    public static void validarNoEsGestor(Usuario objetivo) {
        if (objetivo.esGestor()) {
            throw new GestorNoModificableException();
        }
    }
}
