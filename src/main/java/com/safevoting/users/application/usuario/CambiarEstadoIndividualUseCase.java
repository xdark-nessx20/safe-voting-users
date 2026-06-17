package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.GestorNoModificableException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoIndividualUseCase {

    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<Usuario> ejecutar(UUID actorId, String documentoObjetivo, EstadoUsuario nuevoEstado) {
        DocumentoIdentidad documento = DocumentoIdentidad.builder().valor(documentoObjetivo).build();

        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .flatMap(actor ->
                        Mono.just(actor)
                                .filter(Usuario::esAdmin)
                                .flatMap(a -> ejecutarCambio(documento, documentoObjetivo, nuevoEstado, null))
                                .switchIfEmpty(
                                        gestorElectoralRepository.findByUsuarioId(actorId)
                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                .flatMap(gestor -> ejecutarCambio(documento, documentoObjetivo, nuevoEstado, gestor))
                                ));
    }

    private Mono<Usuario> ejecutarCambio(DocumentoIdentidad documento, String documentoRaw,
                                          EstadoUsuario nuevoEstado, GestorElectoral gestor) {
        return usuarioRepository.findByDocumento(documento)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoRaw)))
                .filter(objetivo -> gestor == null || !objetivo.esGestor())
                .switchIfEmpty(Mono.error(new GestorNoModificableException()))
                .filter(objetivo -> gestor == null || gestor.cubre(objetivo.getMunicipio()))
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoRaw)))
                .flatMap(objetivo -> {
                    aplicarEstado(objetivo, nuevoEstado);
                    objetivo.validateInfo();
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
            }
        }
    }
}
