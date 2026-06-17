package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.AlcanceInsuficienteException;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoMasivoPorAlcanceUseCase {

    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<Long> ejecutar(UUID gestorUsuarioId, AlcanceOperacion alcanceSolicitado,
                                UUID departamentoId, UUID municipioId, EstadoUsuario nuevoEstado) {
        return gestorElectoralRepository.findByUsuarioId(gestorUsuarioId)
                .flatMap(gestor -> {
                    gestor.validarAlcanceNoSuperado(alcanceSolicitado);
                    return ejecutarActualizacion(alcanceSolicitado, departamentoId, municipioId, nuevoEstado);
                });
    }

    private Mono<Long> ejecutarActualizacion(AlcanceOperacion alcance, UUID departamentoId,
                                              UUID municipioId, EstadoUsuario nuevoEstado) {
        switch (alcance) {
            case MUNICIPAL:
                return usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado);
            case DEPARTAMENTAL:
                return usuarioRepository.updateEstadoBatchByDepartamento(departamentoId, nuevoEstado);
            case NACIONAL:
                return usuarioRepository.updateEstadoBatchNacional(nuevoEstado);
            default:
                return Mono.error(new AlcanceInsuficienteException("Alcance no soportado: " + alcance));
        }
    }
}
