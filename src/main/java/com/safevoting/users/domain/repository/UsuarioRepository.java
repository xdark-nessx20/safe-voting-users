package com.safevoting.users.domain.repository;

import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsuarioRepository {

    Mono<Usuario> findById(UUID id);

    Mono<Usuario> findByEmail(Email email);

    Mono<Usuario> findByDocumento(DocumentoIdentidad documento);

    Mono<Usuario> save(Usuario usuario);

    Flux<Usuario> findByMunicipioId(UUID municipioId, int limit, long offset);

    Mono<Long> countByMunicipioId(UUID municipioId);

    Flux<Usuario> findByDepartamentoId(UUID departamentoId, int limit, long offset);

    Mono<Long> updateEstadoBatch(UUID municipioId, EstadoUsuario nuevoEstado);

    Mono<Long> updateEstadoBatchByDepartamento(UUID departamentoId, EstadoUsuario nuevoEstado);

    Mono<Long> updateEstadoBatchNacional(EstadoUsuario nuevoEstado);
}
