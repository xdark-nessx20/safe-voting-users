package com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper;

import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioDtoMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "email", source = "email.valor")
    @Mapping(target = "documento", source = "documento.valor")
    @Mapping(target = "municipioNombre", source = "municipio.nombre")
    @Mapping(target = "departamentoNombre", source = "municipio.departamento.nombre")
    @Mapping(target = "estado", source = "estado")
    @Mapping(target = "rol", source = "rol")
    @Mapping(target = "telefono", source = "telefono.valor")
    UsuarioResponse toResponse(Usuario usuario);
}
