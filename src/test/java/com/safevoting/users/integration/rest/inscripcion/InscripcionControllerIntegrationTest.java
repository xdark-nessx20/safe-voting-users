package com.safevoting.users.integration.rest.inscripcion;

import com.safevoting.users.application.inscripcion.SolicitarCambioInscripcionUseCase;
import com.safevoting.users.domain.model.inscripcion.EstadoSolicitud;
import com.safevoting.users.domain.model.inscripcion.SolicitudCambioInscripcion;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.InscripcionController;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.dto.SolicitudCambioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.inscripcion.mapper.SolicitudCambioDtoMapper;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = InscripcionController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        }
)
@Import({GlobalExceptionHandler.class})
class InscripcionControllerIntegrationTest {

    private static final UUID VOTANTE_UID = UUID.randomUUID();

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private SolicitarCambioInscripcionUseCase solicitarCambioInscripcionUseCase;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private SolicitudCambioDtoMapper dtoMapper;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SecurityConfig securityConfig;

    private final Departamento depto = Departamento.builder().id(UUID.randomUUID()).nombre("Antioquia").build();
    private final Municipio origen = Municipio.builder().id(UUID.randomUUID()).nombre("Medellin").departamento(depto).build();
    private final Municipio destino = Municipio.builder().id(UUID.randomUUID()).nombre("Envigado").departamento(depto).build();
    private final Usuario votante = Usuario.builder()
            .id(VOTANTE_UID)
            .nombre("Juan")
            .email(Email.builder().valor("juan@example.com").build())
            .documento(DocumentoIdentidad.builder().valor("123456789").build())
            .municipio(origen)
            .rol(Rol.VOTANTE)
            .estado(EstadoUsuario.HABILITADO)
            .build();

    @BeforeEach
    void setUp() {
        when(jwtFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(inv -> {
                    WebFilterChain chain = inv.getArgument(1);
                    return chain.filter(inv.getArgument(0))
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                    new UsernamePasswordAuthenticationToken(VOTANTE_UID.toString(), null,
                                            List.of(new SimpleGrantedAuthority("ROLE_VOTANTE")))));
                });
        when(usuarioRepository.findById(VOTANTE_UID)).thenReturn(Mono.just(votante));
    }

    @Test
    void deberiaRetornar201AlSolicitarCambio() {
        SolicitudCambioInscripcion solicitud = SolicitudCambioInscripcion.builder()
                .id(UUID.randomUUID())
                .usuarioId(VOTANTE_UID)
                .municipioOrigen(origen)
                .municipioDestino(destino)
                .motivo("Quiero cambiar mi lugar de votación por cercanía")
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(Instant.now())
                .build();

        when(solicitarCambioInscripcionUseCase.ejecutar(eq(VOTANTE_UID), eq(destino.getId()), any()))
                .thenReturn(Mono.just(solicitud));
        when(dtoMapper.toResponse(any(), any()))
                .thenReturn(new SolicitudCambioResponse(UUID.randomUUID(), VOTANTE_UID, "Juan", "123456789",
                        "Medellin", "Envigado", "Quiero cambiar mi lugar de votación por cercanía",
                        "PENDIENTE", null, Instant.now(), null));

        webClient.post().uri("/api/v1/users/inscripcion/cambiar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"municipioDestinoId\": \"" + destino.getId() + "\", \"motivo\": \"Quiero cambiar mi lugar de votación por cercanía\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("PENDIENTE");
    }
}
