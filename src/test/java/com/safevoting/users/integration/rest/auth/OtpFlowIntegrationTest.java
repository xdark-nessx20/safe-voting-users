package com.safevoting.users.integration.rest.auth;

import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.AuthController;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.dto.AuthResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.auth.mapper.AuthDtoMapper;
import com.safevoting.users.infrastructure.adapter.in.rest.common.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        }
)
@Import({GlobalExceptionHandler.class})
class OtpFlowIntegrationTest {

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

    @BeforeEach
    void setUp() {
        when(jwtFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(inv -> {
                    WebFilterChain chain = inv.getArgument(1);
                    return chain.filter(inv.getArgument(0));
                });
    }

    @Test
    void deberiaCompletarFlujoCompletoSolicitarYVerificarOtp() {
        var requestBody = "{\"email\": \"juan@example.com\"}";
        var verifyBody = "{\"email\": \"juan@example.com\", \"codigo\": \"123456\"}";

        when(requestOtpUseCase.solicitarOtp("juan@example.com"))
                .thenReturn(Mono.just("Si el email está registrado, recibirás un código."));
        when(verifyOtpUseCase.verificarOtp("juan@example.com", "123456"))
                .thenReturn(Mono.just(new AuthResponse("mock-token", "juan@example.com", "VOTANTE")));

        webClient.post().uri("/api/v1/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Si el email está registrado, recibirás un código.");

        webClient.post().uri("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(verifyBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rol").isEqualTo("VOTANTE");
    }

    @Test
    void deberiaRetornarMensajeGenericoParaEmailNoRegistradoAlSolicitarOtp() {
        var body = "{\"email\": \"noexiste@example.com\"}";

        when(requestOtpUseCase.solicitarOtp("noexiste@example.com"))
                .thenReturn(Mono.just("Si el email está registrado, recibirás un código."));

        webClient.post().uri("/api/v1/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Si el email está registrado, recibirás un código.");
    }
}
