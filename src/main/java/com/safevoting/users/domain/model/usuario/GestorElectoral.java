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

    public static GestorElectoral fromUsuario(Usuario usuario, AlcanceOperacion alcance) {
        return GestorElectoral.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .documento(usuario.getDocumento())
                .municipio(usuario.getMunicipio())
                .rol(usuario.getRol())
                .estado(usuario.getEstado())
                .createdAt(usuario.getCreatedAt())
                .alcance(alcance)
                .build();
    }

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
                    "El gestor con alcance %s no tiene jurisdicción sobre el municipio objetivo".formatted(alcance));
        }
    }

    public void validarAlcanceNoSuperado(AlcanceOperacion alcanceSolicitado) {
        if (alcanceMunicipalSuperado(alcanceSolicitado)) {
            throw new AlcanceInsuficienteException(
                    "Un gestor con alcance MUNICIPAL solo puede realizar operaciones con alcance MUNICIPAL");
        }
        if (alcanceDepartamentalSuperado(alcanceSolicitado)) {
            throw new AlcanceInsuficienteException(
                    "Un gestor con alcance DEPARTAMENTAL no puede realizar operaciones con alcance NACIONAL");
        }
    }

    private boolean alcanceMunicipalSuperado(AlcanceOperacion alcanceSolicitado){
        return alcance == AlcanceOperacion.MUNICIPAL && alcanceSolicitado != AlcanceOperacion.MUNICIPAL;
    }

    private boolean alcanceDepartamentalSuperado(AlcanceOperacion alcanceSolicitado){
        return alcance == AlcanceOperacion.DEPARTAMENTAL && alcanceSolicitado == AlcanceOperacion.NACIONAL;
    }

    public static void validarNoEsGestor(Usuario objetivo) {
        if (objetivo.esGestor()) {
            throw new GestorNoModificableException();
        }
    }
}
