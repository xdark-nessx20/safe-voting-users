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
        return Mono.fromRunnable(() -> {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom("noreply@safevoting.com");
            mensaje.setTo(email.getValor());
            mensaje.setSubject("C\u00f3digo de verificaci\u00f3n Safe-Voting");
            mensaje.setText("Tu c\u00f3digo de verificaci\u00f3n es: " + codigo + "\n\n"
                    + "Este c\u00f3digo expira en 5 minutos.\n\n"
                    + "Si no solicitaste este c\u00f3digo, ignora este mensaje.");

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
