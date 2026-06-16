package com.safevoting.users.infrastructure.adapter.in.rest.admin;

import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.domain.exception.common.DatosInvalidosException;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.*;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final UsuarioRepository usuarioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;
    private final UsuarioDtoMapper dtoMapper;

    @PatchMapping("/{documento}/estado")
    @Operation(summary = "Cambiar el estado de un usuario específico")
    public Mono<ResponseEntity<UsuarioResponse>> cambiarEstadoIndividual(
            @PathVariable String documento,
            @Valid @RequestBody CambiarEstadoRequest request) {
        EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(request.estado().toUpperCase());

        return obtenerGestor()
                .flatMap(gestor -> cambiarEstadoIndividualUseCase.ejecutar(documento, nuevoEstado, gestor))
                .map(usuario -> ResponseEntity.ok(dtoMapper.toResponse(usuario)));
    }

    @PatchMapping("/estado/masivo")
    @Operation(summary = "Cambiar el estado de usuarios masivamente por alcance")
    public Mono<ResponseEntity<Map<String, Long>>> cambiarEstadoMasivoPorAlcance(
            @Valid @RequestBody CambioMasivoRequest request) {
        AlcanceOperacion alcance = AlcanceOperacion.valueOf(request.alcance().toUpperCase());
        EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(request.estado().toUpperCase());

        return obtenerGestor()
                .flatMap(gestor ->
                        cambiarEstadoMasivoPorAlcanceUseCase.ejecutar(
                                alcance,
                                request.departamentoId(),
                                request.municipioId(),
                                nuevoEstado,
                                gestor))
                .map(count -> ResponseEntity.ok(Map.of("usuariosModificados", count)));
    }

    @PatchMapping("/estado/masivo/municipio/{municipioId}")
    @Operation(summary = "Cambiar el estado de usuarios masivamente por municipio")
    public Mono<ResponseEntity<Map<String, Long>>> cambiarEstadoMasivoPorMunicipio(
            @PathVariable UUID municipioId,
            @RequestParam String estado) {
        EstadoUsuario nuevoEstado = EstadoUsuario.valueOf(estado.toUpperCase());

        return obtenerGestor()
                .flatMap(gestor ->
                        cambiarEstadoMasivoPorMunicipioUseCase.ejecutar(municipioId, nuevoEstado, gestor))
                .map(count -> ResponseEntity.ok(Map.of("usuariosModificados", count)));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios por municipio de inscripción")
    public Mono<ResponseEntity<PaginaResponse<UsuarioResponse>>> listarUsuarios(
            @RequestParam UUID municipioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return obtenerGestor()
                .flatMap(gestor -> listarUsuariosUseCase.ejecutar(municipioId, page, size, gestor))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{documento}")
    @Operation(summary = "Buscar usuario por número de documento")
    public Mono<ResponseEntity<UsuarioResponse>> buscarPorDocumento(
            @PathVariable String documento) {
        return obtenerGestor()
                .flatMap(gestor -> buscarUsuarioPorDocumentoUseCase.ejecutar(documento, gestor))
                .map(usuario -> ResponseEntity.ok(dtoMapper.toResponse(usuario)));
    }

    private Mono<GestorElectoral> obtenerGestor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getPrincipal().toString())
                .flatMap(email -> usuarioRepository.findByEmail(Email.builder().valor(email).build()))
                .filter(usuario -> usuario.getRol() == Rol.GESTOR_ELECTORAL)
                .switchIfEmpty(Mono.error(new DatosInvalidosException("El usuario autenticado no es un gestor electoral")))
                .flatMap(usuario ->
                        gestorElectoralRepository.findByUsuarioId(usuario.getId())
                                .map(gestor -> GestorElectoral.builder()
                                        .id(gestor.getId())
                                        .usuario(usuario)
                                        .alcance(gestor.getAlcance())
                                        .build()));
    }
}
