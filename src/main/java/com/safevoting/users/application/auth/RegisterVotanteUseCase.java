package com.safevoting.users.application.auth;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.DocumentoDuplicadoException;
import com.safevoting.users.domain.exception.usuario.EmailDuplicadoException;
import com.safevoting.users.domain.model.geografia.Municipio;
import com.safevoting.users.domain.model.usuario.Rol;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RegisterVotanteUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;

    public Mono<Usuario> ejecutar(Usuario usuario) {
        return usuarioRepository.findByEmail(usuario.getEmail())
                .flatMap(existente -> Mono.<Usuario>error(new EmailDuplicadoException(usuario.getEmail().getValor())))
                .switchIfEmpty(usuarioRepository.findByDocumento(usuario.getDocumento())
                        .flatMap(existente -> Mono.<Usuario>error(new DocumentoDuplicadoException(usuario.getDocumento().getValor())))
                        .switchIfEmpty(municipioRepository.findById(usuario.getMunicipio().getId())
                                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(usuario.getMunicipio().getId().toString())))
                                .flatMap(municipio -> {
                                    Usuario nuevoUsuario = buildVotante(usuario, municipio);
                                    nuevoUsuario.validateInfo();
                                    return usuarioRepository.save(nuevoUsuario);
                                })
                        ));
    }

    private Usuario buildVotante(Usuario usuario, Municipio municipio){
        return Usuario.builder()
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .documento(usuario.getDocumento())
                .municipio(municipio)
                .rol(Rol.VOTANTE)
                .build();
    }
}
