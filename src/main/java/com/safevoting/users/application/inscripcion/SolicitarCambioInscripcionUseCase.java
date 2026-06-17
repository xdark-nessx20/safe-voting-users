package com.safevoting.users.application.inscripcion;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.inscripcion.SolicitudDuplicadaException;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.SolicitudCambioInscripcionRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class SolicitarCambioInscripcionUseCase {

    private final SolicitudCambioInscripcionRepository solicitudRepository;
    private final MunicipioRepository municipioRepository;
    private final UsuarioRepository usuarioRepository;

    public Mono<SolicitudCambioInscripcion> ejecutar(UUID votanteId, UUID municipioDestinoId, String motivo) {
        return solicitudRepository.findPendienteByUsuarioId(votanteId)
                .flatMap(existente -> Mono.<SolicitudCambioInscripcion>error(
                        new SolicitudDuplicadaException("Ya existe una solicitud pendiente para este usuario")))
                .switchIfEmpty(Mono.defer(() ->
                        usuarioRepository.findById(votanteId)
                            .flatMap(votante ->
                                municipioRepository.findById(municipioDestinoId)
                                    .switchIfEmpty(Mono.error(
                                            new MunicipioNoEncontradoException(municipioDestinoId.toString())))
                                    .flatMap(municipioDestino -> {
                                        SolicitudCambioInscripcion solicitud =
                                                buildSolicitud(votante, municipioDestino, motivo);
                                        solicitud.validateInfo();
                                        return solicitudRepository.save(solicitud);
                                    }))));
    }

    private SolicitudCambioInscripcion buildSolicitud(Usuario votante, Municipio municipioDestino, String motivo) {
        return SolicitudCambioInscripcion.builder()
                .usuarioId(votante.getId())
                .municipioOrigen(votante.getMunicipio())
                .municipioDestino(municipioDestino)
                .motivo(motivo)
                .build();
    }
}
