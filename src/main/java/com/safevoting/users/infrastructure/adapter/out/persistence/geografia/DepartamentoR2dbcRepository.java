package com.safevoting.users.infrastructure.adapter.out.persistence.geografia;

import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.repository.DepartamentoRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class DepartamentoR2dbcRepository implements DepartamentoRepository {

    private final DatabaseClient databaseClient;

    public DepartamentoR2dbcRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Departamento> findById(UUID id) {
        return databaseClient.sql("SELECT id, nombre FROM departamento WHERE id = :id")
                .bind("id", id)
                .map(row -> Departamento.builder()
                        .id(row.get("id", UUID.class))
                        .nombre(row.get("nombre", String.class))
                        .build())
                .one();
    }

    @Override
    public Flux<Departamento> findAll() {
        return databaseClient.sql("SELECT id, nombre FROM departamento")
                .map(row -> Departamento.builder()
                        .id(row.get("id", UUID.class))
                        .nombre(row.get("nombre", String.class))
                        .build())
                .all();
    }
}
