package com.safevoting.users.integration.rest.admin;

import com.safevoting.users.application.inscripcion.AceptarSolicitudInscripcionUseCase;
import com.safevoting.users.application.inscripcion.ListarSolicitudesPendientesUseCase;
import com.safevoting.users.application.inscripcion.RechazarSolicitudInscripcionUseCase;
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
import com.safevoting.users.infrastructure.adapter.in.rest.admin.AdminInscripcionController;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = AdminInscripcionController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        }
)
@Import({GlobalExceptionHandler.class})
class AdminInscripcionControllerIntegrationTest {

    private static final UUID GESTOR_UID = UUID.randomUUID();

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private ListarSolicitudesPendientesUseCase listarSolicitudesPendientesUseCase;

    @MockitoBean
    private AceptarSolicitudInscripcionUseCase aceptarSolicitudInscripcionUseCase;

    @MockitoBean
    private RechazarSolicitudInscripcionUseCase rechazarSolicitudInscripcionUseCase;

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
    private final SolicitudCambioInscripcion solicitud = SolicitudCambioInscripcion.builder()
            .id(UUID.randomUUID())
            .usuarioId(UUID.randomUUID())
            .municipioOrigen(origen)
            .municipioDestino(destino)
            .motivo("Quiero cambiar mi lugar de votacion por cercania")
            .estado(EstadoSolicitud.PENDIENTE)
            .fechaSolicitud(Instant.now())
            .build();
    private final Usuario votante = Usuario.builder()
            .id(solicitud.getUsuarioId())
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
                                    new UsernamePasswordAuthenticationToken(GESTOR_UID.toString(), null,
                                            List.of(new SimpleGrantedAuthority("ROLE_GESTOR_ELECTORAL")))));
                });
    }

    @Test
    void deberiaRetornar200AlListarSolicitudes() {
        when(listarSolicitudesPendientesUseCase.ejecutar(eq(GESTOR_UID), eq(0), eq(20)))
                .thenReturn(Flux.just(solicitud));
        when(usuarioRepository.findById(solicitud.getUsuarioId())).thenReturn(Mono.just(votante));
        when(dtoMapper.toResponse(any(), any()))
                .thenReturn(new SolicitudCambioResponse(UUID.randomUUID(), solicitud.getUsuarioId(), "Juan", "123456789",
                        "Medellin", "Envigado", "motivo", "PENDIENTE", null, Instant.now(), null));

        webClient.get().uri("/api/v1/admin/inscripciones/solicitudes?page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1);
    }

    @Test
    void deberiaRetornar200AlAceptarSolicitud() {
        SolicitudCambioInscripcion aceptada = SolicitudCambioInscripcion.builder()
                .id(solicitud.getId())
                .usuarioId(solicitud.getUsuarioId())
                .municipioOrigen(origen)
                .municipioDestino(destino)
                .motivo("motivo")
                .estado(EstadoSolicitud.ACEPTADA)
                .gestorId(GESTOR_UID)
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .fechaResolucion(Instant.now())
                .build();

        when(aceptarSolicitudInscripcionUseCase.ejecutar(eq(GESTOR_UID), eq(solicitud.getId())))
                .thenReturn(Mono.just(aceptada));
        when(usuarioRepository.findById(solicitud.getUsuarioId())).thenReturn(Mono.just(votante));
        when(dtoMapper.toResponse(any(), any()))
                .thenReturn(new SolicitudCambioResponse(UUID.randomUUID(), solicitud.getUsuarioId(), "Juan", "123456789",
                        "Medellin", "Envigado", "motivo", "ACEPTADA", null, Instant.now(), Instant.now()));

        webClient.post().uri("/api/v1/admin/inscripciones/" + solicitud.getId() + "/aceptar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("ACEPTADA");
    }

    @Test
    void deberiaRetornar200AlRechazarSolicitud() {
        SolicitudCambioInscripcion rechazada = SolicitudCambioInscripcion.builder()
                .id(solicitud.getId())
                .usuarioId(solicitud.getUsuarioId())
                .municipioOrigen(origen)
                .municipioDestino(destino)
                .motivo("motivo")
                .estado(EstadoSolicitud.RECHAZADA)
                .motivoRechazo("No cumple con los requisitos necesarios")
                .gestorId(GESTOR_UID)
                .fechaSolicitud(solicitud.getFechaSolicitud())
                .fechaResolucion(Instant.now())
                .build();

        when(rechazarSolicitudInscripcionUseCase.ejecutar(eq(GESTOR_UID), eq(solicitud.getId()), eq("No cumple con los requisitos necesarios")))
                .thenReturn(Mono.just(rechazada));
        when(usuarioRepository.findById(solicitud.getUsuarioId())).thenReturn(Mono.just(votante));
        when(dtoMapper.toResponse(any(), any()))
                .thenReturn(new SolicitudCambioResponse(UUID.randomUUID(), solicitud.getUsuarioId(), "Juan", "123456789",
                        "Medellin", "Envigado", "motivo", "RECHAZADA", "No cumple con los requisitos necesarios", Instant.now(), Instant.now()));

        webClient.post().uri("/api/v1/admin/inscripciones/" + solicitud.getId() + "/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"motivoRechazo\": \"No cumple con los requisitos necesarios\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("RECHAZADA");
    }
}
