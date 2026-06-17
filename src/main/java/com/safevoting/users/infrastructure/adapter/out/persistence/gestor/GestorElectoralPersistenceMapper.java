package com.safevoting.users.infrastructure.adapter.out.persistence.gestor;

import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GestorElectoralPersistenceMapper {

    @Mapping(target = "usuarioId", source = "id")
    @Mapping(target = "alcanceOperacion", source = "alcance")
    GestorElectoralEntity toEntity(GestorElectoral gestor);

    @Mapping(target = "id", source = "usuario.id")
    @Mapping(target = "nombre", source = "usuario.nombre")
    @Mapping(target = "email", source = "usuario.email")
    @Mapping(target = "telefono", source = "usuario.telefono")
    @Mapping(target = "documento", source = "usuario.documento")
    @Mapping(target = "municipio", source = "usuario.municipio")
    @Mapping(target = "rol", source = "usuario.rol")
    @Mapping(target = "estado", source = "usuario.estado")
    @Mapping(target = "createdAt", source = "usuario.createdAt")
    @Mapping(target = "alcance", source = "entity.alcanceOperacion")
    GestorElectoral toDomain(GestorElectoralEntity entity, Usuario usuario);

    default String mapAlcanceOperacion(AlcanceOperacion alcance) {
        return alcance.name();
    }

    default AlcanceOperacion mapAlcanceOperacionToDomain(String alcance) {
        return AlcanceOperacion.valueOf(alcance);
    }
}
