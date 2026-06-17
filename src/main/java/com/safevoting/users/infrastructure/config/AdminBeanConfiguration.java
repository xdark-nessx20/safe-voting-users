package com.safevoting.users.infrastructure.config;

import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBeanConfiguration {

    @Bean
    public CambiarEstadoIndividualUseCase cambiarEstadoIndividualUseCase(
            UsuarioRepository usuarioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new CambiarEstadoIndividualUseCase(usuarioRepository, gestorElectoralRepository);
    }

    @Bean
    public CambiarEstadoMasivoPorAlcanceUseCase cambiarEstadoMasivoPorAlcanceUseCase(
            UsuarioRepository usuarioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new CambiarEstadoMasivoPorAlcanceUseCase(usuarioRepository, gestorElectoralRepository);
    }

    @Bean
    public CambiarEstadoMasivoPorMunicipioUseCase cambiarEstadoMasivoPorMunicipioUseCase(
            UsuarioRepository usuarioRepository,
            MunicipioRepository municipioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new CambiarEstadoMasivoPorMunicipioUseCase(usuarioRepository, municipioRepository, gestorElectoralRepository);
    }

    @Bean
    public ListarUsuariosUseCase listarUsuariosUseCase(
            UsuarioRepository usuarioRepository,
            MunicipioRepository municipioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new ListarUsuariosUseCase(usuarioRepository, municipioRepository, gestorElectoralRepository);
    }

    @Bean
    public BuscarUsuarioPorDocumentoUseCase buscarUsuarioPorDocumentoUseCase(
            UsuarioRepository usuarioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new BuscarUsuarioPorDocumentoUseCase(usuarioRepository, gestorElectoralRepository);
    }
}
