package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.departamento;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface DepartamentoReactiveRepository extends ReactiveCrudRepository<DepartamentoEntity, UUID> {
}
