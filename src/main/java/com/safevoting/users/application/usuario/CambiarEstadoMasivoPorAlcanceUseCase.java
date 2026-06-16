package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.usuario.AlcanceInsuficienteException;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CambiarEstadoMasivoPorAlcanceUseCase {

    private final UsuarioRepository usuarioRepository;

    public Mono<Long> ejecutar(AlcanceOperacion alcanceSolicitado, UUID departamentoId,
                                UUID municipioId, EstadoUsuario nuevoEstado, GestorElectoral gestor) {
        gestor.validarAlcanceNoSuperado(alcanceSolicitado);

        return switch (alcanceSolicitado) {
            case MUNICIPAL -> usuarioRepository.updateEstadoBatch(municipioId, nuevoEstado);
            case DEPARTAMENTAL -> usuarioRepository.updateEstadoBatchByDepartamento(departamentoId, nuevoEstado);
            case NACIONAL -> usuarioRepository.updateEstadoBatchNacional(nuevoEstado);
            default -> Mono.error(new AlcanceInsuficienteException("Alcance no soportado: " + alcanceSolicitado));
        };
    }
}
