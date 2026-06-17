package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class BuscarUsuarioPorDocumentoUseCase {

    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<Usuario> ejecutar(UUID actorId, String documento) {
        DocumentoIdentidad doc = DocumentoIdentidad.builder().valor(documento).build();

        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .flatMap(actor ->
                        Mono.just(actor)
                                .filter(Usuario::esAdmin)
                                .flatMap(a -> usuarioRepository.findByDocumento(doc)
                                        .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento))))
                                .switchIfEmpty(
                                        gestorElectoralRepository.findByUsuarioId(actorId)
                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                .flatMap(gestor ->
                                                        usuarioRepository.findByDocumento(doc)
                                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento)))
                                                                .filter(objetivo -> !objetivo.esGestor())
                                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento)))
                                                                .filter(objetivo -> gestor.cubre(objetivo.getMunicipio()))
                                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento))))
                                ));
    }
}
