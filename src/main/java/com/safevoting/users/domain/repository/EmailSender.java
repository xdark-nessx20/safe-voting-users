package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.shared.Email;
import reactor.core.publisher.Mono;

public interface EmailSender {

    Mono<Void> enviarOtp(Email email, String codigo);
}
