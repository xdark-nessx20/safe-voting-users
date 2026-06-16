package com.safevoting.users.application.auth;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.DocumentoDuplicadoException;
import com.safevoting.users.domain.exception.usuario.EmailDuplicadoException;
import com.safevoting.users.domain.model.usuario.EstadoUsuario;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegisterVotanteUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;

    public Mono<Usuario> registrar(Usuario usuario) {
        return usuarioRepository.findByEmail(usuario.getEmail())
                .flatMap(existente -> Mono.<Usuario>error(new EmailDuplicadoException(usuario.getEmail().getValor())))
                .switchIfEmpty(usuarioRepository.findByDocumento(usuario.getDocumento())
                        .flatMap(existente -> Mono.<Usuario>error(new DocumentoDuplicadoException(usuario.getDocumento().getValor())))
                        .switchIfEmpty(municipioRepository.findById(usuario.getMunicipio().getId())
                                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(usuario.getMunicipio().getId().toString())))
                                .flatMap(municipio -> {
                                    Usuario nuevoUsuario = Usuario.builder()
                                            .nombre(usuario.getNombre())
                                            .email(usuario.getEmail())
                                            .telefono(usuario.getTelefono())
                                            .documento(usuario.getDocumento())
                                            .municipio(municipio)
                                            .rol(Rol.VOTANTE)
                                            .estado(EstadoUsuario.ACTIVO)
                                            .createdAt(Instant.now())
                                            .build();
                                    nuevoUsuario.validateInfo();
                                    return usuarioRepository.save(nuevoUsuario);
                                })
                        ));
    }
}
