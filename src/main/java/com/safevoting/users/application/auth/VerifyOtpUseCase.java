package com.safevoting.users.application.auth;

import com.safevoting.users.domain.model.exception.otp.OtpExpiradoException;
import com.safevoting.users.domain.model.exception.otp.OtpInvalidoException;
import com.safevoting.users.domain.model.exception.otp.ReintentosExcedidosException;
import com.safevoting.users.domain.model.exception.usuario.UsuarioNoHabilitadoException;
import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.dto.AuthResponse;
import com.safevoting.users.infrastructure.config.JwtProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VerifyOtpUseCase {

    private final UsuarioRepository usuarioRepository;
    private final OtpRepository otpRepository;
    private final JwtProvider jwtProvider;

    public VerifyOtpUseCase(UsuarioRepository usuarioRepository,
                            OtpRepository otpRepository,
                            JwtProvider jwtProvider) {
        this.usuarioRepository = usuarioRepository;
        this.otpRepository = otpRepository;
        this.jwtProvider = jwtProvider;
    }

    public Mono<AuthResponse> verificarOtp(String emailStr, String codigo) {
        Email email = Email.builder().valor(emailStr).build();

        return otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)
                .switchIfEmpty(Mono.error(new OtpInvalidoException()))
                .flatMap(otp -> {
                    if (!otp.esValido()) {
                        return Mono.error(new OtpExpiradoException());
                    }

                    if (!otp.getCodigo().equals(codigo)) {
                        otp.incrementarIntento();
                        return otpRepository.update(otp)
                                .flatMap(otpActualizado -> {
                                    if (otpActualizado.getEstado() == EstadoOtp.INVALIDADO) {
                                        return Mono.error(new ReintentosExcedidosException());
                                    }
                                    return Mono.error(new OtpInvalidoException());
                                });
                    }

                    otp.marcarUsado();
                    return otpRepository.update(otp)
                            .then(usuarioRepository.findByEmail(email))
                            .flatMap(usuario -> {
                                if (usuario.getEstado() != EstadoUsuario.HABILITADO) {
                                    return Mono.error(new UsuarioNoHabilitadoException());
                                }
                                String token = jwtProvider.generarToken(
                                        usuario.getEmail().getValor(),
                                        usuario.getRol().name());
                                return Mono.just(new AuthResponse(
                                        token,
                                        usuario.getEmail().getValor(),
                                        usuario.getRol().name()));
                            });
                });
    }
}
