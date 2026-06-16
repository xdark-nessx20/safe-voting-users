package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.shared.Email;
import reactor.core.publisher.Mono;

public interface OtpRepository {

    Mono<Otp> save(Otp otp);

    Mono<Otp> findByEmailAndEstado(Email email, EstadoOtp estado);

    Mono<Otp> update(Otp otp);

    Mono<Void> invalidarExpirados();
}
