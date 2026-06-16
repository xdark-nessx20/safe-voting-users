package com.safevoting.users.infrastructure.adapter.out.persistence.gestor;

import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GestorElectoralPersistenceMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "alcanceOperacion", source = "alcance")
    GestorElectoralEntity toEntity(GestorElectoral gestor);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "usuario", source = "usuario")
    @Mapping(target = "alcance", source = "entity.alcanceOperacion")
    GestorElectoral toDomain(GestorElectoralEntity entity, Usuario usuario);

    default String mapAlcance(AlcanceOperacion alcance) {
        return alcance.name();
    }

    default AlcanceOperacion mapAlcanceToDomain(String alcance) {
        return AlcanceOperacion.valueOf(alcance);
    }
}
