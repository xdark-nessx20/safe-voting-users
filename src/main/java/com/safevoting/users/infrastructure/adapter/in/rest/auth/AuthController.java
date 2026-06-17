package com.safevoting.users.infrastructure.adapter.in.rest.auth;

import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.dto.*;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.mapper.AuthDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterVotanteUseCase registerVotanteUseCase;
    private final RequestOtpUseCase requestOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final AuthDtoMapper authDtoMapper;

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo votante")
    public Mono<ResponseEntity<MessageResponse>> registrar(@Valid @RequestBody RegisterRequest request) {
        Usuario usuario = authDtoMapper.toUsuarioParaRegistro(request);
        return registerVotanteUseCase.ejecutar(usuario)
                .map(u -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new MessageResponse("Usuario registrado exitosamente")));
    }

    @PostMapping("/login/request-otp")
    @Operation(summary = "Solicitar código OTP de autenticación")
    public Mono<ResponseEntity<MessageResponse>> solicitarOtp(@Valid @RequestBody OtpRequest request) {
        return requestOtpUseCase.ejecutar(request.documento())
                .map(mensaje -> ResponseEntity.ok(new MessageResponse(mensaje)));
    }

    @PostMapping("/login/verify-otp")
    @Operation(summary = "Verificar código OTP y obtener token de sesión")
    public Mono<ResponseEntity<AuthResponse>> verificarOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return verifyOtpUseCase.ejecutar(request.documento(), request.codigo())
                .map(result -> ResponseEntity.ok(new AuthResponse(result.token(), result.email(), result.rol())));
    }
}
