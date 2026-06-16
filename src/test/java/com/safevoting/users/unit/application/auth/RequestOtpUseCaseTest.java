package com.safevoting.users.unit.application.auth;

import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.domain.model.otp.EstadoOtp;
import com.safevoting.users.domain.model.otp.Otp;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.repository.EmailSender;
import com.safevoting.users.domain.repository.OtpRepository;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestOtpUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private RequestOtpUseCase useCase;

    private final String emailStr = "juan@example.com";
    private final Email email = Email.builder().valor(emailStr).build();
    private final Municipio municipio = Municipio.builder()
            .id(UUID.randomUUID())
            .nombre("Medellín")
            .departamento(Departamento.builder().id(UUID.randomUUID()).nombre("Antioquia").build())
            .build();

    @Test
    void deberiaGenerarOtpYEnviarCorreoCuandoUsuarioExisteYEstaHabilitado() {
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

        when(usuarioRepository.findByEmail(any(Email.class))).thenReturn(Mono.just(usuario));
        when(otpRepository.findByEmailAndEstado(any(Email.class), any(EstadoOtp.class))).thenReturn(Mono.empty());
        when(otpRepository.save(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(emailSender.enviarOtp(any(Email.class), any(String.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.solicitarOtp(emailStr))
                .assertNext(mensaje -> {
                    assert mensaje.contains("recibirás un código");
                })
                .verifyComplete();
    }

    @Test
    void deberiaRetornarMensajeGenericoCuandoEmailNoExiste() {
        when(usuarioRepository.findByEmail(any(Email.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.solicitarOtp(emailStr))
                .assertNext(mensaje -> {
                    assert mensaje.contains("recibirás un código");
                })
                .verifyComplete();
    }

    @Test
    void deberiaRetornarMensajeGenericoCuandoUsuarioEstaInactivo() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(email)
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.INACTIVO)
                .createdAt(Instant.now())
                .build();

        when(usuarioRepository.findByEmail(any(Email.class))).thenReturn(Mono.just(usuario));

        StepVerifier.create(useCase.solicitarOtp(emailStr))
                .assertNext(mensaje -> {
                    assert mensaje.contains("recibirás un código");
                })
                .verifyComplete();
    }

    @Test
    void deberiaInvalidarOtpActivoPrevioAntesDeCrearNuevo() {
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
        Otp otpPrevio = Otp.builder()
                .id(UUID.randomUUID())
                .email(email)
                .codigo("111111")
                .expiracion(Instant.now().plusSeconds(300))
                .intentos(0)
                .estado(EstadoOtp.ACTIVO)
                .build();

        when(usuarioRepository.findByEmail(any(Email.class))).thenReturn(Mono.just(usuario));
        when(otpRepository.findByEmailAndEstado(any(Email.class), any(EstadoOtp.class))).thenReturn(Mono.just(otpPrevio));
        when(otpRepository.update(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(otpRepository.save(any(Otp.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(emailSender.enviarOtp(any(Email.class), any(String.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.solicitarOtp(emailStr))
                .assertNext(mensaje -> {
                    assert mensaje.contains("recibirás un código");
                })
                .verifyComplete();
    }
}
