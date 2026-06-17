package com.safevoting.users.application.auth;

import com.safevoting.users.domain.exception.otp.OtpExpiradoException;
import com.safevoting.users.domain.exception.otp.OtpInvalidoException;
import com.safevoting.users.domain.exception.otp.ReintentosExcedidosException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoHabilitadoException;
import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.repository.TokenService;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class VerifyOtpUseCase {

    private final UsuarioRepository usuarioRepository;
    private final OtpRepository otpRepository;
    private final TokenService tokenService;

    public Mono<AuthResult> ejecutar(String documentoStr, String codigo) {
        DocumentoIdentidad documento = DocumentoIdentidad.builder().valor(documentoStr).build();

        return usuarioRepository.findByDocumento(documento)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(documentoStr)))
                .flatMap(usuario -> {
                    Email email = usuario.getEmail();
                    return otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)
                            .switchIfEmpty(Mono.error(new OtpInvalidoException()))
                            .filter(Otp::esValido)
                            .switchIfEmpty(Mono.error(new OtpExpiradoException()))
                            .flatMap(otp -> procesarCodigo(otp, codigo, usuario));
                });
    }

    private Mono<AuthResult> procesarCodigo(Otp otp, String codigo, Usuario usuario) {
        return Mono.just(otp)
                .flatMap(o -> decidirRama(o, codigo))
                .flatMap(otpRepository::update)
                .then(Mono.just(usuario))
                .filter(Usuario::esHabilitado)
                .switchIfEmpty(Mono.error(new UsuarioNoHabilitadoException()))
                .map(this::construirAuthResult);
    }

    private Mono<Otp> decidirRama(Otp otp, String codigo) {
        return otp.getCodigo().equals(codigo)
                ? ramaCodigoCorrecto(otp)
                : ramaCodigoIncorrecto(otp);
    }

    private Mono<Otp> ramaCodigoCorrecto(Otp otp) {
        return Mono.fromRunnable(otp::marcarUsado)
                .then(Mono.just(otp));
    }

    private Mono<Otp> ramaCodigoIncorrecto(Otp otp) {
        return Mono.fromRunnable(otp::incrementarIntento)
                .then(otpRepository.update(otp))
                .flatMap(this::evaluarReintentos);
    }

    private Mono<Otp> evaluarReintentos(Otp otpActualizado) {
        return Mono.<Otp>error(
                otpActualizado.esInvalidado()
                        ? new ReintentosExcedidosException()
                        : new OtpInvalidoException());
    }

    private AuthResult construirAuthResult(Usuario usuario) {
        String token = tokenService.generarToken(
                usuario.getId(),
                usuario.getEmail().getValor(),
                usuario.getRol().name());
        return new AuthResult(
                token,
                usuario.getEmail().getValor(),
                usuario.getRol().name());
    }
}
