package com.safevoting.users.application.inscripcion;

import com.safevoting.users.domain.exception.inscripcion.SolicitudNotFoundException;
import com.safevoting.users.domain.exception.inscripcion.SolicitudYaProcesadaException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
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
public class AceptarSolicitudInscripcionUseCase {

    private static final Logger log = LoggerFactory.getLogger(AceptarSolicitudInscripcionUseCase.class);

    private final SolicitudCambioInscripcionRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<SolicitudCambioInscripcion> ejecutar(UUID actorId, UUID solicitudId) {
        log.debug("Aceptar solicitud {} como actor {}", solicitudId, actorId);

        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .doOnNext(actor -> log.debug("Actor encontrado: id={}, rol={}", actor.getId(), actor.getRol()))
                .flatMap(actor ->
                        solicitudRepository.findById(solicitudId)
                                .switchIfEmpty(Mono.error(new SolicitudNotFoundException(solicitudId)))
                                .flatMap(solicitud -> {
                                    log.debug("Solicitud cargada: id={}, estado={}, votante={}",
                                            solicitud.getId(), solicitud.getEstado(), solicitud.getUsuarioId());
                                    return Mono.just(actor)
                                            .filter(Usuario::esAdmin)
                                            .flatMap(a -> operacionAdmin(solicitud, actorId))
                                            .switchIfEmpty(
                                                    gestorElectoralRepository.findByUsuarioId(actorId)
                                                            .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                            .flatMap(gestor -> {
                                                                log.debug("Gestor cargado: id={}, alcance={}",
                                                                        gestor.getId(), gestor.getAlcance());
                                                                return operacionGestor(solicitud, gestor);
                                                            })
                                            );
                                }))
                .doOnError(e -> log.error("Error al aceptar solicitud {}: {}", solicitudId, e.getMessage(), e));
    }

    private Mono<SolicitudCambioInscripcion> operacionAdmin(SolicitudCambioInscripcion solicitud, UUID actorId) {
        return usuarioRepository.findById(solicitud.getUsuarioId())
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(solicitud.getUsuarioId())))
                .flatMap(votante -> {
                    solicitud.aceptar();
                    solicitud.setGestor(actorId);
                    votante.setMunicipio(solicitud.getMunicipioDestino());
                    return updateSolicitudAndVotante(solicitud, votante);
                });
    }

    private Mono<SolicitudCambioInscripcion> operacionGestor(SolicitudCambioInscripcion solicitud, GestorElectoral gestor){
        gestor.validarAlcance(solicitud.getMunicipioDestino());
        return usuarioRepository.findById(solicitud.getUsuarioId())
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(solicitud.getUsuarioId())))
                .flatMap(votante -> {
                    solicitud.aceptar();
                    solicitud.setGestor(gestor.getId());
                    votante.setMunicipio(solicitud.getMunicipioDestino());
                    return updateSolicitudAndVotante(solicitud, votante);
                });
    }

    private Mono<SolicitudCambioInscripcion> updateSolicitudAndVotante(SolicitudCambioInscripcion solicitud, Usuario votante) {
        return solicitudRepository.update(solicitud)
                .filter(rows -> rows > 0)
                .switchIfEmpty(Mono.error(new SolicitudYaProcesadaException(solicitud.getId())))
                .then(usuarioRepository.save(votante))
                .thenReturn(solicitud);
    }
}
