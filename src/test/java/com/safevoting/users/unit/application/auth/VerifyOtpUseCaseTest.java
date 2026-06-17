package com.safevoting.users.unit.application.auth;

import com.safevoting.users.application.auth.AuthResult;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.domain.exception.otp.OtpExpiradoException;
import com.safevoting.users.domain.exception.otp.OtpInvalidoException;
import com.safevoting.users.domain.exception.otp.ReintentosExcedidosException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoHabilitadoException;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.repository.TokenService;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyOtpUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private VerifyOtpUseCase useCase;

    private final String emailStr = "juan@example.com";
    private final Email email = Email.builder().valor(emailStr).build();
    private final String codigoCorrecto = "123456";
    private final Municipio municipio = Municipio.builder()
            .id(UUID.randomUUID())
            .nombre("Medellín")
            .departamento(Departamento.builder().id(UUID.randomUUID()).nombre("Antioquia").build())
            .build();

    @Test
    void deberiaAutenticarUsuarioHabilitadoConOtpCorrectoYRetornarJwt() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo(codigoCorrecto)
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(email)
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.HABILITADO)
                .createdAt(Instant.now())
                .build();
        String tokenEsperado = "eyJhbGciOiJIUzI1NiJ9.mocktoken";

        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.just(otp));
        when(otpRepository.update(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.just(usuario));
        when(tokenService.generarToken(usuario.getId(), emailStr, "VOTANTE")).thenReturn(tokenEsperado);

        StepVerifier.create(useCase.ejecutar(emailStr, codigoCorrecto))
                .assertNext(result -> {
                    assert result.token().equals(tokenEsperado);
                    assert result.email().equals(emailStr);
                    assert result.rol().equals("VOTANTE");
                })
                .verifyComplete();
    }

    @Test
    void deberiaLanzarUsuarioNoHabilitadoCuandoUsuarioEstaActivo() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo(codigoCorrecto)
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(email)
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(Instant.now())
                .build();

        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.just(otp));
        when(otpRepository.update(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.just(usuario));

        StepVerifier.create(useCase.ejecutar(emailStr, codigoCorrecto))
                .expectError(UsuarioNoHabilitadoException.class)
                .verify();
    }

    @Test
    void deberiaLanzarOtpExpiradoCuandoOtpEstaExpirado() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo(codigoCorrecto)
                .expiracion(Instant.now().minus(1, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.just(otp));

        StepVerifier.create(useCase.ejecutar(emailStr, codigoCorrecto))
                .expectError(OtpExpiradoException.class)
                .verify();
    }

    @Test
    void deberiaLanzarOtpInvalidoCuandoCodigoNoCoincide() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo(codigoCorrecto)
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.just(otp));
        when(otpRepository.update(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(emailStr, "999999"))
                .expectError(OtpInvalidoException.class)
                .verify();
    }

    @Test
    void deberiaLanzarReintentosExcedidosEnTercerIntentoFallido() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo(codigoCorrecto)
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(2)
                .estado(EstadoOtp.ACTIVO)
                .build();

        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.just(otp));
        when(otpRepository.update(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(emailStr, "999999"))
                .expectError(ReintentosExcedidosException.class)
                .verify();
    }

    @Test
    void deberiaLanzarOtpInvalidoCuandoNoExisteOtpActivo() {
        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(emailStr, codigoCorrecto))
                .expectError(OtpInvalidoException.class)
                .verify();
    }

    @Test
    void deberiaAutenticarGestorElectoralHabilitado() {
        Otp otp = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo(codigoCorrecto)
                .expiracion(Instant.now().plus(5, ChronoUnit.MINUTES))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();
        Usuario gestor = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Gestor")
                .email(email)
                .documento(DocumentoIdentidad.builder().valor("987654321").build())
                .municipio(municipio)
                .rol(Rol.GESTOR_ELECTORAL)
                .estado(EstadoUsuario.HABILITADO)
                .createdAt(Instant.now())
                .build();
        String tokenEsperado = "eyJhbGciOiJIUzI1NiJ9.gestortoken";

        when(otpRepository.findByEmailAndEstado(email, EstadoOtp.ACTIVO)).thenReturn(Mono.just(otp));
        when(otpRepository.update(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.just(gestor));
        when(tokenService.generarToken(gestor.getId(), emailStr, "GESTOR_ELECTORAL")).thenReturn(tokenEsperado);

        StepVerifier.create(useCase.ejecutar(emailStr, codigoCorrecto))
                .assertNext(result -> {
                    assert result.rol().equals("GESTOR_ELECTORAL");
                })
                .verifyComplete();
    }
}
