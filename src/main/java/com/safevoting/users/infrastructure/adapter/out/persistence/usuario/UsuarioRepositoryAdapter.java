package com.safevoting.users.infrastructure.adapter.out.persistence.usuario;

import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private static final String SELECT_JOIN = """
            SELECT u.id, u.nombre, u.email, u.telefono, u.documento, u.municipio_id,
                   u.rol, u.estado, u.created_at,
                   m.id AS m_id, m.nombre AS m_nombre,
                   d.id AS d_id, d.nombre AS d_nombre
            FROM usuario u
            JOIN municipio m ON u.municipio_id = m.id
            JOIN departamento d ON m.departamento_id = d.id
            """;

    private final UsuarioReactiveRepository reactiveRepository;
    private final UsuarioPersistenceMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Usuario> findById(UUID id) {
        return databaseClient.sql(SELECT_JOIN + "WHERE u.id = :id")
                .bind("id", id)
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toMunicipio(row)))
                .one();
    }

    @Override
    public Mono<Usuario> findByEmail(Email email) {
        return databaseClient.sql(SELECT_JOIN + "WHERE u.email = :email")
                .bind("email", email.getValor())
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toMunicipio(row)))
                .one();
    }

    @Override
    public Mono<Usuario> findByDocumento(DocumentoIdentidad documento) {
        return databaseClient.sql(SELECT_JOIN + "WHERE u.documento = :documento")
                .bind("documento", documento.getValor())
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toMunicipio(row)))
                .one();
    }

    @Override
    public Mono<Usuario> save(Usuario usuario) {
        UsuarioEntity entity = mapper.toEntity(usuario);
        return reactiveRepository.save(entity)
                .map(savedEntity -> mapper.toDomain(savedEntity, usuario.getMunicipio()));
    }

    @Override
    public Flux<Usuario> findByMunicipioId(UUID municipioId, int limit, long offset) {
        return databaseClient.sql(SELECT_JOIN + "WHERE u.municipio_id = :municipioId ORDER BY u.nombre LIMIT :limit OFFSET :offset")
                .bind("municipioId", municipioId)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toMunicipio(row)))
                .all();
    }

    @Override
    public Mono<Long> countByMunicipioId(UUID municipioId) {
        return databaseClient.sql("SELECT COUNT(*) AS total FROM usuario WHERE municipio_id = :municipioId")
                .bind("municipioId", municipioId)
                .map((row, meta) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Flux<Usuario> findByDepartamentoId(UUID departamentoId, int limit, long offset) {
        return databaseClient.sql(SELECT_JOIN + "WHERE m.departamento_id = :departamentoId ORDER BY u.nombre LIMIT :limit OFFSET :offset")
                .bind("departamentoId", departamentoId)
                .bind("limit", limit)
                .bind("offset", offset)
                .map((row, meta) -> mapper.toDomain(mapper.toEntity(row), mapper.toMunicipio(row)))
                .all();
    }

    @Override
    public Mono<Long> updateEstadoBatch(UUID municipioId, EstadoUsuario nuevoEstado) {
        return databaseClient.sql("""
                    UPDATE usuario
                    SET estado = :nuevoEstado
                    WHERE municipio_id = :municipioId
                      AND rol = 'VOTANTE'
                      AND estado != :nuevoEstado
                    """)
                .bind("nuevoEstado", nuevoEstado.name())
                .bind("municipioId", municipioId)
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    @Override
    public Mono<Long> updateEstadoBatchByDepartamento(UUID departamentoId, EstadoUsuario nuevoEstado) {
        return databaseClient.sql("""
                    UPDATE usuario
                    SET estado = :nuevoEstado
                    WHERE municipio_id IN (SELECT id FROM municipio WHERE departamento_id = :departamentoId)
                      AND rol = 'VOTANTE'
                      AND estado != :nuevoEstado
                    """)
                .bind("nuevoEstado", nuevoEstado.name())
                .bind("departamentoId", departamentoId)
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }

    @Override
    public Mono<Long> updateEstadoBatchNacional(EstadoUsuario nuevoEstado) {
        return databaseClient.sql("""
                    UPDATE usuario
                    SET estado = :nuevoEstado
                    WHERE rol = 'VOTANTE'
                      AND estado != :nuevoEstado
                    """)
                .bind("nuevoEstado", nuevoEstado.name())
                .fetch()
                .rowsUpdated()
                .map(Long::valueOf);
    }
}
