package com.safevoting.users.infrastructure.adapter.out.email;

import com.safevoting.users.domain.repository.EmailSender;
import com.safevoting.users.domain.shared.Email;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class EmailSenderAdapter implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderAdapter.class);

    private final JavaMailSender javaMailSender;

    @Override
    public Mono<Void> enviarOtp(Email email, String codigo) {
        var text = """
                Tu código de verificación es %s.
                Este código expira en 5 minutos.
                Si no solicitaste este código, reportalo ante soporte.
                """.formatted(codigo);

        return Mono.fromRunnable(() -> {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom("noreply@safevoting.com");
            mensaje.setTo(email.getValor());
            mensaje.setSubject("Código de verificación Safe-Voting");
            mensaje.setText(text);

            try {
                javaMailSender.send(mensaje);
                log.info("OTP enviado a {}", email.getValor());
            } catch (Exception e) {
                log.error("Error al enviar OTP a {}: {}", email.getValor(), e.getMessage());
                throw new RuntimeException("Error al enviar el correo electrónico", e);
            }
        });
    }
}
