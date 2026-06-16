package com.safevoting.users.application.usuario;

import com.safevoting.users.domain.exception.geografia.MunicipioNoEncontradoException;
import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.PaginaResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.dto.UsuarioResponse;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ListarUsuariosUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MunicipioRepository municipioRepository;
    private final UsuarioDtoMapper dtoMapper;

    public Mono<PaginaResponse<UsuarioResponse>> ejecutar(UUID municipioId, int pagina, int tamano, GestorElectoral gestor) {
        long offset = (long) pagina * tamano;

        return municipioRepository.findById(municipioId)
                .switchIfEmpty(Mono.error(new MunicipioNoEncontradoException(municipioId.toString())))
                .flatMap(municipio -> {
                    gestor.validarAlcance(municipio);
                    return Mono.zip(
                            usuarioRepository.findByMunicipioId(municipioId, tamano, offset).collectList(),
                            usuarioRepository.countByMunicipioId(municipioId)
                    ).map(tuple -> {
                        List<UsuarioResponse> contenido = tuple.getT1().stream()
                                .map(dtoMapper::toResponse)
                                .toList();
                        long total = tuple.getT2();
                        int totalPaginas = (int) Math.ceil((double) total / tamano);
                        return new PaginaResponse<>(contenido, pagina, tamano, total, totalPaginas);
                    });
                });
    }
}
