package com.safevoting.users.application.inscripcion;

import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
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

    public Flux<SolicitudCambioInscripcion> ejecutar(UUID gestorUsuarioId, int pagina, int tamano) {
        long offset = (long) pagina * tamano;

        return gestorElectoralRepository.findByUsuarioId(gestorUsuarioId)
                .flatMapMany(gestor -> ejecutarPorAlcance(gestor, offset, tamano));
    }

    private Flux<SolicitudCambioInscripcion> ejecutarPorAlcance(GestorElectoral gestor, long offset, int limit) {
        return switch (gestor.getAlcance()) {
            case NACIONAL -> solicitudRepository.findAllPendientes(offset, limit);
            case DEPARTAMENTAL -> solicitudRepository.findPendientesByDepartamentoDestinoId(
                    getDepartamentoGestor(gestor), offset, limit);
            case MUNICIPAL -> solicitudRepository.findPendientesByMunicipioDestinoId(
                    gestor.getMunicipio().getId(), offset, limit);
        };
    }

    private UUID getDepartamentoGestor(GestorElectoral gestor){
        return gestor.getMunicipio().getDepartamento().getId();
    }
}
