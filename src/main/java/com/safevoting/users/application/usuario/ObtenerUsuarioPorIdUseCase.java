package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ObtenerUsuarioPorIdUseCase {

    private final UsuarioRepository usuarioRepository;

    public Mono<Usuario> ejecutar(UUID id) {
        return usuarioRepository.findById(id)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(id)));
    }
}
