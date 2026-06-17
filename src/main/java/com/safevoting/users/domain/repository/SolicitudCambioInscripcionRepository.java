package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SolicitudCambioInscripcionRepository {

    Mono<SolicitudCambioInscripcion> save(SolicitudCambioInscripcion solicitud);

    Mono<SolicitudCambioInscripcion> findById(UUID id);

    Mono<SolicitudCambioInscripcion> findPendienteByUsuarioId(UUID usuarioId);

    Flux<SolicitudCambioInscripcion> findPendientesByMunicipioDestinoId(UUID municipioId, long offset, int limit);

    Mono<Long> countPendientesByMunicipioDestinoId(UUID municipioId);

    Flux<SolicitudCambioInscripcion> findPendientesByDepartamentoDestinoId(UUID departamentoId, long offset, int limit);

    Mono<Long> countPendientesByDepartamentoDestinoId(UUID departamentoId);

    Flux<SolicitudCambioInscripcion> findAllPendientes(long offset, int limit);

    Mono<Long> countAllPendientes();

    Mono<Long> cancelarPendientesPorUsuarioId(UUID usuarioId, String motivo);

    Mono<Long> cancelarPendientesPorMunicipioDestinoId(UUID municipioId, String motivo);

    Mono<Long> update(SolicitudCambioInscripcion solicitud);
}
