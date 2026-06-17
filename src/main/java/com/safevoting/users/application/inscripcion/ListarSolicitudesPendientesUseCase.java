package com.safevoting.users.application.inscripcion;

import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.SolicitudCambioInscripcionRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarSolicitudesPendientesUseCase {

    private final SolicitudCambioInscripcionRepository solicitudRepository;
    private final GestorElectoralRepository gestorElectoralRepository;
    private final UsuarioRepository usuarioRepository;

    public Flux<SolicitudCambioInscripcion> ejecutar(UUID actorId, int pagina, int tamano) {
        long offset = (long) pagina * tamano;

        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .flatMapMany(actor ->
                        Flux.just(actor)
                                .filter(Usuario::esAdmin)
                                .flatMap(a -> solicitudRepository.findAllPendientes(offset, tamano))
                                .switchIfEmpty(
                                        gestorElectoralRepository.findByUsuarioId(actorId)
                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                .flatMapMany(gestor -> ejecutarPorAlcance(gestor, offset, tamano))
                                ));
    }

    private Flux<SolicitudCambioInscripcion> ejecutarPorAlcance(GestorElectoral gestor, long offset, int limit) {
        return switch (gestor.getAlcance()) {
            case NACIONAL -> solicitudRepository.findAllPendientes(offset, limit);
            case DEPARTAMENTAL -> solicitudRepository.findPendientesByDepartamentoDestinoId(
                    gestor.getMunicipio().getDepartamento().getId(), offset, limit);
            case MUNICIPAL -> solicitudRepository.findPendientesByMunicipioDestinoId(
                    gestor.getMunicipio().getId(), offset, limit);
        };
    }
}
