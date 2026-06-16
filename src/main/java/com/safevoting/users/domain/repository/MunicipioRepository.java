package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.geografia.Municipio;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MunicipioRepository {

    Mono<Municipio> findById(UUID id);

    Flux<Municipio> findAll();
}
