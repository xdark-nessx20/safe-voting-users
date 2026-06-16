package com.safevoting.users.infrastructure.adapter.out.persistence.otp;

import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.shared.Email;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Repository
public class OtpR2dbcRepository implements OtpRepository {

    private final DatabaseClient databaseClient;

    public OtpR2dbcRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Otp> save(Otp otp) {
        UUID id = otp.getId() != null ? otp.getId() : UUID.randomUUID();

        return databaseClient.sql("""
                    INSERT INTO otp (id, email, codigo, expiracion, intentos, estado)
                    VALUES (:id, :email, :codigo, :expiracion, :intentos, :estado)
                    """)
                .bind("id", id)
                .bind("email", otp.getEmail().getValor())
                .bind("codigo", otp.getCodigo())
                .bind("expiracion", otp.getExpiracion())
                .bind("intentos", otp.getIntentos())
                .bind("estado", otp.getEstado().name())
                .then()
                .thenReturn(Otp.builder()
                        .id(id)
                        .email(otp.getEmail())
                        .codigo(otp.getCodigo())
                        .expiracion(otp.getExpiracion())
                        .intentos(otp.getIntentos())
                        .estado(otp.getEstado())
                        .build());
    }

    @Override
    public Mono<Otp> findByEmailAndEstado(Email email, EstadoOtp estado) {
        return databaseClient.sql("""
                    SELECT id, email, codigo, expiracion, intentos, estado
                    FROM otp
                    WHERE email = :email AND estado = :estado
                    ORDER BY expiracion DESC
                    LIMIT 1
                    """)
                .bind("email", email.getValor())
                .bind("estado", estado.name())
                .map((io.r2dbc.spi.Row row, io.r2dbc.spi.RowMetadata meta) -> mapToOtp(row))
                .one();
    }

    @Override
    public Mono<Otp> update(Otp otp) {
        return databaseClient.sql("""
                    UPDATE otp
                    SET intentos = :intentos, estado = :estado
                    WHERE id = :id
                    """)
                .bind("intentos", otp.getIntentos())
                .bind("estado", otp.getEstado().name())
                .bind("id", otp.getId())
                .then()
                .thenReturn(otp);
    }

    @Override
    public Mono<Void> invalidarExpirados() {
        return databaseClient.sql("""
                    UPDATE otp
                    SET estado = :nuevoEstado
                    WHERE estado = :estadoActivo AND expiracion < :ahora
                    """)
                .bind("nuevoEstado", EstadoOtp.INVALIDADO.name())
                .bind("estadoActivo", EstadoOtp.ACTIVO.name())
                .bind("ahora", Instant.now())
                .then();
    }

    private Otp mapToOtp(io.r2dbc.spi.Row row) {
        return Otp.builder()
                .id(row.get("id", UUID.class))
                .email(Email.builder().valor(row.get("email", String.class)).build())
                .codigo(row.get("codigo", String.class))
                .expiracion(row.get("expiracion", Instant.class))
                .intentos(row.get("intentos", Integer.class))
                .estado(EstadoOtp.valueOf(row.get("estado", String.class)))
                .build();
    }
}
