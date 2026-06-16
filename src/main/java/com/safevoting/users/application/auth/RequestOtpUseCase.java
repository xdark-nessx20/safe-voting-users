package com.safevoting.users.application.auth;

import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.repository.EmailSender;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.Email;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RequestOtpUseCase {

    private final UsuarioRepository usuarioRepository;
    private final OtpRepository otpRepository;
    private final EmailSender emailSender;

    public RequestOtpUseCase(UsuarioRepository usuarioRepository,
                             OtpRepository otpRepository,
                             EmailSender emailSender) {
        this.usuarioRepository = usuarioRepository;
        this.otpRepository = otpRepository;
        this.emailSender = emailSender;
    }

    public Mono<String> solicitarOtp(String emailStr) {
        Email email = Email.builder().valor(emailStr).build();

        return usuarioRepository.findByEmail(email)
                .flatMap(usuario -> {
                    if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
                        return Mono.just("Si el email está registrado, recibirás un código.");
                    }
                    return generarYEnviarOtp(email);
                })
                .switchIfEmpty(Mono.just("Si el email está registrado, recibirás un código."));
    }

    private Mono<String> generarYEnviarOtp(Email email) {
        return otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)
                .flatMap(otpActivo -> {
                    otpActivo.invalidar();
                    return otpRepository.update(otpActivo);
                })
                .then(Mono.defer(() -> {
                    String codigo = generarCodigoAleatorio();
                    Otp nuevoOtp = Otp.builder()
                            .email(email)
                            .codigo(codigo)
                            .expiracion(Instant.now().plus(Otp.TIEMPO_EXPIRACION_MINUTOS, ChronoUnit.MINUTES))
                            .intentos(0)
                            .estado(EstadoOtp.ACTIVO)
                            .build();
                    return otpRepository.save(nuevoOtp)
                            .flatMap(otp -> emailSender.enviarOtp(email, codigo)
                                    .thenReturn("Si el email está registrado, recibirás un código."));
                }));
    }

    private String generarCodigoAleatorio() {
        int numero = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(numero);
    }
}
