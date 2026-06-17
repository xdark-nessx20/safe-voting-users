package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
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

    public Mono<Long> ejecutar(UUID gestorUsuarioId, UUID municipioId, EstadoUsuario nuevoEstado) {
        return gestorElectoralRepository.findByUsuarioId(gestorUsuarioId)
                .flatMap(gestor ->
                        municipioRepository.findById(municipioId)
                                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(municipioId.toString())))
                                .flatMap(municipio -> {
                                    gestor.validarAlcance(municipio);
                                    return usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado);
                                }));
    }
}
