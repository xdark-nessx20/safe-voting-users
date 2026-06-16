package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import reactor.core.publisher.Mono;

public interface UsuarioRepository {

    Mono<Usuario> findByEmail(Email email);

    Mono<Usuario> findByDocumento(DocumentoIdentidad documento);

    Mono<Usuario> save(Usuario usuario);
}
