package com.safevoting.users.infrastructure.adapter.in.rest.auth.mapper;

import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.domain.shared.Phone;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.dto.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(componentModel = "spring")
public interface AuthDtoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "municipio", ignore = true)
    @Mapping(target = "rol", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "telefono", source = "telefono")
    Usuario toUsuario(RegisterRequest request);

    default Usuario toUsuarioParaRegistro(RegisterRequest request) {
        Usuario base = toUsuario(request);
        return Usuario.builder()
                .nombre(base.getNombre())
                .email(base.getEmail())
                .telefono(base.getTelefono())
                .documento(base.getDocumento())
                .municipio(new Municipio(request.municipioId(), "", null))
                .rol(Rol.VOTANTE)
                .build();
    }

    default Email mapEmail(String valor) {
        return Email.builder().valor(valor).build();
    }

    default DocumentoIdentidad mapDocumento(String valor) {
        return DocumentoIdentidad.builder().valor(valor).build();
    }

    default Phone mapTelefono(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return Phone.builder().valor(valor).build();
    }
}
