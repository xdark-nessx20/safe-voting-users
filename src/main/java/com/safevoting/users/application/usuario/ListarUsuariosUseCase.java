package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.Usuario;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;
    private final GestorElectoralRepository gestorElectoralRepository;

    public Mono<PaginaResultado<Usuario>> ejecutar(UUID gestorUsuarioId, UUID municipioId, int pagina, int tamano) {
        long offset = (long) pagina * tamano;

        return gestorElectoralRepository.findByUsuarioId(gestorUsuarioId)
                .flatMap(gestor ->
                        municipioRepository.findById(municipioId)
                                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(municipioId.toString())))
                                .flatMap(municipio -> {
                                    gestor.validarAlcance(municipio);
                                    return Mono.zip(
                                            usuarioRepository.findByMunicipioId(municipioId, tamano, offset).collectList(),
                                            usuarioRepository.countByMunicipioId(municipioId)
                                    ).map(tuple -> {
                                        List<Usuario> contenido = tuple.getT1();
                                        long total = tuple.getT2();
                                        int totalPaginas = (int) Math.ceil((double) total / tamano);
                                        return new PaginaResultado<>(contenido, pagina, tamano, total, totalPaginas);
                                    });
                                }));
    }
}
