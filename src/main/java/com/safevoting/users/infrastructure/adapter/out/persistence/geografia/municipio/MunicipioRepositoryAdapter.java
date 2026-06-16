package com.safevoting.users.infrastructure.adapter.out.persistence.geografia.municipio;

import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MunicipioRepositoryAdapter implements MunicipioRepository {

    private static final String SELECT_JOIN = """
            SELECT m.id, m.nombre, m.departamento_id,
                   d.id AS d_id, d.nombre AS d_nombre
            FROM municipio m
            JOIN departamento d ON m.departamento_id = d.id
            """;

    private final MunicipioReactiveRepository reactiveRepository;
    private final MunicipioPersistenceMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Municipio> findById(UUID id) {
        return databaseClient.sql(SELECT_JOIN + "WHERE m.id = :id")
                .bind("id", id)
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toDepartamento(row)))
                .one();
    }

    @Override
    public Flux<Municipio> findAll() {
        return databaseClient.sql(SELECT_JOIN)
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toDepartamento(row)))
                .all();
    }

    @Override
    public Flux<Municipio> findByDepartmentId(UUID departamentoId) {
        return databaseClient.sql(SELECT_JOIN + "WHERE m.departamento_id = :departamentoId")
                .bind("departamentoId", departamentoId)
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toDepartamento(row)))
                .all();
    }
}
