package com.safevoting.users.infrastructure.adapter.out.persistence.gestor;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GestorElectoralReactiveRepository extends ReactiveCrudRepository<GestorElectoralEntity, UUID> {

    Mono<GestorElectoralEntity> findByUsuarioId(UUID usuarioId);
}
