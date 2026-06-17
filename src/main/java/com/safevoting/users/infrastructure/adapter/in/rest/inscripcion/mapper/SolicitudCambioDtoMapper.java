package com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.mapper;

import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto.SolicitudCambioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SolicitudCambioDtoMapper {

    @Mapping(target = "id", source = "solicitud.id")
    @Mapping(target = "usuarioId", source = "solicitud.usuarioId")
    @Mapping(target = "nombreVotante", source = "votante.nombre")
    @Mapping(target = "documentoVotante", source = "votante.documento.valor")
    @Mapping(target = "municipioOrigenNombre", source = "solicitud.municipioOrigen.nombre")
    @Mapping(target = "municipioDestinoNombre", source = "solicitud.municipioDestino.nombre")
    @Mapping(target = "motivo", source = "solicitud.motivo")
    @Mapping(target = "estado", source = "solicitud.estado")
    @Mapping(target = "motivoRechazo", source = "solicitud.motivoRechazo")
    @Mapping(target = "fechaSolicitud", source = "solicitud.fechaSolicitud")
    @Mapping(target = "fechaResolucion", source = "solicitud.fechaResolucion")
    SolicitudCambioResponse toResponse(SolicitudCambioInscripcion solicitud, Usuario votante);
}
