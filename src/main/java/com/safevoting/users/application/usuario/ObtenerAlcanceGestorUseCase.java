package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerAlcanceGestorUseCase {

    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<GestorElectoral> ejecutar(UUID usuarioId) {
        return gestorElectoralRepository.findByUsuarioId(usuarioId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(usuarioId)));
    }
}
