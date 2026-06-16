package com.safevoting.users.integration.rest.admin;

import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.AlcanceOperacion;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.AdminController;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.PaginaResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.UsuarioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
import com.safevoting.users.infrastructure.adapter.in.rest.common.GlobalExceptionHandler;
import com.safevoting.users.infrastructure.config.AdminBeanConfiguration;
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
@Import({GlobalExceptionHandler.class, AdminBeanConfiguration.class})
class AdminControllerIntegrationTest {

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
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private GestorElectoralRepository gestorElectoralRepository;

    @MockitoBean
    private UsuarioDtoMapper dtoMapper;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SecurityConfig securityConfig;

    private final UUID gestorId = UUID.randomUUID();
    private final Departamento depto = Departamento.builder().id(UUID.randomUUID()).nombre("Antioquia").build();
    private final Municipio municipioGestor = Municipio.builder().id(UUID.randomUUID()).nombre("Medellin").departamento(depto).build();
    private final Usuario gestorUsuario = Usuario.builder()
            .id(gestorId)
            .nombre("Gestor")
            .email(Email.builder().valor("gestor@safevoting.com").build())
            .documento(DocumentoIdentidad.builder().valor("111111111").build())
            .municipio(municipioGestor)
            .rol(Rol.GESTOR_ELECTORAL)
            .estado(EstadoUsuario.HABILITADO)
            .build();
    private final GestorElectoral gestor = GestorElectoral.builder()
            .id(UUID.randomUUID())
            .usuario(gestorUsuario)
            .alcance(AlcanceOperacion.DEPARTAMENTAL)
            .build();

    @BeforeEach
    void setUp() {
        when(jwtFilter.filter(any(ServerWebExchange.class), any(WebFilterChain.class)))
                .thenAnswer(inv -> {
                    WebFilterChain chain = inv.getArgument(1);
                    return chain.filter(inv.getArgument(0))
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                    new UsernamePasswordAuthenticationToken("gestor@safevoting.com", null,
                                            List.of(new SimpleGrantedAuthority("ROLE_GESTOR_ELECTORAL")))));
                });
        when(usuarioRepository.findByEmail(any(Email.class))).thenReturn(Mono.just(gestorUsuario));
        when(gestorElectoralRepository.findByUsuarioId(gestorId)).thenReturn(Mono.just(gestor));
    }

    @Test
    void deberiaRetornar200AlCambiarEstadoIndividual() {
        Usuario usuarioObjetivo = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(Email.builder().valor("juan@example.com").build())
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .municipio(Municipio.builder().id(UUID.randomUUID()).nombre("Envigado").departamento(depto).build())
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.HABILITADO)
                .build();

        when(cambiarEstadoIndividualUseCase.ejecutar(eq("123456789"), eq(EstadoUsuario.HABILITADO), any(GestorElectoral.class)))
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
        when(cambiarEstadoIndividualUseCase.ejecutar(eq("999999999"), eq(EstadoUsuario.HABILITADO), any(GestorElectoral.class)))
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
                eq(AlcanceOperacion.DEPARTAMENTAL), any(), any(), eq(EstadoUsuario.INACTIVO), any(GestorElectoral.class)))
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
        when(cambiarEstadoMasivoPorMunicipioUseCase.ejecutar(eq(municipioId), eq(EstadoUsuario.INACTIVO), any(GestorElectoral.class)))
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
        var pagina = new PaginaResponse<>(
                List.of(new UsuarioResponse(UUID.randomUUID(), "Juan", "juan@example.com", "123", "Medellin", "Antioquia", "ACTIVO", "VOTANTE", null)),
                0, 20, 1, 1);

        when(listarUsuariosUseCase.ejecutar(eq(municipioId), eq(0), eq(20), any(GestorElectoral.class)))
                .thenReturn(Mono.just(pagina));

        webClient.get().uri("/api/v1/admin/users?municipioId=" + municipioId + "&page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElementos").isEqualTo(1);
    }

    @Test
    void deberiaRetornar200AlBuscarPorDocumento() {
        Usuario usuarioObjetivo = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Juan")
                .email(Email.builder().valor("juan@example.com").build())
                .documento(DocumentoIdentidad.builder().valor("123456789").build())
                .municipio(Municipio.builder().id(UUID.randomUUID()).nombre("Envigado").departamento(depto).build())
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        when(buscarUsuarioPorDocumentoUseCase.ejecutar(eq("123456789"), any(GestorElectoral.class)))
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
        when(buscarUsuarioPorDocumentoUseCase.ejecutar(eq("999999999"), any(GestorElectoral.class)))
                .thenReturn(Mono.error(new UsuarioNoEncontradoException("999999999")));

        webClient.get().uri("/api/v1/admin/users/999999999")
                .exchange()
                .expectStatus().isEqualTo(404)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("USUARIO_NO_ENCONTRADO");
    }
}
