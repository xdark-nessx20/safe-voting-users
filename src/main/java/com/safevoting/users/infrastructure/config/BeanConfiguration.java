package com.safevoting.users.infrastructure.config;

import com.safevoting.users.application.auth.AsignarGestorUseCase;
import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.application.inscripcion.AceptarSolicitudInscripcionUseCase;
import com.safevoting.users.application.inscripcion.ListarSolicitudesPendientesUseCase;
import com.safevoting.users.application.inscripcion.RechazarSolicitudInscripcionUseCase;
import com.safevoting.users.application.inscripcion.SolicitarCambioInscripcionUseCase;
import com.safevoting.users.application.usuario.BuscarUsuarioPorDocumentoUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoIndividualUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorAlcanceUseCase;
import com.safevoting.users.application.usuario.CambiarEstadoMasivoPorMunicipioUseCase;
import com.safevoting.users.application.usuario.ListarUsuariosUseCase;
import com.safevoting.users.domain.repository.EmailSender;
import com.safevoting.users.domain.repository.GestorElectoralRepository;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.OtpRepository;
import com.safevoting.users.domain.repository.SolicitudCambioInscripcionRepository;
import com.safevoting.users.domain.repository.TokenService;
import com.safevoting.users.domain.repository.UsuarioRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public RegisterVotanteUseCase registerVotanteUseCase(
            UsuarioRepository usuarioRepository,
            MunicipioRepository municipioRepository) {
        return new RegisterVotanteUseCase(usuarioRepository, municipioRepository);
    }

    @Bean
    public RequestOtpUseCase requestOtpUseCase(
            UsuarioRepository usuarioRepository,
            OtpRepository otpRepository,
            EmailSender emailSender) {
        return new RequestOtpUseCase(usuarioRepository, otpRepository, emailSender);
    }

    @Bean
    public VerifyOtpUseCase verifyOtpUseCase(
            UsuarioRepository usuarioRepository,
            OtpRepository otpRepository,
            TokenService tokenService) {
        return new VerifyOtpUseCase(usuarioRepository, otpRepository, tokenService);
    }

    @Bean
    public AsignarGestorUseCase asignarGestorUseCase(
            UsuarioRepository usuarioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new AsignarGestorUseCase(usuarioRepository, gestorElectoralRepository);
    }

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

    @Bean
    public SolicitarCambioInscripcionUseCase solicitarCambioInscripcionUseCase(
            SolicitudCambioInscripcionRepository solicitudRepository,
            MunicipioRepository municipioRepository,
            UsuarioRepository usuarioRepository) {
        return new SolicitarCambioInscripcionUseCase(solicitudRepository, municipioRepository, usuarioRepository);
    }

    @Bean
    public ListarSolicitudesPendientesUseCase listarSolicitudesPendientesUseCase(
            SolicitudCambioInscripcionRepository solicitudRepository,
            GestorElectoralRepository gestorElectoralRepository,
            UsuarioRepository usuarioRepository) {
        return new ListarSolicitudesPendientesUseCase(solicitudRepository, gestorElectoralRepository, usuarioRepository);
    }

    @Bean
    public AceptarSolicitudInscripcionUseCase aceptarSolicitudInscripcionUseCase(
            SolicitudCambioInscripcionRepository solicitudRepository,
            UsuarioRepository usuarioRepository,
            GestorElectoralRepository gestorElectoralRepository) {
        return new AceptarSolicitudInscripcionUseCase(solicitudRepository, usuarioRepository, gestorElectoralRepository);
    }

    @Bean
    public RechazarSolicitudInscripcionUseCase rechazarSolicitudInscripcionUseCase(
            SolicitudCambioInscripcionRepository solicitudRepository,
            GestorElectoralRepository gestorElectoralRepository,
            UsuarioRepository usuarioRepository) {
        return new RechazarSolicitudInscripcionUseCase(solicitudRepository, gestorElectoralRepository, usuarioRepository);
    }
}
