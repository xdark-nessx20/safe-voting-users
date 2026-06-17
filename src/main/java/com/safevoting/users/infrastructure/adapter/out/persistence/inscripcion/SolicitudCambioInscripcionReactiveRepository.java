package com.safevoting.users.infrastructure.adapter.out.persistence.inscripcion;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SolicitudCambioInscripcionReactiveRepository
        extends ReactiveCrudRepository<SolicitudCambioInscripcionEntity, UUID> {

    Mono<SolicitudCambioInscripcionEntity> findFirstByUsuarioIdAndEstado(UUID usuarioId, String estado);

    Flux<SolicitudCambioInscripcionEntity> findByMunicipioDestinoIdAndEstado(UUID municipioId, String estado);

    Mono<Long> countByMunicipioDestinoIdAndEstado(UUID municipioId, String estado);
}
