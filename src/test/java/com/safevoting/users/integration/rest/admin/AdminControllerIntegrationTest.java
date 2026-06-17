package com.safevoting.users.integration.rest.admin;

import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.application.usuario.PaginaResultado;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.AdminController;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.UsuarioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = AdminController.class,
        excludeAutoConfiguration = {
                ReactiveSecurityAutoConfiguration.class,
                ReactiveUserDetailsServiceAutoConfiguration.class
        }
)
@Import({GlobalExceptionHandler.class})
class AdminControllerIntegrationTest {

    private static final UUID GESTOR_UID = UUID.randomUUID();

    @Autowired
    private WebTestClient webClient;

    @MockitoBean
    private CambiarEstadoIndividualUseCase cambiarEstadoIndividualUseCase;

    @MockitoBean
    private CambiarEstadoMasivoPorAlcanceUseCase cambiarEstadoMasivoPorAlcanceUseCase;

    @MockitoBean
    private CambiarEstadoMasivoPorMunicipioUseCase cambiarEstadoMasivoPorMunicipioUseCase;

    @MockitoBean
    private ListarUsuariosUseCase listarUsuariosUseCase;

    @MockitoBean
    private BuscarUsuarioPorDocumentoUseCase buscarUsuarioPorDocumentoUseCase;

    @MockitoBean
    private UsuarioDtoMapper dtoMapper;

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
                    return chain.filter(inv.getArgument(0))
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                    new UsernamePasswordAuthenticationToken(GESTOR_UID.toString(), null,
                                            List.of(new SimpleGrantedAuthority("ROLE_GESTOR_ELECTORAL")))));
                });
    }

    @Test
    void deberiaRetornar200AlCambiarEstadoIndividual() {
        Usuario usuarioObjetivo = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(Email.builder().valor("juan@example.com").build())
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.HABILITADO)
                .build();

        when(cambiarEstadoIndividualUseCase.ejecutar(eq(GESTOR_UID), eq("123456789"), eq(EstadoUsuario.HABILITADO)))
                .thenReturn(Mono.just(usuarioObjetivo));
        when(dtoMapper.toResponse(any(Usuario.class)))
                .thenReturn(new UsuarioResponse(UUID.randomUUID(), "Juan", "juan@example.com", "123456789", "Envigado", "Antioquia", "HABILITADO", "VOTANTE", "3001234567"));

        webClient.patch().uri("/api/v1/admin/users/123456789/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\": \"HABILITADO\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("HABILITADO");
    }

    @Test
    void deberiaRetornar404CuandoUsuarioNoEncontrado() {
        when(cambiarEstadoIndividualUseCase.ejecutar(eq(GESTOR_UID), eq("999999999"), eq(EstadoUsuario.HABILITADO)))
                .thenReturn(Mono.error(new UsuarioNoEncontradoException("999999999")));

        webClient.patch().uri("/api/v1/admin/users/999999999/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"estado\": \"HABILITADO\"}")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("USUARIO_NO_ENCONTRADO");
    }

    @Test
    void deberiaRetornar200AlCambiarEstadoMasivoPorAlcance() {
        when(cambiarEstadoMasivoPorAlcanceUseCase.ejecutar(
                eq(GESTOR_UID), any(), any(), any(), eq(EstadoUsuario.INACTIVO)))
                .thenReturn(Mono.just(5L));

        webClient.patch().uri("/api/v1/admin/users/estado/masivo")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"alcance\": \"DEPARTAMENTAL\", \"departamentoId\": \"" + UUID.randomUUID() + "\", \"estado\": \"INACTIVO\", \"gestorMunicipioId\": \"" + UUID.randomUUID() + "\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.usuariosModificados").isEqualTo(5);
    }

    @Test
    void deberiaRetornar200AlCambiarEstadoMasivoPorMunicipio() {
        UUID municipioId = UUID.randomUUID();
        when(cambiarEstadoMasivoPorMunicipioUseCase.ejecutar(eq(GESTOR_UID), eq(municipioId), eq(EstadoUsuario.INACTIVO)))
                .thenReturn(Mono.just(3L));

        webClient.patch().uri("/api/v1/admin/users/estado/masivo/municipio/" + municipioId + "?estado=INACTIVO")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.usuariosModificados").isEqualTo(3);
    }

    @Test
    void deberiaRetornar200AlListarUsuarios() {
        UUID municipioId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(Email.builder().valor("juan@example.com").build())
                .documento(DocumentoIdentidad.builder().valor("12345").build())
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .build();
        var pagina = new PaginaResultado<>(List.of(usuario), 0, 20, 1, 1);

        when(listarUsuariosUseCase.ejecutar(eq(GESTOR_UID), eq(municipioId), eq(0), eq(20)))
                .thenReturn(Mono.just(pagina));
        when(dtoMapper.toResponse(any(Usuario.class)))
                .thenReturn(new UsuarioResponse(UUID.randomUUID(), "Juan", "juan@example.com", "123", "Medellin", "Antioquia", "ACTIVO", "VOTANTE", null));

        webClient.get().uri("/api/v1/admin/users?municipioId=" + municipioId + "&page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void deberiaRetornar200AlBuscarPorDocumento() {
        Usuario usuarioObjetivo = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(Email.builder().valor("juan@example.com").build())
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        when(buscarUsuarioPorDocumentoUseCase.ejecutar(eq(GESTOR_UID), eq("123456789")))
                .thenReturn(Mono.just(usuarioObjetivo));
        when(dtoMapper.toResponse(any(Usuario.class)))
                .thenReturn(new UsuarioResponse(UUID.randomUUID(), "Juan", "juan@example.com", "123456789", "Envigado", "Antioquia", "ACTIVO", "VOTANTE", null));

        webClient.get().uri("/api/v1/admin/users/123456789")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.documento").isEqualTo("123456789");
    }

    @Test
    void deberiaRetornar404AlBuscarDocumentoNoExistente() {
        when(buscarUsuarioPorDocumentoUseCase.ejecutar(eq(GESTOR_UID), eq("999999999")))
                .thenReturn(Mono.error(new UsuarioNoEncontradoException("999999999")));

        webClient.get().uri("/api/v1/admin/users/999999999")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("USUARIO_NO_ENCONTRADO");
    }
}
