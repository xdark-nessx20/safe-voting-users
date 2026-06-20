package com.safevoting.users.infrastructure.adapter.in.rest.admin;

import com.safevoting.users.application.usuario.ObtenerAlcanceGestorUseCase;
import com.safevoting.users.application.usuario.ObtenerUsuarioPorIdUseCase;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.AlcanceGestorResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.UsuarioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuarios")
@RequiredArgsConstructor
public class UserController {

    private final ObtenerUsuarioPorIdUseCase obtenerUsuarioPorIdUseCase;
    private final ObtenerAlcanceGestorUseCase obtenerAlcanceGestorUseCase;
    private final UsuarioDtoMapper dtoMapper;

    @GetMapping("/me")
    @Operation(summary = "Obtener datos del usuario autenticado")
    public Mono<ResponseEntity<UsuarioResponse>> obtenerUsuarioAutenticado() {
        return usuarioIdAutenticado()
                .flatMap(obtenerUsuarioPorIdUseCase::ejecutar)
                .map(dtoMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener datos de un usuario por ID")
    public Mono<ResponseEntity<UsuarioResponse>> obtenerUsuarioPorId(@PathVariable UUID id) {
        return obtenerUsuarioPorIdUseCase.ejecutar(id)
                .map(dtoMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}/alcance")
    @Operation(summary = "Obtener alcance de operación de un gestor electoral")
    public Mono<ResponseEntity<AlcanceGestorResponse>> obtenerAlcanceGestor(@PathVariable UUID id) {
        return obtenerAlcanceGestorUseCase.ejecutar(id)
                .map(dtoMapper::toAlcanceGestorResponse)
                .map(ResponseEntity::ok);
    }

    private Mono<UUID> usuarioIdAutenticado() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()));
    }
}
