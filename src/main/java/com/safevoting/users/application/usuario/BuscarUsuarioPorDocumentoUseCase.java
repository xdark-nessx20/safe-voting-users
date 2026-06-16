package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class BuscarUsuarioPorDocumentoUseCase {

    private final UsuarioRepository usuarioRepository;

    public Mono<Usuario> ejecutar(String documento, GestorElectoral gestor) {
        DocumentoIdentidad doc = DocumentoIdentidad.builder().valor(documento).build();

        return usuarioRepository.findByDocumento(doc)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento)))
                .filter(objetivo -> !objetivo.esGestor())
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento)))
                .filter(objetivo -> gestor.cubre(objetivo.getMunicipio()))
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documento)));
    }
}
