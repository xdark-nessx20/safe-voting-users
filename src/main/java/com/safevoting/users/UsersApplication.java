package com.safevoting.users;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
        info = @Info(
                title = "Safe-Voting — Módulo de Usuarios",
                version = "0.1.0",
                description = "API REST para registro, autenticación OTP, gestión de usuarios y cambio de inscripción electoral",
                contact = @Contact(name = "Safe-Voting", url = "https://github.com/xdark-nessx20/safe-voting-users")
        )
)
public class UsersApplication {

    public static void main(String[] args) {
        SpringApplication.run(UsersApplication.class, args);
    }
}
