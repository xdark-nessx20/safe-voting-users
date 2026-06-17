package com.safevoting.users.infrastructure.adapter.out.persistence.inscripcion;

import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.inscripcion.EstadoSolicitud;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SolicitudCambioInscripcionPersistenceMapper {

    @Mapping(target = "municipioOrigenId", source = "municipioOrigen.id")
    @Mapping(target = "municipioDestinoId", source = "municipioDestino.id")
    @Mapping(target = "estado", source = "estado")
    SolicitudCambioInscripcionEntity toEntity(SolicitudCambioInscripcion solicitud);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "usuarioId", source = "entity.usuarioId")
    @Mapping(target = "municipioOrigen", source = "municipioOrigen")
    @Mapping(target = "municipioDestino", source = "municipioDestino")
    @Mapping(target = "motivo", source = "entity.motivo")
    @Mapping(target = "estado", source = "entity.estado")
    @Mapping(target = "motivoRechazo", source = "entity.motivoRechazo")
    @Mapping(target = "gestorId", source = "entity.gestorId")
    @Mapping(target = "fechaSolicitud", source = "entity.fechaSolicitud")
    @Mapping(target = "fechaResolucion", source = "entity.fechaResolucion")
    SolicitudCambioInscripcion toDomain(SolicitudCambioInscripcionEntity entity,
                                         Municipio municipioOrigen,
                                         Municipio municipioDestino);

    default SolicitudCambioInscripcionEntity toEntity(io.r2dbc.spi.Row row) {
        return SolicitudCambioInscripcionEntity.builder()
                .id(row.get("id", UUID.class))
                .usuarioId(row.get("usuario_id", UUID.class))
                .municipioOrigenId(row.get("municipio_origen_id", UUID.class))
                .municipioDestinoId(row.get("municipio_destino_id", UUID.class))
                .motivo(row.get("motivo", String.class))
                .estado(row.get("estado", String.class))
                .motivoRechazo(row.get("motivo_rechazo", String.class))
                .gestorId(row.get("gestor_id", UUID.class))
                .fechaSolicitud(row.get("fecha_solicitud", Instant.class))
                .fechaResolucion(row.get("fecha_resolucion", Instant.class))
                .build();
    }

    default String mapEstadoSolicitud(EstadoSolicitud estado) {
        return estado.name();
    }

    default EstadoSolicitud mapEstadoSolicitud(String estado) {
        return EstadoSolicitud.valueOf(estado);
    }
}
