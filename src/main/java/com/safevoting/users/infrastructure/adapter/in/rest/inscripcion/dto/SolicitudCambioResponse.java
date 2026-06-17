package com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto;

import java.time.Instant;
import java.util.UUID;

public record SolicitudCambioResponse(
        UUID id,
        UUID usuarioId,
        String nombreVotante,
        String documentoVotante,
        String municipioOrigenNombre,
        String municipioDestinoNombre,
        String motivo,
        String estado,
        String motivoRechazo,
        Instant fechaSolicitud,
        Instant fechaResolucion
) {}
