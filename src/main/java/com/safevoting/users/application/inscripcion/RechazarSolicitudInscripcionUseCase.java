package com.safevoting.users.application.inscripcion;

import com.safevoting.users.domain.exception.inscripcion.SolicitudNotFoundException;
import com.safevoting.users.domain.exception.inscripcion.SolicitudYaProcesadaException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.SolicitudCambioInscripcionRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class RechazarSolicitudInscripcionUseCase {

    private final SolicitudCambioInscripcionRepository solicitudRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<SolicitudCambioInscripcion> ejecutar(UUID gestorUsuarioId, UUID solicitudId, String motivoRechazo) {
        return Mono.zip(
                gestorElectoralRepository.findByUsuarioId(gestorUsuarioId)
                        .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(gestorUsuarioId))),
                solicitudRepository.findById(solicitudId)
                        .switchIfEmpty(Mono.error(new SolicitudNotFoundException(solicitudId)))
        ).flatMap(tuple -> {
            GestorElectoral gestor = tuple.getT1();
            SolicitudCambioInscripcion solicitud = tuple.getT2();

            gestor.validarAlcance(solicitud.getMunicipioDestino());

            solicitud.rechazar(motivoRechazo);
            solicitud.setGestor(gestor.getId());
            return updateSolicitud(solicitud);
        });
    }

    private Mono<SolicitudCambioInscripcion> updateSolicitud(SolicitudCambioInscripcion solicitud){
        return solicitudRepository.update(solicitud)
                .filter(rows -> rows > 0)
                .switchIfEmpty(Mono.error(new SolicitudYaProcesadaException(solicitud.getId())))
                .thenReturn(solicitud);
    }
}
