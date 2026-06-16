package com.safevoting.users.infrastructure.adapter.out.persistence.otp;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface OtpReactiveRepository extends ReactiveCrudRepository<OtpEntity, UUID> {

    Mono<OtpEntity> findFirstByEmailAndEstadoOrderByExpiracionDesc(String email, String estado);
}
