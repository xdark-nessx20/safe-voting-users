package com.safevoting.users.infrastructure.adapter.in.rest.admin;

import com.safevoting.users.application.inscripcion.AceptarSolicitudInscripcionUseCase;
import com.safevoting.users.application.inscripcion.ListarSolicitudesPendientesUseCase;
import com.safevoting.users.application.inscripcion.RechazarSolicitudInscripcionUseCase;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.RechazarSolicitudRequest;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto.SolicitudCambioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.mapper.SolicitudCambioDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inscripciones")
@Tag(name = "Administración de Cambios de Inscripción")
@RequiredArgsConstructor
public class AdminInscripcionController {

    private static final Logger log = LoggerFactory.getLogger(AdminInscripcionController.class);

    private final ListarSolicitudesPendientesUseCase listarSolicitudesPendientesUseCase;
    private final AceptarSolicitudInscripcionUseCase aceptarSolicitudInscripcionUseCase;
    private final RechazarSolicitudInscripcionUseCase rechazarSolicitudInscripcionUseCase;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCambioDtoMapper dtoMapper;

    @GetMapping("/solicitudes")
    @Operation(summary = "Listar solicitudes pendientes según alcance del gestor")
    public Mono<ResponseEntity<Page<SolicitudCambioResponse>>> listarSolicitudes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return usuarioIdAutenticado()
                .flatMapMany(id -> listarSolicitudesPendientesUseCase.ejecutar(id, page, size))
                .flatMap(solicitud ->
                        usuarioRepository.findById(solicitud.getUsuarioId())
                                .map(votante -> dtoMapper.toResponse(solicitud, votante)))
                .collectList()
                .map(list -> ResponseEntity.ok(new PageImpl<>(list,
                        PageRequest.of(page, size), list.size())));
    }

    @PostMapping("/{id}/aceptar")
    @Operation(summary = "Aceptar una solicitud de cambio de inscripción")
    public Mono<ResponseEntity<SolicitudCambioResponse>> aceptarSolicitud(@PathVariable UUID id) {
        return usuarioIdAutenticado()
                .flatMap(gestorId -> aceptarSolicitudInscripcionUseCase.ejecutar(gestorId, id))
                .flatMap(solicitud ->
                        usuarioRepository.findById(solicitud.getUsuarioId())
                                .map(votante -> dtoMapper.toResponse(solicitud, votante)))
                .map(ResponseEntity::ok);
    }

    @PostMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar una solicitud de cambio de inscripción")
    public Mono<ResponseEntity<SolicitudCambioResponse>> rechazarSolicitud(@PathVariable UUID id,
                                                                           @Valid @RequestBody RechazarSolicitudRequest request) {
        return usuarioIdAutenticado()
                .flatMap(gestorId -> rechazarSolicitudInscripcionUseCase.ejecutar(gestorId, id, request.motivoRechazo()))
                .flatMap(solicitud ->
                        usuarioRepository.findById(solicitud.getUsuarioId())
                                .map(votante -> dtoMapper.toResponse(solicitud, votante)))
                .map(ResponseEntity::ok);
    }

    private Mono<UUID> usuarioIdAutenticado() {
        return ReactiveSecurityContextHolder.getContext()
                .handle((ctx, sink) -> {
                    var auth = ctx.getAuthentication();
                    if (auth == null || !auth.isAuthenticated()) {
                        sink.error(new UsuarioNoEncontradoException("No hay usuario autenticado"));
                        return;
                    }
                    var principal = auth.getName();
                    try {
                        sink.next(UUID.fromString(principal));
                    } catch (IllegalArgumentException e) {
                        log.error("uid inválido en el token: '{}'", principal, e);
                        sink.error(new UsuarioNoEncontradoException("Token con identificador de usuario inválido"));
                    }
                });
    }
}
