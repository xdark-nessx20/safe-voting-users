package com.safevoting.users.application.auth;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AsignarGestorUseCase {

    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<GestorElectoral> ejecutar(String documentoStr, AlcanceOperacion alcance) {
        DocumentoIdentidad documento = DocumentoIdentidad.builder().valor(documentoStr).build();

        return usuarioRepository.findByDocumento(documento)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoStr)))
                .flatMap(usuario -> {
                    usuario.asignarRolGestor();
                    return usuarioRepository.save(usuario);
                })
                .flatMap(usuarioGuardado -> {
                    GestorElectoral gestor = GestorElectoral.fromUsuario(usuarioGuardado, alcance);
                    gestor.validateInfo();
                    return gestorElectoralRepository.save(gestor);
                });
    }
}
