package com.safevoting.users.infrastructure.adapter.out.persistence.inscripcion;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.SolicitudCambioInscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SolicitudCambioInscripcionAdapter implements SolicitudCambioInscripcionRepository {

    private static final Logger log = LoggerFactory.getLogger(SolicitudCambioInscripcionAdapter.class);

    private final SolicitudCambioInscripcionReactiveRepository reactiveRepository;
    private final SolicitudCambioInscripcionPersistenceMapper mapper;
    private final MunicipioRepository municipioRepository;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<SolicitudCambioInscripcion> save(SolicitudCambioInscripcion solicitud) {
        SolicitudCambioInscripcionEntity entity = mapper.toEntity(solicitud);
        return reactiveRepository.save(entity)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<SolicitudCambioInscripcion> findById(UUID id) {
        return reactiveRepository.findById(id)
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<SolicitudCambioInscripcion> findPendienteByUsuarioId(UUID usuarioId) {
        return reactiveRepository.findFirstByUsuarioIdAndEstado(usuarioId, "PENDIENTE")
                .flatMap(this::toDomain);
    }

    @Override
    public Flux<SolicitudCambioInscripcion> findPendientesByMunicipioDestinoId(UUID municipioId, long offset, int limit) {
        return databaseClient.sql("""
                    SELECT * FROM solicitudes_cambio_inscripcion
                    WHERE municipio_destino_id = :municipioId AND estado = 'PENDIENTE'
                    ORDER BY fecha_solicitud ASC
                    LIMIT :limit OFFSET :offset
                    """)
                .bind("municipioId", municipioId)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> mapper.toEntity(row))
                .all()
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Long> countPendientesByMunicipioDestinoId(UUID municipioId) {
        return reactiveRepository.countByMunicipioDestinoIdAndEstado(municipioId, "PENDIENTE");
    }

    @Override
    public Flux<SolicitudCambioInscripcion> findPendientesByDepartamentoDestinoId(UUID departamentoId, long offset, int limit) {
        return databaseClient.sql("""
                    SELECT s.* FROM solicitudes_cambio_inscripcion s
                    JOIN municipios m ON s.municipio_destino_id = m.id
                    WHERE m.departamento_id = :departamentoId AND s.estado = 'PENDIENTE'
                    ORDER BY s.fecha_solicitud ASC
                    LIMIT :limit OFFSET :offset
                    """)
                .bind("departamentoId", departamentoId)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> mapper.toEntity(row))
                .all()
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Long> countPendientesByDepartamentoDestinoId(UUID departamentoId) {
        return databaseClient.sql("""
                    SELECT COUNT(*) AS total
                    FROM solicitudes_cambio_inscripcion s
                    JOIN municipios m ON s.municipio_destino_id = m.id
                    WHERE m.departamento_id = :departamentoId AND s.estado = 'PENDIENTE'
                    """)
                .bind("departamentoId", departamentoId)
                .map((row, meta) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Flux<SolicitudCambioInscripcion> findAllPendientes(long offset, int limit) {
        return databaseClient.sql("""
                    SELECT * FROM solicitudes_cambio_inscripcion
                    WHERE estado = 'PENDIENTE'
                    ORDER BY fecha_solicitud ASC
                    LIMIT :limit OFFSET :offset
                    """)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> mapper.toEntity(row))
                .all()
                .flatMap(this::toDomain);
    }

    @Override
    public Mono<Long> countAllPendientes() {
        return databaseClient.sql("SELECT COUNT(*) AS total FROM solicitudes_cambio_inscripcion WHERE estado = 'PENDIENTE'")
                .map((row, meta) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> cancelarPendientesPorUsuarioId(UUID usuarioId, String motivo) {
        return databaseClient.sql("""
                    UPDATE solicitudes_cambio_inscripcion
                    SET estado = 'CANCELADA', motivo_rechazo = :motivo, fecha_resolucion = :ahora
                    WHERE usuario_id = :usuarioId AND estado = 'PENDIENTE'
                    """)
                .bind("motivo", motivo)
                .bind("ahora", Instant.now())
                .bind("usuarioId", usuarioId)
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    @Override
    public Mono<Long> cancelarPendientesPorMunicipioDestinoId(UUID municipioId, String motivo) {
        return databaseClient.sql("""
                    UPDATE solicitudes_cambio_inscripcion
                    SET estado = 'CANCELADA', motivo_rechazo = :motivo, fecha_resolucion = :ahora
                    WHERE municipio_destino_id = :municipioId AND estado = 'PENDIENTE'
                    """)
                .bind("motivo", motivo)
                .bind("ahora", Instant.now())
                .bind("municipioId", municipioId)
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    @Override
    public Mono<Long> update(SolicitudCambioInscripcion solicitud) {
        SolicitudCambioInscripcionEntity entity = mapper.toEntity(solicitud);

        var spec = databaseClient.sql("""
                    UPDATE solicitudes_cambio_inscripcion
                    SET estado = :estado, gestor_id = :gestorId, motivo_rechazo = :motivoRechazo,
                        fecha_resolucion = :fechaResolucion
                    WHERE id = :id AND estado = 'PENDIENTE'
                    """)
                .bind("estado", entity.getEstado())
                .bind("id", entity.getId());

        if (entity.getGestorId() != null) {
            spec = spec.bind("gestorId", entity.getGestorId());
        } else {
            spec = spec.bindNull("gestorId", UUID.class);
        }

        if (entity.getMotivoRechazo() != null) {
            spec = spec.bind("motivoRechazo", entity.getMotivoRechazo());
        } else {
            spec = spec.bindNull("motivoRechazo", String.class);
        }

        if (entity.getFechaResolucion() != null) {
            spec = spec.bind("fechaResolucion", entity.getFechaResolucion());
        } else {
            spec = spec.bindNull("fechaResolucion", Instant.class);
        }

        return spec.fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    private Mono<SolicitudCambioInscripcion> toDomain(SolicitudCambioInscripcionEntity entity) {
        return Mono.zip(
                municipioRepository.findById(entity.getMunicipioOrigenId())
                        .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(
                                entity.getMunicipioOrigenId().toString()))),
                municipioRepository.findById(entity.getMunicipioDestinoId())
                        .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(
                                entity.getMunicipioDestinoId().toString())))
        ).map(tuple -> mapper.toDomain(entity, tuple.getT1(), tuple.getT2()));
    }

}
