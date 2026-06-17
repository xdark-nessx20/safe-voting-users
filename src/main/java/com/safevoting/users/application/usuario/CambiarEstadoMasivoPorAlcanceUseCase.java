package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.AlcanceInsuficienteException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoMasivoPorAlcanceUseCase {

    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<Long> ejecutar(UUID actorId, AlcanceOperacion alcanceSolicitado,
                                UUID departamentoId, UUID municipioId, EstadoUsuario nuevoEstado) {
        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .flatMap(actor ->
                        Mono.just(actor)
                                .filter(Usuario::esAdmin)
                                .flatMap(a -> ejecutarActualizacion(alcanceSolicitado, departamentoId, municipioId, nuevoEstado))
                                .switchIfEmpty(
                                        gestorElectoralRepository.findByUsuarioId(actorId)
                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                .flatMap(gestor -> {
                                                    gestor.validarAlcanceNoSuperado(alcanceSolicitado);
                                                    return ejecutarActualizacion(alcanceSolicitado, departamentoId, municipioId, nuevoEstado);
                                                })
                                ));
    }

    private Mono<Long> ejecutarActualizacion(AlcanceOperacion alcance, UUID departamentoId,
                                              UUID municipioId, EstadoUsuario nuevoEstado) {
        return switch (alcance) {
            case MUNICIPAL -> usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado);
            case DEPARTAMENTAL -> usuarioRepository.updateEstadoBatchByDepartamento(departamentoId, nuevoEstado);
            case NACIONAL -> usuarioRepository.updateEstadoBatchNacional(nuevoEstado);
        };
    }
}
