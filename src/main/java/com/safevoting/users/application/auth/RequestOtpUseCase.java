package com.safevoting.users.application.auth;

import com.safevoting.users.domain.exception.usuario.UsuarioInactivoException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.EmailSender;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class RequestOtpUseCase {

    private final UsuarioRepository usuarioRepository;
    private final OtpRepository otpRepository;
    private final EmailSender emailSender;

    public Mono<String> ejecutar(String documentoStr) {
        DocumentoIdentidad documento = DocumentoIdentidad.builder().valor(documentoStr).build();

        return usuarioRepository.findByDocumento(documento)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoStr)))
                .filter(Usuario::esHabilitado)
                .switchIfEmpty(Mono.error(new UsuarioInactivoException()))
                .flatMap(usuario -> generarYEnviarOtp(usuario.getEmail()));
    }

    private Mono<String> generarYEnviarOtp(Email email) {
        return otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)
                .flatMap(otpActivo -> {
                    otpActivo.invalidar();
                    return otpRepository.update(otpActivo);
                })
                .then(Mono.defer(() -> {
                    Otp nuevoOtp = buildOtp(email);
                    return otpRepository.save(nuevoOtp)
                            .flatMap(otp -> emailSender.enviarOtp(email, otp.getCodigo())
                                    .thenReturn("Si el documento está registrado, recibirás un código en tu correo."));
                }));
    }

    private String generarCodigoAleatorio() {
        return UUID.randomUUID().toString().toUpperCase().substring(0, 6);
    }

    private Otp buildOtp(Email votanteEmail) {
        String codigo = generarCodigoAleatorio();
        return Otp.builder()
                .email(votanteEmail)
                .codigo(codigo)
                .build();
    }
}
