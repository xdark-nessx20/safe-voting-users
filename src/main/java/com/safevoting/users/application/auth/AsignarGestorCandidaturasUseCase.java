package com.safevoting.users.application.auth;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AsignarGestorCandidaturasUseCase {

    private final UsuarioRepository usuarioRepository;

    public Mono<Usuario> ejecutar(String documentoStr) {
        DocumentoIdentidad documento = DocumentoIdentidad.builder().valor(documentoStr).build();

        return usuarioRepository.findByDocumento(documento)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoStr)))
                .flatMap(usuario -> {
                    usuario.asignarRolGestorCandidaturas();
                    return usuarioRepository.save(usuario);
                });
    }
}
