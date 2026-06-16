package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.municipio;

import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MunicipioPersistenceMapper {

    @Mapping(target = "departamentoId", source = "departamento.id")
    MunicipioEntity toEntity(Municipio municipio);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "nombre", source = "entity.nombre")
    @Mapping(target = "departamento", source = "departamento")
    Municipio toDomain(MunicipioEntity entity, Departamento departamento);

    default MunicipioEntity toEntity(io.r2dbc.spi.Row row) {
        return MunicipioEntity.builder()
                .id(row.get("id", UUID.class))
                .nombre(row.get("nombre", String.class))
                .departamentoId(row.get("departamento_id", UUID.class))
                .build();
    }

    default Departamento toDepartamento(io.r2dbc.spi.Row row) {
        return Departamento.builder()
                .id(row.get("d_id", UUID.class))
                .nombre(row.get("d_nombre", String.class))
                .build();
    }
}
