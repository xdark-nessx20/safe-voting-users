package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoMasivoPorMunicipioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<Long> ejecutar(UUID actorId, UUID municipioId, EstadoUsuario nuevoEstado) {
        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .flatMap(actor ->
                        municipioRepository.findById(municipioId)
                                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(municipioId.toString())))
                                .flatMap(municipio ->
                                        Mono.just(actor)
                                                .filter(Usuario::esAdmin)
                                                .flatMap(a -> usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado))
                                                .switchIfEmpty(
                                                        gestorElectoralRepository.findByUsuarioId(actorId)
                                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                                .flatMap(gestor -> {
                                                                    gestor.validarAlcance(municipio);
                                                                    return usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado);
                                                                })
                                                ))
                );
    }
}
