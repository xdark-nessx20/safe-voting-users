package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.GestorNoModificableException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CambiarEstadoIndividualUseCase {

    private final UsuarioRepository usuarioRepository;

    public Mono<Usuario> ejecutar(String documentoObjetivo, EstadoUsuario nuevoEstado, GestorElectoral gestor) {
        DocumentoIdentidad documento = DocumentoIdentidad.builder().valor(documentoObjetivo).build();

        return usuarioRepository.findByDocumento(documento)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoObjetivo)))
                .filter(objetivo -> !objetivo.esGestor())
                .switchIfEmpty(Mono.error(new GestorNoModificableException()))
                .filter(objetivo -> gestor.cubre(objetivo.getMunicipio()))
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoObjetivo)))
                .flatMap(objetivo -> {
                    aplicarEstado(objetivo, nuevoEstado);
                    return usuarioRepository.save(objetivo);
                });
    }

    private void aplicarEstado(Usuario usuario, EstadoUsuario nuevoEstado) {
        if (usuario.getEstado() == nuevoEstado) {
            return;
        }
        switch (nuevoEstado) {
            case HABILITADO -> usuario.habilitar();
            case INACTIVO -> usuario.suspender();
            case ACTIVO -> {
                if (usuario.esInactivo()) usuario.reactivar();
                else if (usuario.esHabilitado()) usuario.inhabilitar();
                else throw new UsuarioNoEncontradoException("");
            }
        }
    }
}
