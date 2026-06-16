package com.safevoting.users.infrastructure.adapter.out.persistence.otp;

import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.shared.Email;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OtpPersistenceMapper {

    @Mapping(target = "email", source = "email.valor")
    @Mapping(target = "estado", source = "estado")
    OtpEntity toEntity(Otp otp);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "codigo", source = "codigo")
    @Mapping(target = "expiracion", source = "expiracion")
    @Mapping(target = "intentos", source = "intentos")
    @Mapping(target = "estado", source = "estado")
    Otp toDomain(OtpEntity entity);

    default Email mapEmailToDomain(String valor) {
        return Email.builder().valor(valor).build();
    }
}
