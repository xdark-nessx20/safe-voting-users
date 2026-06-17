package com.safevoting.users.infrastructure.adapter.in.rest.admin;

import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.*;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Administración de Usuarios")
@RequiredArgsConstructor
public class AdminController {

    private final CambiarEstadoIndividualUseCase cambiarEstadoIndividualUseCase;
    private final CambiarEstadoMasivoPorAlcanceUseCase cambiarEstadoMasivoPorAlcanceUseCase;
    private final CambiarEstadoMasivoPorMunicipioUseCase cambiarEstadoMasivoPorMunicipioUseCase;
    private final ListarUsuariosUseCase listarUsuariosUseCase;
    private final BuscarUsuarioPorDocumentoUseCase buscarUsuarioPorDocumentoUseCase;
    private final UsuarioDtoMapper dtoMapper;

    @PatchMapping("/{documento}/estado")
    @Operation(summary = "Cambiar el estado de un usuario específico")
    public Mono<ResponseEntity<UsuarioResponse>> cambiarEstadoIndividual(
            @PathVariable String documento,
            @Valid @RequestBody CambiarEstadoRequest request) {
        EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(request.estado().toUpperCase());

        return usuarioIdAutenticado()
                .flatMap(id -> cambiarEstadoIndividualUseCase.ejecutar(id, documento, nuevoEstado))
                .map(usuario -> ResponseEntity.ok(dtoMapper.toResponse(usuario)));
    }

    @PatchMapping("/estado/masivo")
    @Operation(summary = "Cambiar el estado de usuarios masivamente por alcance")
    public Mono<ResponseEntity<Map<String, Long>>> cambiarEstadoMasivoPorAlcance(
            @Valid @RequestBody CambioMasivoRequest request) {
        AlcanceOperacion alcance = AlcanceOperacion.valueOf(request.alcance().toUpperCase());
        EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(request.estado().toUpperCase());

        return usuarioIdAutenticado()
                .flatMap(id ->
                        cambiarEstadoMasivoPorAlcanceUseCase.ejecutar(
                                id, alcance, request.departamentoId(), request.municipioId(), nuevoEstado))
                .map(count -> ResponseEntity.ok(Map.of("usuariosModificados", count)));
    }

    @PatchMapping("/estado/masivo/municipio/{municipioId}")
    @Operation(summary = "Cambiar el estado de usuarios masivamente por municipio")
    public Mono<ResponseEntity<Map<String, Long>>> cambiarEstadoMasivoPorMunicipio(
            @PathVariable UUID municipioId,
            @RequestParam String estado) {
        EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(estado.toUpperCase());

        return usuarioIdAutenticado()
                .flatMap(id ->
                        cambiarEstadoMasivoPorMunicipioUseCase.ejecutar(id, municipioId, nuevoEstado))
                .map(count -> ResponseEntity.ok(Map.of("usuariosModificados", count)));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios por municipio de inscripción")
    public Mono<ResponseEntity<Page<UsuarioResponse>>> listarUsuarios(
            @RequestParam UUID municipioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return usuarioIdAutenticado()
                .flatMap(id -> listarUsuariosUseCase.ejecutar(id, municipioId, page, size))
                .map(resultado -> {
                    var contenido = resultado.contenido().stream()
                            .map(dtoMapper::toResponse)
                            .toList();
                    return ResponseEntity.ok(new PageImpl<>(contenido,
                            PageRequest.of(resultado.pagina(), resultado.tamano()),
                            resultado.totalElementos()));
                });
    }

    @GetMapping("/{documento}")
    @Operation(summary = "Buscar usuario por número de documento")
    public Mono<ResponseEntity<UsuarioResponse>> buscarPorDocumento(
            @PathVariable String documento) {
        return usuarioIdAutenticado()
                .flatMap(id -> buscarUsuarioPorDocumentoUseCase.ejecutar(id, documento))
                .map(usuario -> ResponseEntity.ok(dtoMapper.toResponse(usuario)));
    }

    private Mono<UUID> usuarioIdAutenticado() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> UUID.fromString(ctx.getAuthentication().getName()));
    }
}
