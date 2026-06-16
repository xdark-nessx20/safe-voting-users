package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.departamento;

import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepartamentoRepositoryAdapter implements DepartamentoRepository {

    private final DepartamentoReactiveRepository reactiveRepository;
    private final DepartamentoPersistenceMapper mapper;

    @Override
    public Mono<Departamento> findById(UUID id) {
        return reactiveRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Flux<Departamento> findAll() {
        return reactiveRepository.findAll().map(mapper::toDomain);
    }
}
