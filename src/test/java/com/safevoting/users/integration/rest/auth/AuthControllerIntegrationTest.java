package com.safevoting.users.integration.rest.auth;

import com.safevoting.users.application.auth.AuthResult;
import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.DocumentoDuplicadoException;
import com.safevoting.users.domain.exception.usuario.EmailDuplicadoException;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.domain.shared.Phone;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.AuthController;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.dto.RegisterRequest;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.mapper.AuthDtoMapper;
import com.safevoting.users.infrastructure.adapter.in.rest.common.GlobalExceptionHandler;
import com.safevoting.users.infrastructure.config.BeanConfiguration;
import com.safevoting.users.infrastructure.config.JwtFilter;
import com.safevoting.users.infrastructure.config.JwtProvider;
import com.safevoting.users.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        }
)
@Import({GlobalExceptionHandler.class, BeanConfiguration.class})
class AuthControllerIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private RegisterVotanteUseCase registerVotanteUseCase;

    @MockitoBean
    private RequestOtpUseCase requestOtpUseCase;

    @MockitoBean
    private VerifyOtpUseCase verifyOtpUseCase;

    @MockitoBean
    private AuthDtoMapper authDtoMapper;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SecurityConfig securityConfig;

    private final Municipio municipioMock = Municipio.builder()
            .id(UUID.randomUUID())
            .nombre("Medellín")
            .departamento(Departamento.builder().id(UUID.randomUUID()).nombre("Antioquia").build())
            .build();

    private final Usuario usuarioMapeado = Usuario.builder()
            .nombre("Juan Pérez")
            .email(Email.builder().valor("juan@example.com").build())
            .telefono(Phone.builder().valor("3001234567").build())
            .documento(DocumentoIdentidad.builder().valor("123456789").build())
            .municipio(municipioMock)
            .rol(Rol.VOTANTE)
            .estado(EstadoUsuario.ACTIVO)
            .build();

    @BeforeEach
    void setUp() {
        when(authDtoMapper.toUsuarioParaRegistro(any(RegisterRequest.class))).thenReturn(usuarioMapeado);
        when(jwtFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(inv -> {
                    WebFilterChain chain = inv.getArgument(1);
                    return chain.filter(inv.getArgument(0));
                });
    }

    @Test
    void deberiaRetornar201CuandoRegistroExitoso() {
        var body = """
                {
                    "nombre": "Juan Pérez",
                    "email": "juan@example.com",
                    "telefono": "3001234567",
                    "documento": "123456789",
                    "municipioId": "b1c2d3e4-0003-4000-8000-000000000013"
                }
                """;

        when(registerVotanteUseCase.ejecutar(any(Usuario.class)))
                .thenReturn(Mono.just(Usuario.builder()
                        .id(UUID.randomUUID())
                        .nombre("Juan Pérez")
                        .email(Email.builder().valor("juan@example.com").build())
                        .documento(DocumentoIdentidad.builder().valor("123456789").build())
                        .municipio(municipioMock)
                        .rol(Rol.VOTANTE)
                        .estado(EstadoUsuario.ACTIVO)
                        .createdAt(Instant.now())
                        .build()));

        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Usuario registrado exitosamente");
    }

    @Test
    void deberiaRetornar409CuandoEmailDuplicado() {
        var body = """
                {
                    "nombre": "Juan Pérez",
                    "email": "juan@example.com",
                    "telefono": "3001234567",
                    "documento": "123456789",
                    "municipioId": "b1c2d3e4-0003-4000-8000-000000000013"
                }
                """;

        when(registerVotanteUseCase.ejecutar(any(Usuario.class)))
                .thenReturn(Mono.error(new EmailDuplicadoException("juan@example.com")));

        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("EMAIL_DUPLICADO");
    }

    @Test
    void deberiaRetornar409CuandoDocumentoDuplicado() {
        var body = """
                {
                    "nombre": "Juan Pérez",
                    "email": "otro@example.com",
                    "telefono": "3001234567",
                    "documento": "123456789",
                    "municipioId": "b1c2d3e4-0003-4000-8000-000000000013"
                }
                """;

        when(registerVotanteUseCase.ejecutar(any(Usuario.class)))
                .thenReturn(Mono.error(new DocumentoDuplicadoException("123456789")));

        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("DOCUMENTO_DUPLICADO");
    }

    @Test
    void deberiaRetornar404CuandoMunicipioNoExiste() {
        var body = """
                {
                    "nombre": "Juan Pérez",
                    "email": "juan@example.com",
                    "telefono": "3001234567",
                    "documento": "123456789",
                    "municipioId": "b1c2d3e4-9999-4000-8000-000000000099"
                }
                """;

        when(registerVotanteUseCase.ejecutar(any(Usuario.class)))
                .thenReturn(Mono.error(new MunicipioNoEncontradoException("b1c2d3e4-9999-4000-8000-000000000099")));

        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("MUNICIPIO_NO_ENCONTRADO");
    }

    @Test
    void deberiaRetornar422CuandoFaltaNombre() {
        var body = """
                {
                    "email": "juan@example.com",
                    "telefono": "3001234567",
                    "documento": "123456789",
                    "municipioId": "b1c2d3e4-0003-4000-8000-000000000013"
                }
                """;

        webClient.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("VALIDACION");
    }

    @Test
    void deberiaSolicitarOtpYRetornar200ConMensajeGenerico() {
        var body = """
                {
                    "email": "juan@example.com"
                }
                """;

        when(requestOtpUseCase.ejecutar("juan@example.com"))
                .thenReturn(Mono.just("Si el email está registrado, recibirás un código."));

        webClient.post().uri("/api/v1/auth/login/request-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Si el email está registrado, recibirás un código.");
    }

    @Test
    void deberiaVerificarOtpYRetornarAuthResponse() {
        var body = """
                {
                    "email": "juan@example.com",
                    "codigo": "123456"
                }
                """;

        when(verifyOtpUseCase.ejecutar("juan@example.com", "123456"))
                .thenReturn(Mono.just(new AuthResult("mock-token", "juan@example.com", "VOTANTE")));

        webClient.post().uri("/api/v1/auth/login/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("mock-token")
                .jsonPath("$.email").isEqualTo("juan@example.com")
                .jsonPath("$.rol").isEqualTo("VOTANTE");
    }
}
