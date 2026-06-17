package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.exception.usuario.UsuarioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ListarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<PaginaResultado<Usuario>> ejecutar(UUID actorId, UUID municipioId, int pagina, int tamano) {
        long offset = (long) pagina * tamano;

        return usuarioRepository.findById(actorId)
                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                .flatMap(actor ->
                        municipioRepository.findById(municipioId)
                                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(municipioId.toString())))
                                .flatMap(municipio ->
                                        Mono.just(actor)
                                                .filter(Usuario::esAdmin)
                                                .flatMap(a -> buscarUsuarios(municipioId, tamano, offset, pagina, tamano))
                                                .switchIfEmpty(
                                                        gestorElectoralRepository.findByUsuarioId(actorId)
                                                                .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException(actorId)))
                                                                .flatMap(gestor -> {
                                                                    gestor.validarAlcance(municipio);
                                                                    return buscarUsuarios(municipioId, tamano, offset, pagina, tamano);
                                                                })
                                                ))
                );
    }

    private Mono<PaginaResultado<Usuario>> buscarUsuarios(UUID municipioId, int limit, long offset,
                                                           int pagina, int tamano) {
        return Mono.zip(
                usuarioRepository.findByMunicipioId(municipioId, limit, offset).collectList(),
                usuarioRepository.countByMunicipioId(municipioId)
        ).map(tuple -> {
            var contenido = tuple.getT1();
            long total = tuple.getT2();
            int totalPaginas = (int) Math.ceil((double) total / tamano);
            return new PaginaResultado<>(contenido, pagina, tamano, total, totalPaginas);
        });
    }
}
