package com.safevoting.users.infrastructure.config;

import com.safevoting.users.application.auth.RegisterVotanteUseCase;
import com.safevoting.users.application.auth.RequestOtpUseCase;
import com.safevoting.users.application.auth.VerifyOtpUseCase;
import com.safevoting.users.domain.repository.EmailSender;
import com.safevoting.users.domain.repository.MunicipioRepository;
import com.safevoting.users.domain.repository.OtpRepository;
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
}
