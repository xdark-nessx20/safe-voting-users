package com.safevoting.users.infrastructure.config;

import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import com.safevoting.users.infrastructure.adapter.in.rest.admin.mapper.UsuarioDtoMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBeanConfiguration {

    @Bean
    public CambiarEstadoIndividualUseCase cambiarEstadoIndividualUseCase(
            UsuarioRepository usuarioRepository) {
        return new CambiarEstadoIndividualUseCase(usuarioRepository);
    }

    @Bean
    public CambiarEstadoMasivoPorAlcanceUseCase cambiarEstadoMasivoPorAlcanceUseCase(
            UsuarioRepository usuarioRepository) {
        return new CambiarEstadoMasivoPorAlcanceUseCase(usuarioRepository);
    }

    @Bean
    public CambiarEstadoMasivoPorMunicipioUseCase cambiarEstadoMasivoPorMunicipioUseCase(
            UsuarioRepository usuarioRepository,
            MunicipioRepository municipioRepository) {
        return new CambiarEstadoMasivoPorMunicipioUseCase(usuarioRepository, municipioRepository);
    }

    @Bean
    public ListarUsuariosUseCase listarUsuariosUseCase(
            UsuarioRepository usuarioRepository,
            MunicipioRepository municipioRepository,
            UsuarioDtoMapper dtoMapper) {
        return new ListarUsuariosUseCase(usuarioRepository, municipioRepository, dtoMapper);
    }

    @Bean
    public BuscarUsuarioPorDocumentoUseCase buscarUsuarioPorDocumentoUseCase(
            UsuarioRepository usuarioRepository) {
        return new BuscarUsuarioPorDocumentoUseCase(usuarioRepository);
    }
}
