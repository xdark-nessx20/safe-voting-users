package com.safevoting.users.infrastructure.adapter.in.rest.auth;

import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.dto.*;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.mapper.AuthDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final RegisterVotanteUseCase registerVotanteUseCase;
    private final RequestOtpUseCase requestOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final AuthDtoMapper authDtoMapper;

    public AuthController(RegisterVotanteUseCase registerVotanteUseCase,
                          RequestOtpUseCase requestOtpUseCase,
                          VerifyOtpUseCase verifyOtpUseCase,
                          AuthDtoMapper authDtoMapper) {
        this.registerVotanteUseCase = registerVotanteUseCase;
        this.requestOtpUseCase = requestOtpUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.authDtoMapper = authDtoMapper;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo votante")
    public Mono<ResponseEntity<Map<String, String>>> registrar(@Valid @RequestBody RegisterRequest request) {
        Usuario usuarioBase = authDtoMapper.toUsuario(request);
        Usuario usuarioConMunicipio = Usuario.builder()
                .nombre(usuarioBase.getNombre())
                .email(usuarioBase.getEmail())
                .telefono(usuarioBase.getTelefono())
                .documento(usuarioBase.getDocumento())
                .municipio(new Municipio(request.municipioId(), "", null))
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(Instant.now())
                .build();
        return registerVotanteUseCase.registrar(usuarioConMunicipio)
                .map(u -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("mensaje", "Usuario registrado exitosamente")));
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Solicitar código OTP de autenticación")
    public Mono<ResponseEntity<Map<String, String>>> solicitarOtp(@Valid @RequestBody OtpRequest request) {
        return requestOtpUseCase.solicitarOtp(request.email())
                .map(mensaje -> ResponseEntity.ok(Map.of("mensaje", mensaje)));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verificar código OTP y obtener token de sesión")
    public Mono<ResponseEntity<AuthResponse>> verificarOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return verifyOtpUseCase.verificarOtp(request.email(), request.codigo())
                .map(ResponseEntity::ok);
    }
}
