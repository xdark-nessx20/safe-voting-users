package com.safevoting.users.infrastructure.adapter.out.persistence.otp;

import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.shared.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OtpRepositoryAdapter implements OtpRepository {

    private final OtpReactiveRepository reactiveRepository;
    private final OtpPersistenceMapper mapper;
    private final DatabaseClient databaseClient;

    @Override
    public Mono<Otp> save(Otp otp) {
        OtpEntity entity = mapper.toEntity(otp);
        return reactiveRepository.save(entity)
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Otp> findByEmailAndEstado(Email email, EstadoOtp estado) {
        return reactiveRepository
                .findFirstByEmailAndEstadoOrderByExpiracionDesc(email.getValor(), estado.name())
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Otp> update(Otp otp) {
        return reactiveRepository.save(mapper.toEntity(otp))
                .map(mapper::toDomain);
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
}
