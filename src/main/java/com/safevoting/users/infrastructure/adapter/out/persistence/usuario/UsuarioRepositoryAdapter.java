package com.safevoting.users.infrastructure.adapter.out.persistence.usuario;

import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
}
