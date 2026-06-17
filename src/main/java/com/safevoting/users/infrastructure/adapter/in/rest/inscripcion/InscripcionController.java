package com.safevoting.users.infrastructure.adapter.in.rest.inscripcion;

import com.safevoting.users.application.inscripcion.SolicitarCambioInscripcionUseCase;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto.SolicitarCambioRequest;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto.SolicitudCambioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.mapper.SolicitudCambioDtoMapper;
import com.safevoting.users.domain.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/inscripcion")
@Tag(name = "Cambio de Inscripción")
@RequiredArgsConstructor
public class InscripcionController {

    private final SolicitarCambioInscripcionUseCase solicitarCambioInscripcionUseCase;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCambioDtoMapper dtoMapper;

    @PostMapping("/cambiar")
    @Operation(summary = "Solicitar cambio de lugar de inscripción")
    public Mono<ResponseEntity<SolicitudCambioResponse>> solicitarCambio(
            @Valid @RequestBody SolicitarCambioRequest request) {
        return usuarioIdAutenticado()
                .flatMap(id -> solicitarCambioInscripcionUseCase.ejecutar(id, request.municipioDestinoId(), request.motivo()))
                .flatMap(solicitud ->
                        usuarioRepository.findById(solicitud.getUsuarioId())
                                .map(votante -> dtoMapper.toResponse(solicitud, votante)))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    private Mono<UUID> usuarioIdAutenticado() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()));
    }
}
