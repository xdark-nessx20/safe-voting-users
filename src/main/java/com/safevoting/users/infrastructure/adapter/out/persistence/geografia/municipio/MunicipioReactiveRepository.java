package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.municipio;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface MunicipioReactiveRepository extends ReactiveCrudRepository<MunicipioEntity, UUID> {

    Flux<MunicipioEntity> findByDepartamentoId(UUID departamentoId);
}
