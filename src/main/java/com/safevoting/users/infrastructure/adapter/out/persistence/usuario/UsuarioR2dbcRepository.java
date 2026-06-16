package com.safevoting.users.infrastructure.adapter.out.persistence.usuario;

import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class UsuarioR2dbcRepository implements UsuarioRepository {

    private final DatabaseClient databaseClient;

    public UsuarioR2dbcRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Usuario> findByEmail(Email email) {
        return databaseClient.sql("""
                    SELECT u.id, u.nombre, u.email, u.telefono, u.documento,
                           u.rol, u.estado, u.created_at,
                           m.id AS m_id, m.nombre AS m_nombre,
                           d.id AS d_id, d.nombre AS d_nombre
                    FROM usuario u
                    JOIN municipio m ON u.municipio_id = m.id
                    JOIN departamento d ON m.departamento_id = d.id
                    WHERE u.email = :email
                    """)
                .bind("email", email.getValor())
                .map((io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) -> mapToUsuario(row))
                .one();
    }

    @Override
    public Mono<Usuario> findByDocumento(DocumentoIdentidad documento) {
        return databaseClient.sql("""
                    SELECT u.id, u.nombre, u.email, u.telefono, u.documento,
                           u.rol, u.estado, u.created_at,
                           m.id AS m_id, m.nombre AS m_nombre,
                           d.id AS d_id, d.nombre AS d_nombre
                    FROM usuario u
                    JOIN municipio m ON u.municipio_id = m.id
                    JOIN departamento d ON m.departamento_id = d.id
                    WHERE u.documento = :documento
                    """)
                .bind("documento", documento.getValor())
                .map((io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) -> mapToUsuario(row))
                .one();
    }

    @Override
    public Mono<Usuario> save(Usuario usuario) {
        UUID id = usuario.getId() != null ? usuario.getId() : UUID.randomUUID();
        Instant createdAt = usuario.getCreatedAt() != null ? usuario.getCreatedAt() : Instant.now();

        return databaseClient.sql("""
                    INSERT INTO usuario (id, nombre, email, telefono, documento, municipio_id, rol, estado, created_at)
                    VALUES (:id, :nombre, :email, :telefono, :documento, :municipio_id, :rol, :estado, :created_at)
                    """)
                .bind("id", id)
                .bind("nombre", usuario.getNombre())
                .bind("email", usuario.getEmail().getValor())
                .bind("telefono", usuario.getTelefono())
                .bind("documento", usuario.getDocumento().getValor())
                .bind("municipio_id", usuario.getMunicipio().getId())
                .bind("rol", usuario.getRol().name())
                .bind("estado", usuario.getEstado().name())
                .bind("created_at", createdAt)
                .then()
                .thenReturn(Usuario.builder()
                        .id(id)
                        .nombre(usuario.getNombre())
                        .email(usuario.getEmail())
                        .telefono(usuario.getTelefono())
                        .documento(usuario.getDocumento())
                        .municipio(usuario.getMunicipio())
                        .rol(usuario.getRol())
                        .estado(usuario.getEstado())
                        .createdAt(createdAt)
                        .build());
    }

    private Usuario mapToUsuario(io.r2dbc.spi.Row row) {
        Departamento depto = Departamento.builder()
                .id(row.get("d_id", UUID.class))
                .nombre(row.get("d_nombre", String.class))
                .build();
        Municipio municipio = Municipio.builder()
                .id(row.get("m_id", UUID.class))
                .nombre(row.get("m_nombre", String.class))
                .departamento(depto)
                .build();
        return Usuario.builder()
                .id(row.get("id", UUID.class))
                .nombre(row.get("nombre", String.class))
                .email(Email.builder().valor(row.get("email", String.class)).build())
                .telefono(row.get("telefono", String.class))
                .documento(DocumentoIdentidad.builder().valor(row.get("documento", String.class)).build())
                .municipio(municipio)
                .rol(Rol.valueOf(row.get("rol", String.class)))
                .estado(EstadoUsuario.valueOf(row.get("estado", String.class)))
                .createdAt(row.get("created_at", Instant.class))
                .build();
    }
}
