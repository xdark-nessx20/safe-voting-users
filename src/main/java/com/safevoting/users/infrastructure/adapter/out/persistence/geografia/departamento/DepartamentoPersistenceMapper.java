package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.departamento;

import com.safevoting.users.domain.model.geografia.Departamento;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DepartamentoPersistenceMapper {

    DepartamentoEntity toEntity(Departamento departamento);

    Departamento toDomain(DepartamentoEntity entity);
}
