package com.safevoting.users.infrastructure.adapter.out.persistence.geografia;

import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.repository.MunicipioRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class MunicipioR2dbcRepository implements MunicipioRepository {

    private final DatabaseClient databaseClient;

    public MunicipioR2dbcRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Municipio> findById(UUID id) {
        return databaseClient.sql("""
                    SELECT m.id, m.nombre, m.departamento_id,
                           d.id AS d_id, d.nombre AS d_nombre
                    FROM municipio m
                    JOIN departamento d ON m.departamento_id = d.id
                    WHERE m.id = :id
                    """)
                .bind("id", id)
                .map(row -> {
                    Departamento depto = Departamento.builder()
                            .id(row.get("d_id", UUID.class))
                            .nombre(row.get("d_nombre", String.class))
                            .build();
                    return Municipio.builder()
                            .id(row.get("id", UUID.class))
                            .nombre(row.get("nombre", String.class))
                            .departamento(depto)
                            .build();
                })
                .one();
    }

    @Override
    public Flux<Municipio> findAll() {
        return databaseClient.sql("""
                    SELECT m.id, m.nombre, m.departamento_id,
                           d.id AS d_id, d.nombre AS d_nombre
                    FROM municipio m
                    JOIN departamento d ON m.departamento_id = d.id
                    """)
                .map(row -> {
                    Departamento depto = Departamento.builder()
                            .id(row.get("d_id", UUID.class))
                            .nombre(row.get("d_nombre", String.class))
                            .build();
                    return Municipio.builder()
                            .id(row.get("id", UUID.class))
                            .nombre(row.get("nombre", String.class))
                            .departamento(depto)
                            .build();
                })
                .all();
    }
}
