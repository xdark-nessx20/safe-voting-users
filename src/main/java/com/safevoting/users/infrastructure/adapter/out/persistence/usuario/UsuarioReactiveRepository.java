package com.safevoting.users.infrastructure.adapter.out.persistence.usuario;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsuarioReactiveRepository extends ReactiveCrudRepository<UsuarioEntity, UUID> {

    Mono<UsuarioEntity> findByEmail(String email);

    Mono<UsuarioEntity> findByDocumento(String documento);

    Flux<UsuarioEntity> findByMunicipioId(UUID municipioId);
}
