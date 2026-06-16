package com.safevoting.users.unit.application.auth;

import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.DocumentoDuplicadoException;
import com.safevoting.users.domain.exception.usuario.EmailDuplicadoException;
import com.safevoting.users.domain.model.geografia.Departamento;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.domain.shared.DocumentoIdentidad;
import com.safevoting.users.domain.shared.Email;
import com.safevoting.users.domain.shared.Phone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterVotanteUseCaseTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MunicipioRepository municipioRepository;

    @InjectMocks
    private RegisterVotanteUseCase useCase;

    private final Email email = Email.builder().valor("juan@example.com").build();
    private final DocumentoIdentidad documento = DocumentoIdentidad.builder().valor("123456789").build();
    private final UUID municipioId = UUID.randomUUID();
    private final Departamento departamento = Departamento.builder().id(UUID.randomUUID()).nombre("Antioquia").build();
    private final Municipio municipio = Municipio.builder().id(municipioId).nombre("Medellín").departamento(departamento).build();

    private Usuario crearUsuarioBase() {
        return Usuario.builder()
                .nombre("Juan Pérez")
                .email(email)
                .telefono(Phone.builder().valor("3001234567").build())
                .documento(documento)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(java.time.Instant.now())
                .build();
    }

    @Test
    void deberiaRegistrarVotanteExitosamenteConEstadoActivo() {
        Usuario usuarioBase = crearUsuarioBase();

        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.empty());
        when(usuarioRepository.findByDocumento(documento)).thenReturn(Mono.empty());
        when(municipioRepository.findById(municipioId)).thenReturn(Mono.just(municipio));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            return Mono.just(Usuario.builder()
                    .id(UUID.randomUUID())
                    .nombre(u.getNombre())
                    .email(u.getEmail())
                    .telefono(u.getTelefono())
                    .documento(u.getDocumento())
                    .municipio(u.getMunicipio())
                    .rol(u.getRol())
                    .estado(u.getEstado())
                    .createdAt(u.getCreatedAt())
                    .build());
        });

        StepVerifier.create(useCase.registrar(usuarioBase))
                .assertNext(usuario -> {
                    assert usuario.getEstado() == EstadoUsuario.ACTIVO;
                    assert usuario.getRol() == Rol.VOTANTE;
                })
                .verifyComplete();
    }

    @Test
    void deberiaLanzarEmailDuplicadoExceptionCuandoEmailYaExiste() {
        Usuario usuarioBase = crearUsuarioBase();
        Usuario existente = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Otro")
                .email(email)
                .documento(DocumentoIdentidad.builder().valor("987654321").build())
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(java.time.Instant.now())
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.just(existente));
        when(usuarioRepository.findByDocumento(documento)).thenReturn(Mono.empty());
        when(municipioRepository.findById(municipioId)).thenReturn(Mono.just(municipio));

        StepVerifier.create(useCase.registrar(usuarioBase))
                .expectError(EmailDuplicadoException.class)
                .verify();
    }

    @Test
    void deberiaLanzarDocumentoDuplicadoExceptionCuandoDocumentoYaExiste() {
        Usuario usuarioBase = crearUsuarioBase();
        Usuario existente = Usuario.builder()
                .id(UUID.randomUUID())
                .nombre("Otro")
                .email(Email.builder().valor("otro@example.com").build())
                .documento(documento)
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .estado(EstadoUsuario.ACTIVO)
                .createdAt(java.time.Instant.now())
                .build();

        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.empty());
        when(usuarioRepository.findByDocumento(documento)).thenReturn(Mono.just(existente));
        when(municipioRepository.findById(municipioId)).thenReturn(Mono.just(municipio));

        StepVerifier.create(useCase.registrar(usuarioBase))
                .expectError(DocumentoDuplicadoException.class)
                .verify();
    }

    @Test
    void deberiaLanzarMunicipioNoEncontradoExceptionCuandoMunicipioNoExiste() {
        Usuario usuarioBase = crearUsuarioBase();

        when(usuarioRepository.findByEmail(email)).thenReturn(Mono.empty());
        when(usuarioRepository.findByDocumento(documento)).thenReturn(Mono.empty());
        when(municipioRepository.findById(municipioId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.registrar(usuarioBase))
                .expectError(MunicipioNoEncontradoException.class)
                .verify();
    }
}
