package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.usuario.GestorElectoral;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface GestorElectoralRepository {

    Mono<GestorElectoral> findByUsuarioId(UUID usuarioId);
}
