package com.safevoting.users.infrastructure.adapter.out.persistence.gestor;

import com.safevoting.users.domain.model.usuario.GestorElectoral;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GestorElectoralRepositoryAdapter implements GestorElectoralRepository {

    private static final Logger log = LoggerFactory.getLogger(GestorElectoralRepositoryAdapter.class);

    private final GestorElectoralReactiveRepository reactiveRepository;
    private final GestorElectoralPersistenceMapper mapper;
    private final UsuarioRepository usuarioRepository;

    @Override
    public Mono<GestorElectoral> findByUsuarioId(UUID usuarioId) {
        return reactiveRepository.findByUsuarioId(usuarioId)
                .flatMap(entity ->
                        usuarioRepository.findById(usuarioId)
                                .map(usuario -> mapper.toDomain(entity, usuario)));
    }

    @Override
    public Mono<GestorElectoral> save(GestorElectoral gestor) {
        GestorElectoralEntity entity = GestorElectoralEntity.builder()
                .usuarioId(gestor.getId())
                .alcanceOperacion(gestor.getAlcance().name())
                .build();

        return reactiveRepository.save(entity)
                .doOnError(e -> log.error("Error al guardar gestor electoral: {}", e.getMessage(), e))
                .thenReturn(gestor);
    }
}
