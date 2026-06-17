package com.safevoting.users.application.inscripcion;

import com.safevoting.users.domain.exception.inscripcion.SolicitudNotFoundException;
import com.safevoting.users.domain.exception.inscripcion.SolicitudYaProcesadaException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.SolicitudCambioInscripcionRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class RechazarSolicitudInscripcionUseCase {

    private static final Logger log = LoggerFactory.getLogger(RechazarSolicitudInscripcionUseCase.class);

    private final SolicitudCambioInscripcionRepository solicitudRepository;
    private final GestorElectoralRepository gestorElectoralRepository;
    private final UsuarioRepository usuarioRepository;

    public Mono<SolicitudCambioInscripcion> ejecutar(UUID actorId, UUID solicitudId, String motivoRechazo) {
        log.debug("Rechazar solicitud {} como actor {}", solicitudId, actorId);

        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .doOnNext(actor -> log.debug("Actor encontrado: id={}, rol={}", actor.getId(), actor.getRol()))
                .flatMap(actor ->
                        solicitudRepository.findById(solicitudId)
                                .switchIfEmpty(Mono.error(new SolicitudNotFoundException(solicitudId)))
                                .flatMap(solicitud -> {
                                    log.debug("Solicitud cargada: id={}, estado={}", solicitud.getId(), solicitud.getEstado());
                                    return Mono.just(actor)
                                            .filter(Usuario::esAdmin)
                                            .flatMap(a -> procesarSolicitud(solicitud, motivoRechazo, actorId))
                                            .switchIfEmpty(
                                                    gestorElectoralRepository.findByUsuarioId(actorId)
                                                            .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                            .flatMap(gestor -> {
                                                                log.debug("Gestor cargado: id={}, alcance={}",
                                                                        gestor.getId(), gestor.getAlcance());
                                                                gestor.validarAlcance(solicitud.getMunicipioDestino());
                                                                return procesarSolicitud(solicitud, motivoRechazo, gestor.getId());
                                                            })
                                            );
                                }))
                .doOnError(e -> log.error("Error al rechazar solicitud {}: {}", solicitudId, e.getMessage(), e));
    }

    private Mono<SolicitudCambioInscripcion> procesarSolicitud(SolicitudCambioInscripcion solicitud, String motivoRechazo, UUID actorId){
        solicitud.rechazar(motivoRechazo);
        solicitud.setGestor(actorId);
        return updateSolicitud(solicitud);
    }

    private Mono<SolicitudCambioInscripcion> updateSolicitud(SolicitudCambioInscripcion solicitud) {
        return solicitudRepository.update(solicitud)
                .filter(rows -> rows > 0)
                .switchIfEmpty(Mono.error(new SolicitudYaProcesadaException(solicitud.getId())))
                .thenReturn(solicitud);
    }
}
