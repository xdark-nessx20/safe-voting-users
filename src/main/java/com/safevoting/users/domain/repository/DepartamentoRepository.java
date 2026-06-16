package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.geografia.Departamento;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface DepartamentoRepository {

    Mono<Departamento> findById(UUID id);

    Flux<Departamento> findAll();
}
