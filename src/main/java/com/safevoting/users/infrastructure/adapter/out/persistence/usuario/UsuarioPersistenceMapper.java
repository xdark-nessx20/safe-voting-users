package com.safevoting.users.infrastructure.adapter.out.persistence.usuario;

import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.domain.shared.Phone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UsuarioPersistenceMapper {

    @Mapping(target = "email", source = "email.valor")
    @Mapping(target = "telefono", source = "telefono.valor")
    @Mapping(target = "documento", source = "documento.valor")
    @Mapping(target = "municipioId", source = "municipio.id")
    @Mapping(target = "rol", source = "rol")
    @Mapping(target = "estado", source = "estado")
    UsuarioEntity toEntity(Usuario usuario);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "nombre", source = "entity.nombre")
    @Mapping(target = "email", source = "entity.email")
    @Mapping(target = "telefono", source = "entity.telefono")
    @Mapping(target = "documento", source = "entity.documento")
    @Mapping(target = "municipio", source = "municipio")
    @Mapping(target = "rol", source = "entity.rol")
    @Mapping(target = "estado", source = "entity.estado")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    Usuario toDomain(UsuarioEntity entity, Municipio municipio);

    default UsuarioEntity toEntity(io.r2dbc.spi.Row row) {
        return UsuarioEntity.builder()
                .id(row.get("id", UUID.class))
                .nombre(row.get("nombre", String.class))
                .email(row.get("email", String.class))
                .telefono(row.get("telefono", String.class))
                .documento(row.get("documento", String.class))
                .municipioId(row.get("municipio_id", UUID.class))
                .rol(row.get("rol", String.class))
                .estado(row.get("estado", String.class))
                .createdAt(row.get("created_at", java.time.Instant.class))
                .build();
    }

    default Municipio toMunicipio(io.r2dbc.spi.Row row) {
        Departamento depto = Departamento.builder()
                .id(row.get("d_id", UUID.class))
                .nombre(row.get("d_nombre", String.class))
                .build();
        return Municipio.builder()
                .id(row.get("m_id", UUID.class))
                .nombre(row.get("m_nombre", String.class))
                .departamento(depto)
                .build();
    }

    default Email mapEmailToDomain(String valor) {
        return Email.builder().valor(valor).build();
    }

    default Phone mapTelefonoToDomain(String valor) {
        return valor != null ? Phone.builder().valor(valor).build() : null;
    }

    default DocumentoIdentidad mapDocumentoToDomain(String valor) {
        return DocumentoIdentidad.builder().valor(valor).build();
    }
}
