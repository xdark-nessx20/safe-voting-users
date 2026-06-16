package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoMasivoPorMunicipioUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;

    public Mono<Long> ejecutar(UUID municipioId, EstadoUsuario nuevoEstado, GestorElectoral gestor) {
        return municipioRepository.findById(municipioId)
                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(municipioId.toString())))
                .flatMap(municipio -> {
                    gestor.validarAlcance(municipio);
                    return usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado);
                });
    }
}
