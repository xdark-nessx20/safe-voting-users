package com.safevoting.users.infrastructure.adapter.out.persistence.otp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("otps")
public class OtpEntity {

    @Id
    private UUID id;
    private String email;
    private String codigo;
    private Instant expiracion;
    private int intentos;
    private String estado;
}
