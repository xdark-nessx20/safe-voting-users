package com.safevoting.users.infrastructure.adapter.in.scheduler;

import com.safevoting.users.domain.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OtpExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpRepository otpRepository;

    public OtpExpirationScheduler(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Scheduled(fixedRate = 60000)
    public void invalidarOtpsExpirados() {
        otpRepository.invalidarExpirados().subscribe(
                v -> log.debug("Barrido de OTPs expirados completado"),
                error -> log.error("Error en barrido de OTPs expirados: {}", error.getMessage())
        );
    }
}
